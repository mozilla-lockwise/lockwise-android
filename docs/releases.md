# Release Instructions

Some assumptions:

- `master` is the default branch and is production-ready
  - non-'master' and non-'production' branches run the "primary" workflow which signs a "Debug" variant
  - though, users with access can manually "Start" a "deploy" build against a branch from the bitrise UI
- commits made to `master` and `production` are built and pass in [bitrise][1]
  - the "deploy" workflow in bitrise runs and signs a "Release" variant APK
- bitrise will test, build, and sign the APK for every build on every branch
- `production` is our public release branch and may not match `master`
  - ideally, `production` will perfectly reproduce master
  - but if `master` is in an un-releasable state, we cherry-pick commits to this branch
  - this is an exception rather than the preferred maintenance method
- no automated uploads to Google Play Console occur, manual artifact uploads (from bitrise's signed Release artifacts) are required for internal, closed, open, and production releases
- Play Store has Internal (core team, email-restricted), Alpha (closed, email-restricted), and Production (everyone!) release channels configured
  - currently, no plans exist for "external" testers nor "Open" release channels
  - in the future we may create an open Beta testing channel to allow anyone outside of Mozilla to help us test in-progress work

## Distributing Builds through bitrise (primary = branch, deploy = master)

_all commits on all branches and pull requests are automatically built_

1. Push to the git branch available on GitHub.com and open a pull request
2. Open [bitrise][1] from a mobile device and browse to the build
    - bitrise access is currently private and email-restricted

## Preparing a Release (for Internal, Alpha, or Production via Play Store)

### Collect necessary identifiers for the new build

Before you can begin a new production release, it is necessary to take the following steps into consideration:

1. Find the current production version number. Using the strategy below, increment the version number to identify the state of the new release.
    - **Choosing a new release version number**
        - Build versions are defined using a three-place value, `X.X.X`. Each number in the version is incremented based on the priority of the version being released.
        - For normally scheduled releases, the left-most value is incremented and the following two numbers "reset" to zero.
            - Example: after release `2.1.0`, the next normally scheduled release would be version `3.0.0`.
        - For smaller consecutive builds that come after a scheduled release (this could be bug fixes, etc.), the left-most value is maintained while the middle number is incremented.
            - Example: after release `3.0.0`, a version may be titled `3.1.0`.
        - For emergency hotfix/high priority/not scheduled releases, we may want to do a "point release". In this case, the left two values stay the same and the right-most value is incremented.
            - Example: after release `3.1.0`, an emergency hotfix release would be titled `3.1.1`.
2. Before **public** release:
    - make sure no open "localization strings" Pull Requests are open
3. After **public** release:
    - Consider filing an issue for the next sprint/release to update to the latest dependencies (application-services, for example)
    - Consider running `mkdocs gh-deploy` so the latest `docs` are also published to the GitHub pages website
    - Consider changing the version to the next major/planned release version (depending on the future plans). Update the `versionName` value (and `versionCode` if applicable) in `app/build.gradle`
    - Also update the value in the `list_cell_setting_appversion.xml` layout to _the exact same version_
    - Make sure the `app-mapping.txt` (ProGuard rules) is uploaded to Sentry as part of the process earlier, do it now if not so your error reports are legible.

### Create a release branch and prepare the production branch

1. Update the release notes under `docs/release-notes.md`
    - determine the next build number (see above, "Choosing a new release version number") and include it in release notes
    - create a pull request into the `master` branch and get approval
    - merge the release notes to `master` branch
    - this will result in a release build matching the build number provided
2. Create a new branch from `master` for this release (following the naming convention `release-v1.1.2` for a new version called `1.1.2`)
3. Update the `versionCode` and `versionName` values in the `build.gradle` file to match the new version number
4. Create a pull request from this release branch to `production`
    - https://github.com/mozilla-lockwise/lockwise-android/compare/production...release-vYOURVERSION
    - once the Bitrise checks pass, merge the branch into `production`

### Create the Play Store release

1. Create a tag from `production` matching the format: `major.minor.patch.buildNumber`
    - Example:
        - `1.2.1399` is major version 1.2, Bitrise build #1399
        - `1.3.1.1624` is major version 1.3 with 1 patch release, Bitrise build #1624
    - `git tag -a -s 1.3.1.1624 -m "1.3.1 (Build 1624)"`
2. Push the tag to GitHub and create a corresponding "Release" on GitHub
    - `git push --tags` will push all local tags to origin
    - mark this as a "pre-release" until PI requests are finished
    - copy the release notes to the "Release" on GitHub
3. Download the `-signed.apk` from Bitrise (found in "Apps & Artifacts" tab on the successful production deploy build) and attach it to the Release on GitHub
    - **Important:** Open the `-signed.apk` and confirm release build does not allow screenshots and does not expose the contents of the app in the switcher
4. Upload the `-signed.apk` to the [Play Console][2]:
    - browse to "Release Management" > "App Releases" > "Internal test track" > "Manage"
    - "Create Release" and upload the signed APK, set the version to match the github tag (for example: `1.2.1339`) then "Review" and the build will be immediately available to the core team
5. Download the `app-mapping.txt` (ProGuard rules) from Bitrise
    - Suggestion: rename the file to `app-mapping-BUILDNUMBER.txt` to make it less generic
    - This will be used in step 8
6. Create a file named `AndroidManifest-YOURBUILDNUMBER.xml` in a local folder on your machine to map the Proguard file to the specific build:
    - The file should contain the following (change the versionCode and versionName to match your build):
        ```
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="mozilla.lockbox"
            android:versionCode="4053"
            android:versionName="1.1.1" >
        </manifest>
        ```
    - this file will be used in step 8
7. [Create an auth token](https://sentry.prod.mozaws.net/settings/account/api/auth-tokens/) for your sentry account that will allow you to create releases. This will be used when uploading the `app-mapping` and `AndroidManifest`.
    - **Note:** if you have already created an auth token, there is no need to create a new one.
8. In Sentry, browse to "Android Vitals" and "Deobfusication files" and upload the `app-mapping.txt` file from step 6
    - Upload the mapping and manifest to Sentry using `sentry-cli` and following [these instructions](https://docs.sentry.io/cli/dif/proguard/)
        - The upload script should look something like this:
        ```
        SENTRY_URL=https://sentry.prod.mozaws.net/ sentry-cli --auth-token YOURSENTRYAUTHTOKEN upload-proguard --android-manifest /PATH/to/manifest/AndroidManifest-YOURBUILDNUMBER.xml --org operations --project lockwise-android /PATH/to/appmapping/app-mapping-YOURBUILDNUMBER.txt --no-reprocessing
        ```
    - HINT: if you get a 411 "content-length" error, you may need to add the `--no-reprocessing` flag due to a bug with GCP and the `sentry-cli`

### Distributing Builds through Play Store (Internal, Alpha)

_All builds must be manually uploaded from bitrise to Play Store Console as an artifact aka "New Release" in the "Preparing" instructions above. Only mobile engineering managers and Release Management team members (See #release-coordination) have access to create new non-production releases._

1. Browse to [App Releases][2] in Play Console
2. Browse to the "Internal test track" (this release should already be uploaded and available to the core Lockwise team)
3. Promote the release to the (internal, still) Alpha channel using the "Promote to Alpha" button, complete the questions

### Distributing through the Play Store (Production)

_Only Release Management team members (see private #release-coordination channel in Mozilla Slack) have access to distribute production releases through the Play Store._

1. Contact release management by filing a bug under **[Release Engineering: Release Requests](https://bugzilla.mozilla.org/enter_bug.cgi?product=Release%20Engineering&component=Release%20Requests)**
2. Note the expected version, build and which channel it is in (internal? Alpha? Beta?). [For example](https://bugzilla.mozilla.org/show_bug.cgi?id=1555746):
    - Summary: Promote Lockwise Android 1.1.1 from Beta to Production channel in Play Store
    - Description: We have a minor release to promote from Beta to Production by Tuesday (pending QA approval). APK: the version 1.1.1 (build 4057) in the Internal, Alpha, and Beta tracks is the current release candidate, but a newer build may be created in case of any potential "show stopper" bugs filed in the coming days.

NOTE: _brand new_ apps may take 1 or more hours to appear in the Play Store whereas existing app (updates) can appear within minutes. Schedule accordingly!

## In Case of Emergency (Release)

_similar to above, but requires explicit cherry-pick commits on `production` branch when `master` branch is not in a release-able state_

1. Merge the emergency changes or fixes or features to default `master` branch as usual
2. Update the release notes
3. Create and merge a pull request _up to and including_ the last release-able commit on `master` to `production`
4. Then `git cherry-pick` each additional commit from `master` to be included in the release
    - thus skipping or avoiding the non-release-able commits
5. Push the resulting `production` branch to GitHub.com
6. Create a tag from `production` matching the format: `major.minor.patch.build`
    - for example: `git tag -a -s 1.3.1.1624 -m "1.3.1 (Build 1624)"`
7. Push the tag to GitHub and create a corresponding "Release" on github
    - copy the release notes from `release-notes.md` to the Github release
8. Browse to bitrise and find the desired `production` branch build to distribute
9. Follow the same instructions above starting at _Step 5_.
10. Continue the "Distributing..." instructions

[1]: https://app.bitrise.io/app/20089a88380dd14d
[2]: https://play.google.com/apps/publish/?account=7083182635971239206#ManageReleasesPlace:p=mozilla.lockbox&appid=4972100280256015711
