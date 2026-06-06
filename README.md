thelaboflieven.info.Ladle is a build system that brings modern capabilities, but rejects framework complication.

What makes frameworks handy is that they bring default functionality.
So you don't need to know the details. 

Problems are: 
1. Hard to debug, I added this field, why is it not working or triggered?
2. Hard to optimize. Where do you even start?
3. High learning curve if you need to change some out of the ordinary.
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

### Dependencies (`dependency` command)

The `dependency` command downloads files listed in the INI. It requires a `[dependencies]` section.

#### `[dependencies]`

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `implementation` | yes | — | Comma-separated list of URLs. Each URL is downloaded with PowerShell `wget`; the local filename is the last segment of the URL path. |

Example:

```ini
[dependencies]
implementation = https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar,https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar
```

Downloads `junit-4.13.2.jar` and `hamcrest-core-1.3.jar` into the INI file's directory.

### Full example

```ini
[javac]
path = C:\Java\jdk-21
parameters = -encoding UTF-8 -d build -cp junit-4.13.2.jar

[sources]
paths = src

[dependencies]
implementation = https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
```

Workflow:

1. `ladle dependency build.ini` — fetch JARs.
2. `ladle build build.ini` — compile sources.

## Command reference

| Command | Arguments | Description |
|---------|-----------|-------------|
| *(none)* | — | Print welcome message. |
| `--help` | — | Print brief usage (exits with status 1). |
| `build` | `<ini-file>` | Compile Java sources described in the INI file. |
| `dependency` | `<ini-file>` | Download dependencies described in the INI file. |

If the INI path is missing or not readable, Ladle prints an error and exits with status 2.
