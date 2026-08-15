# Example projects

Sample Ladle projects for manual testing. Each example uses the repo-root `.jdk` directory (downloaded by `./bin/ladle dependency` from the main `build.ini`, or set `path = $JAVA_HOME` there).

Build ladle first if `lib/ladle.jar` is missing:

```powershell
.\build.ps1
```

```sh
./build.sh
```

## Subprojects

A library subproject is built first; its JAR is published to `app/dependencies/lib.jar`, then the app compiles against it.

```powershell
.\bin\ladle.ps1 build examples\subprojects\app\build.ini
```

```sh
./bin/ladle build examples/subprojects/app/build.ini
```

Expected: exit code 0, `examples/subprojects/app/dependencies/lib.jar` exists.

Standalone release of the library:

```powershell
.\bin\ladle.ps1 release examples\subprojects\lib\build.ini
```

```sh
./bin/ladle release examples/subprojects/lib/build.ini
```

Expected: exit code 0, `examples/subprojects/lib/build/lib.jar` exists.

## Failing build

Contains a Java compile error on purpose.

```powershell
.\bin\ladle.ps1 build examples\failing-build\build.ini
```

```sh
./bin/ladle build examples/failing-build/build.ini
```

Expected: non-zero exit code and javac error output.

## Test fixtures

Shared test helpers live under `src/testFixtures/java` and are compiled before tests. `AppTest` uses `AppFixture`. Main sources are `src/main/java` so fixtures are not compiled into the production classes.

```powershell
.\bin\ladle.ps1 dependency examples\test-fixtures\build.ini
.\bin\ladle.ps1 build examples\test-fixtures\build.ini
.\bin\ladle.ps1 test examples\test-fixtures\build.ini
```

```sh
./bin/ladle dependency examples/test-fixtures/build.ini
./bin/ladle build examples/test-fixtures/build.ini
./bin/ladle test examples/test-fixtures/build.ini
```

Expected: exit code 0, `Tests successful.`
