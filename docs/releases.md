# Release Instructions

Some assumptions:

- `master` is the default branch and is production-ready
  - Non-'master' and non-'production' branches run the "primary" workflow on Bitrise which signs a "Debug" variant
- [Bitrise][1] access is currently private and email-restricted
  - Users with access can [manually start][4] a "deploy" build against a specified branch from the Bitrise UI
- Commits made to `master` and `production` are built and pass in [Bitrise][1]
  - the "deploy" workflow in bitrise runs and signs a "Release" variant APK
- Bitrise will test, build, and sign the APK for every build on every branch
- No automated uploads to Google Play Console occur. Manual artifact uploads (from a signed Release artifact) are required for internal, closed, open, and production releases
- The Google Play Store has Internal (core team, email-restricted), Alpha (closed, email-restricted), Beta, and Production release channels configured

## Distributing Builds through Bitrise (primary = branch, deploy = master)

_all commits on all branches and pull requests are automatically built_

1. Push all changes to the branch on GitHub and open a [pull request][3] (base: `master`, compare: your branch)
2. Open [Bitrise][1] and browse to the build
3. From the “Apps & Artifacts” tab, download the APK 

## Preparing a Release (Google Play Store)

### Collect necessary identifiers for the new build

2. Before **public** release:
    - Make sure no open "localization strings" Pull Requests are open
    - Update the `versionName` and `versionCode` values in `app/build.gradle`
    - Create a [milestone][5] in GitHub to track the issues to be released
    - Create a PI request in Jira for the QA team to complete before releasing to production ([example][6])
3. After **public** release:
    - Consider filing an issue for the next sprint/release to update to the latest dependencies (for example, application-services and/or android-components)
    - Consider running `mkdocs gh-deploy` so the latest `docs` are also published to the GitHub pages website
    - Also update the value in the `list_cell_setting_appversion.xml` layout to _the exact same version_
    - Make sure the `app-mapping.txt` (ProGuard rules) is uploaded to Sentry as part of the process earlier, do it now if not so your error reports are legible.

## Create a release branch and prepare the production branch

Before you can begin a new production release, it is necessary to take the following steps into consideration:

1. Find the current production version number. Using the strategy below, increment the version number to identify the state of the new release in `app/build.gradle`.
    - **Choosing a new release version number**
        - Build versions are defined using a three-place value, `X.X.X`. Each number in the version is incremented based on the priority of the version being released.
        - For normally scheduled releases, the left-most value is incremented and the following two numbers "reset" to zero.
            - Example: after release `2.1.0`, the next normally scheduled release would be version `3.0.0`.
        - For smaller consecutive builds that come after a scheduled release (this could be bug fixes, etc.), the left-most value is maintained while the middle number is incremented.
            - Example: after release `3.0.0`, a version may be titled `3.1.0`.
        - For emergency hotfix, high priority, or not-scheduled releases, we may want to do a "point release". In this case, the left two values stay the same and the right-most value is incremented.
            - Example: after release `3.1.0`, an emergency hotfix release would be titled `3.1.1`.
2. Update the release notes under `docs/release-notes.md`
    - Determine the next build number (see above, "Choosing a new release version number") and include it in release notes
    - Create a pull request for the release notes into the `master` branch and get approval
    - Merge the release notes to `master` branch
3. Next, we will choose a new version code and update the `versionCode` field in `app/build.gradle`.
    - Version codes will correspond to the build number on Bitrise
        - Example: [build #5931][7]
    - Make sure that both version code and version number are updated in the `build.gradle` file on `master`
4. For each major release (a normally scheduled release where the left-most value of the build number, `X.X.X`, is incremented), create a new branch from `master` based on the following naming convention: 
    - The new branch will follow this pattern: `release-#.x`, where # corresponds to the left-most build number of `X.X.X`
    - For example, `release-v3.x` for version `3.0.0`, `release-v4.x` for version `4.0.0`, etc.
5. Additionally, create another new branch from `master` following the naming convention `release-v1.1.2` for a new version called `1.1.2`
    - In this scheme, all of the versions starting with `X.0.0` will have a specific “snapshot” branch (e.g. `release-v1.1.2`), and the `release-v#.x` branch will reflect the current state of production.
6. At this point, ensure that all changes and branches are pushed upstream to GitHub
    - The new `release-v#.x` branch should match `master`, with new release notes, version code, and version number
    - The new `release-v#.#.#` branch should match both the current state of `master` and `release-v#.x` (remember: the `release-v#.x` branch will always reflect the most up-to-date state of the production app)

## Create the Play Store release

### Tag the release
1. Create a tag from the `release-v#.x` branch matching the format: `release-vX.X.X-RC-#` (RC = release candidate)
    - We may have more than one release candidate for each version
    - Include the Bitrise build number (aka version code) in the tag message
    - Command line: `git tag -a -s release-v3.3.0-RC-1 -m “3.3.0 (Build 5783)”`, where “Build 5783” corresponds to the Bitrise build number (version code)
    - Example:
        - `release-v1.2.0-RC-1` is major version 1.2.0, release candidate 1
        - `release-v1.2.0-RC-2` is major version 1.2.0, the second version of the release candidate
2. Create another tag from the `release-v#.x` branch for the “major” version of the release:
    - Command line: `git tag -a -s release-v3.3.0 -m “3.3.0 (Build 5783)”`
3. Push the tags to GitHub
    - `git push --tags` will push all local tags to origin

### Create the APK and Release
1. From Bitrise, [manually start][4] a "deploy" build against the major release branch
    - “Start/Schedule a Build” -> Branch: `release-v3.x` -> Message: “3.3.0 (Build 5783)” -> Workflow: “Deploy” -> “Start Build”
    - **Important**: wait for this build to finish running. If the build is not green, you cannot release it.
2. Download the `-signed.apk` from Bitrise (found in "Apps & Artifacts" tab on the successful deploy build) and add it as an attachment to the Release on GitHub
    - **Important:** Open the `-signed.apk` on your device/emulator and confirm release build does not allow screenshots and does not expose the contents of the app 
3. Also download the `app-mapping.txt` from the “Apps & Artifacts” tab on Bitrise
    - Suggestion: rename the file to `app-mapping-BUILDNUMBER.txt` to make it less generic
    - This will be used by Proguard, Sentry, and the Play Console later to deobfuscate the apk
4. Create a new [Release][9] on GitHub 
    - [Draft a new release][10] using the major tag version that you created (e.g. `release-v3.3.0`)
    - The release title will match this format: “3.3.0 (Build 5784)”
    - Mark this as a "pre-release" until PI requests are finished
    - Copy the release notes into the release description
    - Upload the `-signed.apk` as an attachment

## Create the release on the Play Console

### Distributing Builds through Play Store (Internal, Alpha, Beta)

_All builds must be manually uploaded from bitrise to Play Store Console as an artifact aka "New Release" in the "Preparing" instructions above. Only mobile engineering managers and Release Management team members (See #release-coordination) have access to create new non-production releases._

1. Upload the `-signed.apk` to the [Play Console][2]:
    - browse to "Release Management" > "App Releases" > "Internal test track" > "Manage"
    - "Create Release" and upload the signed APK, set the version to match the github tag (for example: `3.3.0` with version code matching the Bitrise build number) then "Review" and the build will be immediately available to the core team
2. Map the `app-mapping-YOURBUILDNUMBER.txt` to the specific build for Proguard
    - Create an xml file named `AndroidManifest-YOURBUILDNUMBER.xml` in a local folder on your machine
    - The file should contain the following (change the versionCode and versionName to match your numbers from `app/build.gradle`):
        ```
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="mozilla.lockbox"
            android:versionCode="4053"
            android:versionName="3.3.0" >
        </manifest>
        ```
3. Next, [create an auth token](https://sentry.prod.mozaws.net/settings/account/api/auth-tokens/) for your sentry account that will allow you to create releases. This will be used when uploading the `app-mapping.txt` and `AndroidManifest.xml`.
    - **Note:** if you have already created an auth token, there is no need to create a new one.
4. Upload the mapping and manifest to Sentry using `sentry-cli` and follow [these instructions](https://docs.sentry.io/cli/dif/proguard/)
        - The upload script should look something like this:
        ```
        SENTRY_URL=https://sentry.prod.mozaws.net/ sentry-cli --auth-token YOURSENTRYAUTHTOKEN upload-proguard --android-manifest /PATH/to/manifest/AndroidManifest-YOURBUILDNUMBER.xml --org operations --project lockwise-android /PATH/to/appmapping/app-mapping-YOURBUILDNUMBER.txt --no-reprocessing
        ```
    - HINT: if you get a 411 "content-length" error, you may need to add the `--no-reprocessing` flag due to a bug with GCP and the `sentry-cli`
5. From the Play Console, browse to “Android vitals” and “Deobfuscation files”
    - Upload the `app-mapping.txt` for the new release
6. Once the internal build is uploaded and tested, upgrade the release from the Play Console “App releases” page to alpha and beta as needed.

### Distributing through the Play Store (Production)

_Only Release Management team members have access to distribute production releases through the Play Store. A bug must be filed in Bugzilla in order to promote a release to production._

1. Contact release management by filing a bug under **[Release Engineering: Release Requests](https://bugzilla.mozilla.org/enter_bug.cgi?product=Release%20Engineering&component=Release%20Requests)**
2. Note the expected version, build and which channel it is in (internal? Alpha? Beta?). [For example](https://bugzilla.mozilla.org/show_bug.cgi?id=1555746):
    - Summary: Promote Lockwise Android 1.1.1 from Beta to Production channel in Play Store
    - Description: We have a minor release to promote from Beta to Production by Tuesday (pending QA approval). APK: the version 1.1.1 (build 4057) in the Internal, Alpha, and Beta tracks is the current release candidate, but a newer build may be created in case of any potential "show stopper" bugs filed in the coming days.

NOTE: _brand new_ apps may take 1 or more hours to appear in the Play Store whereas existing app (updates) can appear within minutes. Schedule accordingly!


[1]: https://app.bitrise.io/app/fc6f4fc6f58da1fc#/builds
[2]: https://play.google.com/apps/publish/?account=7083182635971239206#ManageReleasesPlace:p=mozilla.lockbox&appid=4972100280256015711
[3]: https://github.com/mozilla-lockwise/lockwise-android/compare
[4]: https://devcenter.bitrise.io/builds/Starting-builds-manually/
[5]: https://github.com/mozilla-lockwise/lockwise-android/milestones
[6]: https://jira.mozilla.com/browse/PI-384
[7]: https://app.bitrise.io/build/0badfa6c6476aa75#?tab=log
[8]: https://github.com/mozilla-lockwise/lockwise-android/compare/master...release-v3.x
[9]: https://github.com/mozilla-lockwise/lockwise-android/releases
[10]: https://github.com/mozilla-lockwise/lockwise-android/releases/new
[11]: https://sentry.prod.mozaws.net/operations/lockwise-android/
