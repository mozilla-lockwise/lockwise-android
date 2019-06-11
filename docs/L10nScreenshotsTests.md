### Screenshtos Tests
We are using [`screengrab`](https://docs.fastlane.tools/getting-started/android/screenshots/) which works with fastlane to automate the process of capturing screenshots. 
All the l10n screenshots are generated through the ui tests. These particular tests run as part of the screenshots package (`app/src/androidTest/mozilla/lockbox/screenshots`)

### Steps to Run / Build
1. Install the gem:
`sudo gem install screengrab`

2. From command line run: 
`fastlane screengrab --test_instrumentation_runner "androidx.test.runner.AndroidJUnitRunner"` 

Screenshots will be saved in the root directory: `fastlane/metadata/android` 
If there is a failure and screenshots are not saved, it may be necessary to create these folders manually first.
Currently screenshots are uploaded to [`google drive`](https://drive.google.com/drive/folders/1r1SbIBPVLzm3JGlClZYNHB7vfsKdaEJ6?usp=sharing) once they are generated manually by running the previous command line.
In the future this process will be automated too and documented here.
