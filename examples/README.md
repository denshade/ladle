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

The root `examples/subprojects/build.ini` has no `[sources]` section. It only lists subprojects, so `ladle build` compiles `lib` and `app` and skips `javac` for the root.

```powershell
.\bin\ladle.ps1 build examples\subprojects\build.ini
```

```sh
./bin/ladle build examples/subprojects/build.ini
```

Expected: exit code 0, `examples/subprojects/dependencies/lib.jar` and `examples/subprojects/dependencies/app.jar` exist. Building `app` also publishes `lib` to `examples/subprojects/app/dependencies/lib.jar`.

A library subproject can also be built from the app INI; its JAR is published to `app/dependencies/lib.jar`, then the app compiles against it.

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

Standalone fat JAR of the app (unpacks `lib.jar` into the release):

```powershell
.\bin\ladle.ps1 release examples\subprojects\app\build.ini
```

```sh
./bin/ladle release examples/subprojects/app/build.ini
```

Expected: exit code 0, `examples/subprojects/app/build/app.jar` exists and contains both `example/app/App.class` and `example/lib/Lib.class`. The printed `jar` command uses `-C build/fat-classes`.

Tests live on the library subproject. The aggregator INI has no `[test]` section; `ladle test` walks `[subproject]` and runs `lib`'s tests (`app` has none and is skipped).

```powershell
.\bin\ladle.ps1 dependency examples\subprojects\build.ini
.\bin\ladle.ps1 build examples\subprojects\build.ini
.\bin\ladle.ps1 test examples\subprojects\build.ini
```

```sh
./bin/ladle dependency examples/subprojects/build.ini
./bin/ladle build examples/subprojects/build.ini
./bin/ladle test examples/subprojects/build.ini
```

Expected: exit code 0, `Tests successful.`

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

## JUnit 4

Runs tests with `org.junit.runner.JUnitCore` instead of the default JUnit 5 ConsoleLauncher.

```powershell
.\bin\ladle.ps1 dependency examples\junit4\build.ini
.\bin\ladle.ps1 build examples\junit4\build.ini
.\bin\ladle.ps1 test examples\junit4\build.ini
```

```sh
./bin/ladle dependency examples/junit4/build.ini
./bin/ladle build examples/junit4/build.ini
./bin/ladle test examples/junit4/build.ini
```

Expected: exit code 0, `Tests successful.`, and the printed runner is `org.junit.runner.JUnitCore`.
