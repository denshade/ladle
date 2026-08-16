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

`api` and other Gradle scopes are not supported yet. Own-module test fixtures are compiled via `[testfixtures]` (issue #10); consuming another project's test fixtures as a dependency scope is not.

**Affected code:** [DependencyInstaller.java](src/thelaboflieven/info/download/DependencyInstaller.java), [CompileOnlyDependencies.java](src/thelaboflieven/info/download/CompileOnlyDependencies.java), [CompileClasspath.java](src/thelaboflieven/info/build/CompileClasspath.java), [TestCommandBuilder.java](src/thelaboflieven/info/test/TestCommandBuilder.java)

---

## 3. Non-Java resource processing

**Status:** fixed

Ladle copies non-Java files from `[resources]` into the classes directory after `javac` and before JAR packaging. Use `paths` for resource roots and additional `source = destination` entries for explicit copy rules.

**Affected code:** [ResourceCopier.java](src/thelaboflieven/info/build/ResourceCopier.java), [BuildOrchestrator.java](src/thelaboflieven/info/build/BuildOrchestrator.java)

---

## 4. JAR packaging for root modules

**Status:** fixed

`ladle release` compiles the root module, copies resources, and packages a JAR using `[jar].name` and `[jar].directory`. `[jar].include` and `[jar].exclude` are comma-separated Ant-style globs of paths relative to the classes directory. Subprojects are still published automatically to `dependencies/{name}.jar` during parent builds.

**Affected code:** [CompileOrchestrator.java](src/thelaboflieven/info/build/CompileOrchestrator.java), [JarCommandBuilder.java](src/thelaboflieven/info/build/JarCommandBuilder.java), [PathGlobs.java](src/thelaboflieven/info/build/PathGlobs.java), [Ladle.java](src/thelaboflieven/info/Ladle.java)

---

## 5. Cross-platform JDK tool resolution

**Status:** fixed

Ladle resolves `javac`, `java`, and `jar` under `{jdk}/bin/` using the `.exe` suffix on Windows and plain names elsewhere.

**Affected code:** [BuildConfig.java](src/thelaboflieven/info/build/BuildConfig.java), [TestCommandBuilder.java](src/thelaboflieven/info/test/TestCommandBuilder.java)

---

## 6. Command tokenization splits on spaces

**Status:** fixed

Ladle passes argv as `List<String>` end-to-end to `ProcessBuilder`. Long `javac` invocations are written to `{build}/javac.args` (or `{build}/test-javac.args` for tests) and invoked with `@file` when the command line would exceed the platform limit.

**Affected code:** [CommandLine.java](src/thelaboflieven/info/CommandLine.java), [CommandsRunner.java](src/thelaboflieven/info/CommandsRunner.java), [JavacCommandBuilder.java](src/thelaboflieven/info/build/JavacCommandBuilder.java), [TestCommandBuilder.java](src/thelaboflieven/info/test/TestCommandBuilder.java)

---

## 7. JUnit 4 test execution

**Status:** fixed

`ladle test` supports JUnit 5 via `org.junit.platform.console.ConsoleLauncher` (default) and JUnit 4 via `[test].runner = org.junit.runner.JUnitCore`. JUnitCore is invoked with the discovered `*Test` class names as arguments. JUnit 4 tests can also run through ConsoleLauncher when `junit-vintage-engine` is on `[testdependencies]`.

**Affected code:** [TestCommandBuilder.java](src/thelaboflieven/info/test/TestCommandBuilder.java), [Ladle.java](src/thelaboflieven/info/Ladle.java)

---

## 8. Annotation processor support

**Status:** open

There is no first-class support for annotation processors (`-processor`, processor classpath). Flags can be appended manually to `[javac].parameters`, but processor JARs and `-cp` orchestration are left to the user.

**Affected code:** [JavacCommandBuilder.java](src/thelaboflieven/info/build/JavacCommandBuilder.java)

**Impact:** `mockito-errorprone` uses AutoService (`annotationProcessor`). Without structured support, the build.ini becomes fragile.

**Fix direction:** Add `[annotationProcessor]` or extend `[dependencies]` with a `processor` scope that wires `-processor` and `-processorpath` automatically.

---

## 9. Java Platform Module System (JPMS)

**Status:** demoted (no first-class support)

JPMS is uncommon. A `[module]` section, derived `--module-path` heuristics, and test-time `--add-reads` are not worth INI surface.

`[javac].parameters` already accepts `--module-path`, `--add-modules`, `--add-exports`, and `--add-reads`. If `module-info.java` in `[sources].paths` makes `javac` fail, exclude that file or put the flags in `parameters` — do not add a module system to Ladle.

**Affected code:** [JavacCommandBuilder.java](src/thelaboflieven/info/build/JavacCommandBuilder.java)

**Impact:** Mockito core ships `module-info.java`. A classpath-only artifact is still useful to almost every consumer. Faithful modular compile is optional.

**Fix direction (optional, later):** Source include/exclude, or if `module-info.java` is present put dependency JARs on `--module-path` and print that line. No `[module]` section.

---

## 10. Test fixtures source set

**Status:** fixed

`[testfixtures]` compiles shared test utilities (all `.java` files, not only `*Test.java`) to a separate output directory (`build/test-fixtures-classes` by default) and prepends that directory to the test compile and runtime classpaths during `ladle test`.

**Affected code:** [TestCommandBuilder.java](src/thelaboflieven/info/test/TestCommandBuilder.java)

**Impact:** Mockito core keeps test helpers under `src/testFixtures/java/` (e.g. `org.mockitoutil.*`). Tests depend on this output being compiled and on the classpath before main tests compile.

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

1. ~~**#5** Cross-platform tools~~ — done
2. ~~**#6** Command tokenization~~ — done
3. ~~**#1** Dependency classpath~~ — done; ~~**#2** compile-only scopes~~ — done (`[compileonlydependencies]`)
4. ~~**#3** Resources~~ — done; ~~**#4** JAR include/exclude~~ — done
5. ~~**#10** Test fixtures~~ — done; ~~**#7** JUnit 4~~ — done
6. **#8** Annotation processors — required for mockito-errorprone
7. **#11** Incremental compile — quality-of-life
8. ~~**#9** JPMS~~ — demoted; use `[javac].parameters` or exclude `module-info.java`
