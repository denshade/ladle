# Ladle issues

Track ladle limitations discovered while planning a [Mockito](https://github.com/mockito/mockito) core-library migration. Fix these one by one to reduce workarounds in downstream projects.

Items solvable with external scripts alone (OSGi/Bnd, Spotless, Maven publish, etc.) are intentionally omitted.

---

## 1. Wire `[dependencies]` into the compile classpath

**Status:** fixed

`ladle dependency` downloads JARs to `dependencies/`, and `ladle build` adds `[dependencies]` JARs to the `javac` classpath together with subproject JARs.

**Affected code:** [CompileClasspath.java](src/thelaboflieven/info/build/CompileClasspath.java), [JavacCommandBuilder.java](src/thelaboflieven/info/build/JavacCommandBuilder.java), [ImplementationDependencies.java](src/thelaboflieven/info/download/ImplementationDependencies.java)

---

## 2. Dependency scopes (`compileOnly`, `implementation`, `test`, …)

**Status:** fixed (partial — `compileOnly` and `test` scopes)

Ladle now has three dependency sections:

| Section | Gradle equivalent | Main compile | Test compile | Test runtime |
|---------|-------------------|--------------|--------------|--------------|
| `[dependencies]` | `implementation` | yes | via `[test].classpath` | via `[test].classpath` |
| `[compileonlydependencies]` | `compileOnly` | yes | yes | no |
| `[testdependencies]` | `testImplementation` | no | yes | yes |

`api`, `testFixtures`, and other Gradle scopes are not supported yet.

**Affected code:** [DependencyInstaller.java](src/thelaboflieven/info/download/DependencyInstaller.java), [CompileOnlyDependencies.java](src/thelaboflieven/info/download/CompileOnlyDependencies.java), [CompileClasspath.java](src/thelaboflieven/info/build/CompileClasspath.java), [TestCommandBuilder.java](src/thelaboflieven/info/test/TestCommandBuilder.java)

---

## 3. Non-Java resource processing

**Status:** open

Ladle compiles `.java` files only. It does not copy resources from source trees or generated output directories into the classes directory before packaging.

**Affected code:** [JavacCommandBuilder.java](src/thelaboflieven/info/build/JavacCommandBuilder.java), [BuildOrchestrator.java](src/thelaboflieven/info/build/BuildOrchestrator.java)

**Impact:** Mockito core generates an inline-mock resource (`inject-MockMethodDispatcher.raw`) from a compiled class. Any project with `src/main/resources` or generated assets needs external scripts today.

**Fix direction:** Add a `[resources]` INI section (source dirs and/or copy rules) that runs before JAR packaging.

---

## 4. JAR packaging for root modules

**Status:** open

`jar` is only invoked when a module is built as a **subproject** (published to `dependencies/{name}.jar`). The root module of a build gets compile-only; there is no `ladle jar` command and no manifest/exclude support.

**Affected code:** [BuildOrchestrator.java](src/thelaboflieven/info/build/BuildOrchestrator.java), [JarCommandBuilder.java](src/thelaboflieven/info/build/JarCommandBuilder.java)

**Impact:** Mockito core is the root module and needs a production JAR with manifest attributes (`Premain-Class`, `Can-Retransform-Classes`), excluded classes, and bundled resources. Subproject JARs are a bare `jar cf` with no customization.

**Fix direction:** Add `ladle jar` (or a `[jar]` INI section) with manifest entries, include/exclude patterns, and optional main-class / agent attributes.

---

## 5. Cross-platform JDK tool resolution

**Status:** open

Tool paths are hardcoded as `javac.exe`, `java.exe`, and `jar.exe`.

**Affected code:** [BuildConfig.java](src/thelaboflieven/info/build/BuildConfig.java), [TestCommandBuilder.java](src/thelaboflieven/info/test/TestCommandBuilder.java)

**Impact:** Ladle does not run on Linux or macOS without code changes. Mockito CI is Linux-based.

**Fix direction:** Resolve tool names by OS (`javac` vs `javac.exe`) or use `JavaHome/bin/` + standard executable names.

---

## 6. Command tokenization splits on spaces

**Status:** open

The full command string is split on spaces before `ProcessBuilder` runs it.

**Affected code:** [CommandsRunner.java](src/thelaboflieven/info/CommandsRunner.java)

**Impact:** JDK paths with spaces, long classpaths, and `@argfile` references break. Mockito's dependency classpath will exceed practical limits quickly.

**Fix direction:** Pass argv as a `List<String>` end-to-end instead of joining and re-splitting; support `@file` argfiles for large source/classpath lists.

---

## 7. JUnit 4 test execution

**Status:** open

`ladle test` only supports JUnit 5 via `org.junit.platform.console.ConsoleLauncher`. JUnit 4 runners are explicitly rejected.

**Affected code:** [TestCommandBuilder.java](src/thelaboflieven/info/test/TestCommandBuilder.java)

**Impact:** Mockito core has ~380 test classes using `org.junit.Test`. They cannot run via `ladle test` today.

**Fix direction:** Support JUnit Vintage (e.g. `--select-class` via ConsoleLauncher with vintage engine on classpath, or allow configuring the runner without hard rejection of JUnit 4).

---

## 8. Annotation processor support

**Status:** open

There is no first-class support for annotation processors (`-processor`, processor classpath). Flags can be appended manually to `[javac].parameters`, but processor JARs and `-cp` orchestration are left to the user.

**Affected code:** [JavacCommandBuilder.java](src/thelaboflieven/info/build/JavacCommandBuilder.java)

**Impact:** `mockito-errorprone` uses AutoService (`annotationProcessor`). Without structured support, the build.ini becomes fragile.

**Fix direction:** Add `[annotationProcessor]` or extend `[dependencies]` with a `processor` scope that wires `-processor` and `-processorpath` automatically.

---

## 9. Java Platform Module System (JPMS)

**Status:** open

No INI support for `--module-path`, `--add-modules`, `--add-reads`, or `--add-exports`. Modular projects must encode all JPMS flags manually in `[javac].parameters`.

**Affected code:** [JavacCommandBuilder.java](src/thelaboflieven/info/build/JavacCommandBuilder.java)

**Impact:** Mockito core and mockito-junit-jupiter ship `module-info.java` with `requires`/`exports` directives. Modular compile needs correct module-path layout for dependencies.

**Fix direction:** Add `[module]` INI section or derive module-path from modular JARs in `dependencies/`.

---

## 10. Test fixtures source set

**Status:** open

Gradle's `testFixtures` compiles shared test utilities separately and exposes them on the test classpath. Ladle has a single `[test].sources` root and no concept of a secondary compile pass for shared test code.

**Affected code:** [TestCommandBuilder.java](src/thelaboflieven/info/test/TestCommandBuilder.java)

**Impact:** Mockito core keeps test helpers under `src/testFixtures/java/` (e.g. `org.mockitoutil.*`). Tests depend on this output being compiled and on the classpath before main tests compile.

**Fix direction:** Add `[testfixtures]` section (sources + classpath) compiled to a separate output dir and prepended to the test classpath.

---

## 11. Incremental compilation

**Status:** open

Every `ladle build` walks all source roots and invokes `javac` on every `.java` file. There is no change detection, build cache, or incremental compile.

**Affected code:** [JavacCommandBuilder.java](src/thelaboflieven/info/build/JavacCommandBuilder.java), [BuildOrchestrator.java](src/thelaboflieven/info/build/BuildOrchestrator.java)

**Impact:** Mockito core alone has ~530 main sources. Full recompiles on every build are slow compared to Gradle.

**Fix direction:** Optional incremental mode (mtime check, `-sourcepath` strategy, or delegating to `javac` with an explicit changed-files list). Lower priority than correctness issues above.

---

## Suggested fix order

Prioritize issues that unblock Mockito core compilation and packaging without scripts:

1. **#5** Cross-platform tools — required for CI
2. **#6** Command tokenization — required for any real classpath
3. ~~**#1** Dependency classpath~~ — done; ~~**#2** compile-only scopes~~ — done (`[compileonlydependencies]`)
4. **#9** JPMS — required for mockito-core module-info
5. **#3 + #4** Resources + JAR — required for a correct artifact
6. **#10 + #7** Test fixtures + JUnit 4 — required for running mockito-core tests
7. **#8** Annotation processors — required for mockito-errorprone
8. **#11** Incremental compile — quality-of-life
