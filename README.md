thelaboflieven.info.Ladle is a build system that brings modern capabilities, but rejects framework complication.

What makes frameworks handy is that they bring default functionality.
So you don't need to know the details. 

Problems are: 
1. Hard to debug, I added this field, why is it not working or triggered?
2. Hard to optimize. Where do you even start?
3. High learning curve if you need to change something out of the ordinary.
4. Low backwards compatibility: New versions require elaborate reconfiguring. 
5. Custom workflows are done in languages you don't know/have limited experience with. 
6. Hard to customize small things. How do I add a specific flag to the JVM?

thelaboflieven.info.Ladle supports: 
1. Downloading dependencies
2. Building class files
3. Building jar files.
4. Running unit tests

thelaboflieven.info.Ladle will optimize for transparency and speed.
It will document clearly each and every used parameter.

## Using Ladle

Each project ships with `lib/ladle.jar` and launcher scripts under `bin/`. No global install is required.

```
your-project/
  bin/ladle          # Unix launcher
  bin/ladle.cmd      # Windows launcher
  lib/ladle.jar      # committed in git
  build.ini
```

Run from the project root:

```sh
./bin/ladle build build.ini
./bin/ladle release build.ini
./bin/ladle dependency build.ini
./bin/ladle test build.ini
./bin/ladle --help
```

Windows:

```powershell
.\bin\ladle.ps1 build build.ini
```

Or invoke the JAR directly (requires `java` on PATH):

```sh
java -jar lib/ladle.jar build build.ini
```

### Developing Ladle itself

Rebuild `lib/ladle.jar` after changing Java sources:

```powershell
.\build.ps1
```

```sh
./build.sh
```

Commit the updated `lib/ladle.jar` so other projects and CI pick up the new version.

### Adding Ladle to another project

Copy into your repository:

- `lib/ladle.jar`
- `bin/ladle`, `bin/ladle.cmd` (and optionally `bin/ladle.ps1`)

## Quick start

Run Ladle with a path to your build INI file:

```sh
./bin/ladle build build.ini
./bin/ladle release build.ini
./bin/ladle dependency build.ini
```

Or:

```sh
java -jar lib/ladle.jar build build.ini
java -jar lib/ladle.jar dependency build.ini
```

Commands run with the INI file's directory as the working directory, so use paths relative to that file unless you specify absolute paths.

## Configuration file

Ladle reads a plain INI file. Sections use `[name]` headers. Each line inside a section is `key = value`. Lines starting with `#` or `;`, and blank lines, are ignored.

### Build (`build` command)

The `build` command compiles Java sources with `javac`. It requires `[javac]` and `[sources]` sections.

#### `[javac]`

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `path` | yes | — | JDK root directory. Use `$JAVA_HOME` for an installed JDK, or a project-local path such as `.jdk` (downloaded by `ladle dependency` when `download.*` URLs are configured). Supports `$VAR` and `${VAR}` environment expansion. |
| `download.windows` | no | — | Windows JDK archive URL. Used when `path` points at a missing local JDK. |
| `download.linux` | no | — | Linux JDK archive URL. |
| `download.macos` | no | — | macOS JDK archive URL. |
| `release` | no | — | Java platform version for `javac --release` (for example `17` or `21`). Prefer this over `source`/`target`. |
| `source` | no | — | Java source language level for `javac -source` (for example `17`). |
| `target` | no | — | Java bytecode level for `javac -target` (for example `17`). |
| `parameters` | no | *(empty)* | Extra arguments passed to `javac`, separated by spaces (for example `-encoding UTF-8 -d build`). |

#### `[sources]`

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `paths` | yes | — | Comma-separated list of source roots. Ladle walks each directory recursively and compiles every `.java` file it finds. |

#### `[resources]`

Optional. After compilation, Ladle copies non-Java files into the classes directory (from `[javac].parameters` `-d`, default `build/classes`) before JAR packaging.

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `paths` | no | — | Comma-separated resource roots. Every non-`.java` file under each root is copied into the classes directory, preserving its path relative to that root. Missing roots are skipped. |
| *other keys* | no | — | Explicit copy rules: `source = destination`, where `source` is relative to the project directory and `destination` is relative to the classes directory. Use for generated files or one-off assets (for example `build/generated/inject-MockMethodDispatcher.raw = inject-MockMethodDispatcher.raw`). |

Example:

```ini
[resources]
paths = src/main/resources, build/generated-resources
build/generated/inject-MockMethodDispatcher.raw = inject-MockMethodDispatcher.raw
```

During `ladle build`, the compile classpath includes JARs from `[subproject]` (as `dependencies/{name}.jar`), from `[dependencies]`, and from `[compileonlydependencies]` (downloaded JARs under `dependencies/`). Run `ladle dependency` first so dependency JARs and a missing project JDK exist on disk.

Example `build.ini` with a downloaded project JDK:

```ini
[javac]
path = .jdk
download.windows = https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse
download.linux = https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse
download.macos = https://api.adoptium.net/v3/binary/latest/21/ga/mac/x64/jdk/hotspot/normal/eclipse
release = 21
parameters = -encoding UTF-8 -d build/classes

[sources]
paths = src
```

Or set source and target separately:

```ini
[javac]
path = .jdk
source = 17
target = 17
parameters = -encoding UTF-8 -d build/classes
```

Or use an installed JDK explicitly:

```ini
[javac]
path = $JAVA_HOME
parameters = -encoding UTF-8 -d build/classes

[sources]
paths = src
```

This runs (conceptually):

```
{path}/bin/javac -encoding UTF-8 -d build <every .java file under src/ and test/>
```

On Windows, tool names use the `.exe` suffix (`javac.exe`, `java.exe`, `jar.exe`).

Long `javac` command lines are written to `{build}/javac.args` and invoked as `javac @{build}/javac.args` when they would exceed the platform command-line limit. Paths with spaces are supported.

#### `[build]`

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `directory` | no | `build` | Output directory removed by `ladle clear`. Also the default JAR output directory for `ladle release`. |

#### `[jar]`

Required for `ladle release`. Describes the JAR written after compilation and resource copying.

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `name` | no | project directory name | Output JAR filename without the `.jar` extension. |
| `directory` | no | `[build].directory` | Directory that receives the JAR (for example `build` or `lib`). |
| `manifest` | no | — | Path to a `MANIFEST.MF` file relative to the project directory. When set, Ladle runs `jar cfm` instead of `jar cf`. |
| `main-class` | no | — | Shorthand for a generated manifest with `Main-Class`. Ignored when `manifest` is set. |
| `include` | no | all files | Comma-separated Ant-style globs of paths relative to the classes directory. When omitted, every file is packaged (`jar … -C {classes} .`). |
| `exclude` | no | — | Comma-separated Ant-style globs to omit after `include`. `*` does not cross `/`; `**` matches any depth. |
| *other keys* | no | — | Additional manifest attributes when no `manifest` file is configured (for example `premain-class = org.example.Agent`). |

Example:

```ini
[jar]
name = ladle
directory = lib
manifest = manifest/MANIFEST.MF
```

Or without a manifest file:

```ini
[jar]
name = ladle
main-class = thelaboflieven.info.Ladle
```

Filter packaged entries with `include` and `exclude`. Patterns are matched against the path inside the classes directory (`build/classes/org/example/Foo.class` is `org/example/Foo.class`). When either key is set, Ladle lists matching files on the `jar` command instead of `.`.

```ini
[jar]
name = mylib
include = **/*.class, **/*.properties, META-INF/**
exclude = module-info.class, **/internal/**
```

This writes `build/myapp.jar` when you run `./bin/ladle release build.ini`.

#### `[subproject]`

Subprojects are built recursively before the current project. Each entry uses `name = path`, where `path` is a directory containing a `build.ini`. The subproject is compiled, packaged as a JAR, and written to `dependencies/{name}.jar` for use when compiling the parent.

Example:

```ini
[subproject]
lib = lib
utils = ../shared/utils
```

Build order:

1. Build each subproject (and their subprojects) recursively.
2. Publish each subproject JAR to `dependencies/{name}.jar`.
3. Compile the current project with those JARs on the classpath.

### Dependencies (`dependency` command)

The `dependency` command downloads a missing project JDK (when configured) and JARs listed in the INI into `dependencies/`:

```sh
./bin/ladle dependency build.ini
```

#### `[dependencies]`

Implementation dependencies (Gradle `implementation`) use `name = url` pairs. The name may be a local JAR filename or a Java package (for example `net.bytebuddy`); package names are saved using the filename from the URL. Files are written under `dependencies/`. Ladle adds each dependency to the `javac` classpath during `ladle build`.

Example:

```ini
[dependencies]
net.bytebuddy = https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.17.7/byte-buddy-1.17.7.jar
objenesis.jar = https://repo1.maven.org/maven2/org/objenesis/objenesis/3.3/objenesis-3.3.jar
```

#### `[compileonlydependencies]`

Compile-only dependencies (Gradle `compileOnly`) use the same `name = url` format. Files are written under `dependencies/`. Ladle adds each dependency to the main `javac` classpath and to the test compile classpath, but not to the test runtime classpath. Use this for APIs needed at compile time only (for example JUnit 4, Hamcrest, or JSpecify annotations on a library that does not bundle them).

Example:

```ini
[compileonlydependencies]
org.jspecify = https://repo1.maven.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar
org.junit = https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
```

#### `[testdependencies]`

Test dependencies (Gradle `testImplementation`) use the same `name = url` format. Files are written under `dependencies/`. Ladle adds each dependency to the test compile and runtime classpaths automatically.

Example:

```ini
[testdependencies]
org.junit.jupiter.api = https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/6.1.0/junit-jupiter-api-6.1.0.jar
junit-platform-console-standalone-6.1.0.jar = https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.0/junit-platform-console-standalone-6.1.0.jar
```

If `[dependencies]`, `[compileonlydependencies]`, and `[testdependencies]` are all empty and `[javac]` has no JDK to install, `ladle dependency` prints a warning and exits successfully.

### Tests (`test` command)

The `test` command compiles optional `[testfixtures]` sources, then test sources, and runs them with JUnit 5 (default) or JUnit 4. It requires a `[test]` section. Main sources must be built first (`ladle build`), and JUnit JARs must be present (`ladle dependency`).

#### `[test]`

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `sources` | yes | — | Comma-separated test source roots. Ladle finds classes named `*Test.java`. |
| `classpath` | no | `build/classes` | Comma-separated extra classpath entries. Entries from `[testdependencies]` are added to the test compile and runtime classpaths; entries from `[compileonlydependencies]` are added to the test compile classpath only. |
| `output` | no | `build/test-classes` | Directory for compiled test classes. |
| `runner` | no | `org.junit.platform.console.ConsoleLauncher` | Main class used to run tests. JUnit 5 (default): `execute --details-theme=ascii --select-class` for each `*Test` class. JUnit 4: set `runner = org.junit.runner.JUnitCore`; Ladle passes the test class names as arguments. |
| `path` | no | `[javac].path` | JDK root when different from the build JDK. |

Example:

```ini
[testdependencies]
org.junit.jupiter.api = https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/6.1.0/junit-jupiter-api-6.1.0.jar
junit-platform-console-standalone-6.1.0.jar = https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.0/junit-platform-console-standalone-6.1.0.jar

[test]
sources = test
classpath = build/classes
output = build/test-classes
```

If no `*Test.java` files are found, Ladle prints a warning and exits successfully.

JUnit 4:

```ini
[testdependencies]
junit-4.13.2.jar = https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
hamcrest-core-1.3.jar = https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar

[test]
sources = test
classpath = build/classes
output = build/test-classes
runner = org.junit.runner.JUnitCore
```

That runs `java -cp … org.junit.runner.JUnitCore example.AppTest`. JUnit 4.13 needs Hamcrest on the runtime classpath. JUnit 4 tests can also run through the default ConsoleLauncher if `junit-vintage-engine` is on `[testdependencies]`.

#### `[testfixtures]`

Optional. Compiles shared test utilities (Gradle `testFixtures`) to a separate output directory and puts that directory first on the test compile and runtime classpaths. Use this for helpers that are not named `*Test.java`.

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `sources` | yes | — | Comma-separated fixture source roots. Ladle compiles every `.java` file it finds. |
| `classpath` | no | `build/classes` | Comma-separated extra classpath entries used when compiling fixtures. Entries from `[testdependencies]` are added to the fixture compile classpath; entries from `[compileonlydependencies]` are added to the fixture compile classpath only. |
| `output` | no | `build/test-fixtures-classes` | Directory for compiled fixture classes. Prepended to the test compile and runtime classpaths. |

Example:

```ini
[testfixtures]
sources = src/testFixtures/java
classpath = build/classes
output = build/test-fixtures-classes

[test]
sources = src/test/java
classpath = build/classes
output = build/test-classes
```

During `ladle test`, Ladle:

1. Compiles all `.java` files under `[testfixtures].sources` to `[testfixtures].output`.
2. Prepends that output directory to the test compile and runtime classpaths.
3. Compiles `*Test.java` files and runs them as usual.

Long fixture `javac` command lines are written to `{build}/test-fixtures-javac.args`.

### Full example

```ini
[javac]
path = .jdk
download.linux = https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse
parameters = -encoding UTF-8 -d build/classes

[sources]
paths = src

[testdependencies]
org.junit.jupiter.api = https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/6.1.0/junit-jupiter-api-6.1.0.jar
junit-platform-console-standalone-6.1.0.jar = https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.0/junit-platform-console-standalone-6.1.0.jar

[testfixtures]
sources = src/testFixtures/java

[test]
sources = test
classpath = build/classes
```

Workflow:

1. `./bin/ladle dependency build.ini` — fetch JDK and JARs.
2. `./bin/ladle build build.ini` — compile sources.
3. `./bin/ladle release build.ini` — compile sources and package a JAR (requires `[jar]`).
4. `./bin/ladle test build.ini` — compile and run unit tests.

## Command reference

| Command | Arguments | Description |
|---------|-----------|-------------|
| *(none)* | — | Print welcome message. |
| `--help` | — | Print brief usage (exits with status 1). |
| `build` | `<ini-file>` | Compile Java sources described in the INI file. |
| `release` | `<ini-file>` | Compile sources, copy resources, and package a JAR (requires `[jar]`). |
| `dependency` | `<ini-file>` | Download a missing project JDK (when configured) and JAR dependencies from the INI file. |
| `test` | `<ini-file>` | Compile and run unit tests described in the INI file. |
| `clear` | `<ini-file>` | Delete the build directory described in the INI file. |

If the INI path is missing or not readable, Ladle prints an error and exits with status 2.

See `examples/` for sample projects used to test builds, including subprojects, test fixtures, JUnit 4, and a failing compile.
