### Required Libraries

Our linting configuration requires you to have an up-to-date installation of
[`ktlint`](https://github.com/shyiko/ktlint#installation)

### Steps to Run / Build

1. Install the latest Android Studio

2. Clone the repository

3. Open the project in Android Studio

4. Gradle Sync & build/run the project

### Testing
The tests can be run from Android Studio or from the command line:
```sh
./gradlew testDebug
```

#### Code coverage
Local code coverage reports can be generated from Android Studio or from the
command line:
```sh
./gradlew -Pcoverage jacocoDebugTestReport
```

The command line reports can be found at
`app/build/reports/jacoco/jacocoDebugTestReport/html/index.html`.

**N.B.:** each method uses a different reporter and will provide different
results. They both tend to miss things: it's often best to test your code using
both.

The project's [official code coverage rating can be found on codecov.io][codecov],
which uses the results from jacoco (i.e. the command line).

[codecov]: https://codecov.io/gh/mozilla-lockwise/lockwise-android
