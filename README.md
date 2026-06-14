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

## Installation

Install Ladle once, then run `ladle` from any directory (similar to a global Gradle install).

### Build from source

```powershell
.\build.ps1
```

```sh
./build.sh
```

This creates `lib/ladle.jar`.

### Install

**Windows** (installs to `%LOCALAPPDATA%\Programs\ladle` and adds it to your user PATH):

```powershell
.\install.ps1
```

**Linux / macOS** (installs to `~/.local/ladle` and updates your shell profile):

```sh
./install.sh
```

System-wide install on Unix:

```sh
./install.sh --system
```

Custom location:

```sh
./install.sh --prefix /opt
```

After installation, open a new terminal. The launcher reads `LADLE_HOME` if set; otherwise it resolves `lib/ladle.jar` relative to the install layout:

```
ladle/
  bin/ladle          # Unix launcher
  bin/ladle.cmd      # Windows launcher
  lib/ladle.jar
```

### Usage

```sh
ladle build build.ini
ladle dependency build.ini
ladle test build.ini
ladle --help
```

You can also run from a checkout without installing:

```powershell
.\bin\ladle.ps1 build build.ini
```

```sh
./bin/ladle build build.ini
```

## Quick start

Run Ladle with a path to your build INI file:

```sh
ladle build build.ini
ladle dependency build.ini
```

Or without installing:

```sh
java -jar lib/ladle.jar build build.ini
```

Commands run with the INI file's directory as the working directory, so use paths relative to that file unless you specify absolute paths.

## Configuration file

Ladle reads a plain INI file. Sections use `[name]` headers. Each line inside a section is `key = value`. Lines starting with `#` or `;`, and blank lines, are ignored.

### Build (`build` command)

The `build` command compiles Java sources with `javac`. It requires `[javac]` and `[sources]` sections.

#### `[javac]`

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `path` | no | `javac` | Root directory of the JDK installation. Ladle runs `{path}\bin\javac.exe` (Windows). The path must not contain spaces (see note below). |
| `parameters` | no | *(empty)* | Extra arguments passed to `javac`, separated by spaces (for example `-encoding UTF-8 -d build`). |

#### `[sources]`

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `paths` | yes | — | Comma-separated list of source roots. Ladle walks each directory recursively and compiles every `.java` file it finds. |

During `ladle build`, the compile classpath includes JARs from `[subproject]` (as `dependencies/{name}.jar`) and from `[dependencies]` (downloaded JARs under `dependencies/`). Run `ladle dependency` first so dependency JARs exist on disk.

Example `build.ini`:

```ini
[javac]
path = C:\Java\jdk-21
parameters = -encoding UTF-8 -d build

[sources]
paths = src,test
```

This runs (conceptually):

```
C:\Java\jdk-21\bin\javac.exe -encoding UTF-8 -d build <every .java file under src/ and test/>
```

**Path limitation:** Ladle splits the final command on spaces before execution. Values in `path`, `parameters`, and comma-separated entries in `paths` must not contain spaces. Use a JDK install path without spaces, or a directory junction/symlink to one.

#### `[build]`

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `directory` | no | `build` | Output directory removed by `ladle clear`. |

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

The `dependency` command downloads JARs listed in the INI into the project-local `dependencies/` directory (similar to Gradle's dependency cache, but stored in the project).

You can also use the download scripts from a project directory:

```powershell
.\download.ps1
```

```sh
./download.sh
```

Copy `download.ps1` / `download.sh` from `bin/` into your project, or run them from an installed Ladle (`download` on PATH after `install.ps1` / `install.sh`).

#### `[dependencies]`

Compile/runtime dependencies use `name = url` pairs. The name may be a local JAR filename or a Java package (for example `net.bytebuddy`); package names are saved using the filename from the URL. Files are written under `dependencies/`. Ladle adds each dependency to the `javac` classpath during `ladle build`.

Example:

```ini
[dependencies]
net.bytebuddy = https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.17.7/byte-buddy-1.17.7.jar
objenesis.jar = https://repo1.maven.org/maven2/org/objenesis/objenesis/3.3/objenesis-3.3.jar
```

#### `[testdependencies]`

Test dependencies use the same `name = url` format. Files are written under `dependencies/`. Ladle adds each dependency to the test classpath automatically.

Example:

```ini
[testdependencies]
org.junit.jupiter.api = https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/6.1.0/junit-jupiter-api-6.1.0.jar
junit-platform-console-standalone-6.1.0.jar = https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.0/junit-platform-console-standalone-6.1.0.jar
```

If neither `[dependencies]` nor `[testdependencies]` lists anything to download, `ladle dependency` prints a warning and exits successfully.

### Tests (`test` command)

The `test` command compiles test sources and runs them with JUnit 5. It requires a `[test]` section. Main sources must be built first (`ladle build`), and JUnit JARs must be present (`ladle dependency`).

#### `[test]`

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `sources` | yes | — | Comma-separated test source roots. Ladle finds classes named `*Test.java`. |
| `classpath` | no | `build/classes` | Comma-separated extra classpath entries. Entries from `[testdependencies]` are added automatically. |
| `output` | no | `build/test-classes` | Directory for compiled test classes. |
| `runner` | no | `org.junit.platform.console.ConsoleLauncher` | Main class used to run tests. Ladle invokes `execute --details-theme=ascii --select-class` for each `*Test` class found. |
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

### Full example

```ini
[javac]
path = C:\Java\jdk-21
parameters = -encoding UTF-8 -d build/classes

[sources]
paths = src

[testdependencies]
org.junit.jupiter.api = https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/6.1.0/junit-jupiter-api-6.1.0.jar
junit-platform-console-standalone-6.1.0.jar = https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.0/junit-platform-console-standalone-6.1.0.jar

[test]
sources = test
classpath = build/classes
```

Workflow:

1. `ladle dependency build.ini` — fetch JARs.
2. `ladle build build.ini` — compile sources.
3. `ladle test build.ini` — compile and run unit tests.

## Command reference

| Command | Arguments | Description |
|---------|-----------|-------------|
| *(none)* | — | Print welcome message. |
| `--help` | — | Print brief usage (exits with status 1). |
| `build` | `<ini-file>` | Compile Java sources described in the INI file. |
| `dependency` | `<ini-file>` | Download dependencies described in the INI file. |
| `test` | `<ini-file>` | Compile and run unit tests described in the INI file. |
| `clear` | `<ini-file>` | Delete the build directory described in the INI file. |

If the INI path is missing or not readable, Ladle prints an error and exits with status 2.

See `examples/` for sample projects used to test builds, including subprojects and a failing compile.
