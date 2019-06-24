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

1. Update the release notes under `docs/release-notes.md`
  - create a pull request to collaborate and get approval
  - determine the next build number and include it in release notes
  - merge the release notes to `master` branch
  - this will result in a release build matching the build number provided
2. Create and merge a pull request _from_ `master` _to_ `production` so it tracks the release
  - https://github.com/mozilla-lockwise/lockwise-android/compare/production...master
3. Create a tag from `production` matching the format: `major.minor.patch.build`
  - for example: `1.2.1399` is major version 1.2, bitrise build 1399
  - for example: `1.3.1.1624` is major version 1.3 with 1 patch release, (bitrise) build 1624
  - `git tag -a -s 1.3.1.1624 -m "1.3.1 (Build 1624)"`
4. push the tag to GitHub and create a corresponding "Release" on GitHub.com
  - copy the release notes to the "Release" on GitHub
  - download the `-signed.apk` from bitrise and attach it to the Release on GitHub
  - **open the `-signed.apk` and confirm release build does not allow screenshots and does not expose the contents of the app in the switcher**
5. Upload the `-signed.apk` (from bitrise) to the [Play Console][2]:
  - browse to "Release Management" > "App Releases" > "Internal test track" > "Manage"
  - "Create Release" and upload the signed APK, set the version to match the tag (for example: `1.2.1339`) then "Review" and the build will be immediately available to the core team
  - Browse to 
6. Download the `app-mapping.txt` (ProGuard rules) from bitrise and create an `AndroidManifest.xml` to map the file to the specific build:
  ```
  <?xml version="1.0" encoding="utf-8"?>
  <manifest xmlns:android="http://schemas.android.com/apk/res/android"
      package="mozilla.lockbox"
      android:versionCode="4053"
      android:versionName="1.1.1" >
  </manifest>
  ```
7. Browse to "Android Vitals" and "Deobfusication files" and upload the `app-mapping.txt` file from bitrise.io
8. Upload the mapping and manifest to Sentry using `sentry-cli` and following [these instructions](https://docs.sentry.io/cli/dif/proguard/](https://docs.sentry.io/cli/dif/proguard/)
  - HINT: if you get a 411 "content-length" error, you may need to add the `--no-reprocessing` flag due to a bug with GCP and the `sentry-cli`
9. Continue the "Distributing..." instructions

### In Case of Emergency (Release)

_similar to above, but requires explicit cherry-pick commits on `production` branch when `master` branch is not in a release-able state_

1. Merge the emergency changes or fixes or features to default `master` branch as usual
2. Update the release notes
3. Create and merge a pull request _up to and including_ the last release-able commit on `master` to `production`
4. Then `git cherry-pick` each additional commit from `master` to be included in the release
  - thus skipping or avoiding the non-release-able commits
5. Push the resulting `production` branch to GitHub.com
6. Create a tag from `production` matching the format: `major.minor.patch.build`
  - for example: `git tag -a -s 1.3.1.1624 -m "1.3.1 (Build 1624)"`
7. Push the tag to GitHub and create a corresponding "Release" on GitHub.com
  - copy the release notes to the "Release" on GitHub
8. Browse to bitrise and find the desired `production` branch build to distribute
9. Follow the same instructions above starting at _Step 5_.
10. Continue the "Distributing..." instructions

## Distributing Builds through Play Store (Internal, Alpha)

_All builds must be manually uploaded from bitrise to Play Store Console as an artifact aka "New Release" in the "Preparing" instructions above. Only mobile engineering managers and Release Management team members (See #release-coordination) have access to create new non-production releases._

1. Browse to [App Releases][2] in Play Console
2. Browse to the "Internal test track" (this release should already be uploaded and available to the core Lockwise team)
3. Promote the release to the (internal, still) Alpha channel using the "Promote to Alpha" button, complete the questions

## Distributing through the Play Store (Production)

_Only Release Management team members (see private #release-coordination channel in Mozilla Slack) have access to distribute production releases through the Play Store._

1. Contact release management by filing a bug under **[Release Engineering: Release Requests](https://bugzilla.mozilla.org/enter_bug.cgi?product=Release%20Engineering&component=Release%20Requests)**
2. Note the expected version, build and which channel it is in (internal? Alpha? Beta?). [For example](https://bugzilla.mozilla.org/show_bug.cgi?id=1555746):
  - Summary: Promote Lockwise Android 1.1.1 from Beta to Production channel in Play Store
  - Description: We have a minor release to promote from Beta to Production by Tuesday (pending QA approval). APK: the version 1.1.1 (build 4057) in the Internal, Alpha, and Beta tracks is the current release candidate, but a newer build may be created in case of any potential "show stopper" bugs filed in the coming days.

NOTE: _brand new_ apps may take 1 or more hours to appear in the Play Store whereas existing app (updates) can appear within minutes. Schedule accordingly!

## Preparation before and after a release

- **Before testing or distributing a release**: make sure the release number is still what you want and expect it to be, especially if major (versus minor) changes were made. (see below on how to _"change the version"_).
- **Before public release**: make sure no open "localization strings" Pull Requests are open
- **After public release**: consider filing an issue for the next sprint/release to update to the latest dependencies (application-services, for example)
- **After public release**: consider running `mkdocs gh-deploy` so the latest `docs` are also published to the GitHub pages website
- **After public release**: _change the version_ to a major or minor update depending on the future plans.
  - Update the `versionName` value (and `versionCode` if applicable) in `app/build.gradle`
  - Also update the value in the `list_cell_setting_appversion.xml` layout to _the exact same version_
- **After public release**: make sure the `app-mapping.txt` (ProGuard rules) is uploaded to Sentry as part of the process earlier, do it now if not so your error reports are legible.

[1]: https://app.bitrise.io/app/20089a88380dd14d
[2]: https://play.google.com/apps/publish/?account=7083182635971239206#ManageReleasesPlace:p=mozilla.lockbox&appid=4972100280256015711
