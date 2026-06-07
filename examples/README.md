# Example projects

Sample Ladle projects for manual testing. Each example uses the repo-root `.jdk` junction (see the main `build.ini` for setup).

Build ladle first:

```powershell
.\build.ps1
```

## Subprojects

A library subproject is built first; its JAR is published to `app/dependencies/lib.jar`, then the app compiles against it.

```powershell
java -jar lib\ladle.jar build examples\subprojects\app\build.ini
```

Expected: exit code 0, `examples/subprojects/app/dependencies/lib.jar` exists.

## Failing build

Contains a Java compile error on purpose.

```powershell
java -jar lib\ladle.jar build examples\failing-build\build.ini
```

Expected: non-zero exit code and javac error output.
