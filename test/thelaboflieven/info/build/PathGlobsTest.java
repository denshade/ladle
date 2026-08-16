package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathGlobsTest {
    @Test
    void starDoesNotCrossDirectories() {
        assertTrue(PathGlobs.matches("Foo.class", "*.class"));
        assertFalse(PathGlobs.matches("a/Foo.class", "*.class"));
    }

    @Test
    void doubleStarMatchesAnyDepth() {
        assertTrue(PathGlobs.matches("Foo.class", "**/*.class"));
        assertTrue(PathGlobs.matches("a/b/Foo.class", "**/*.class"));
        assertFalse(PathGlobs.matches("a/b/Foo.properties", "**/*.class"));
    }

    @Test
    void doubleStarMatchesEverything() {
        assertTrue(PathGlobs.matches("Foo.class", "**"));
        assertTrue(PathGlobs.matches("a/b/config.properties", "**"));
    }

    @Test
    void packagePrefixMatchesDescendants() {
        assertTrue(PathGlobs.matches("org/example/Foo.class", "org/example/**"));
        assertTrue(PathGlobs.matches("org/example/nested/Foo.class", "org/example/**"));
        assertFalse(PathGlobs.matches("org/other/Foo.class", "org/example/**"));
        assertFalse(PathGlobs.matches("org/example", "org/example/**"));
    }

    @Test
    void internalDirectoryPatternMatchesAnyDepth() {
        assertTrue(PathGlobs.matches("internal/X.class", "**/internal/**"));
        assertTrue(PathGlobs.matches("org/internal/X.class", "**/internal/**"));
        assertFalse(PathGlobs.matches("org/X.class", "**/internal/**"));
    }

    @Test
    void questionMarkMatchesSinglePathCharacter() {
        assertTrue(PathGlobs.matches("Foo.class", "?oo.class"));
        assertFalse(PathGlobs.matches("Floo.class", "?oo.class"));
        assertFalse(PathGlobs.matches("a/Foo.class", "?oo.class"));
    }

    @Test
    void dotsAreLiteral() {
        assertTrue(PathGlobs.matches("Foo.class", "Foo.class"));
        assertFalse(PathGlobs.matches("FooXclass", "Foo.class"));
    }

    @Test
    void backslashesNormalizeToForwardSlashes() {
        assertTrue(PathGlobs.matches("a\\b\\Foo.class", "a/b/Foo.class"));
        assertTrue(PathGlobs.matches("a/b/Foo.class", "a\\b\\*.class"));
    }

    @Test
    void parseListSplitsOnCommasAndTrims() {
        assertEquals(List.of("**/*.class", "**/*.properties"), PathGlobs.parseList(" **/*.class , **/*.properties "));
        assertEquals(List.of(), PathGlobs.parseList(""));
        assertEquals(List.of(), PathGlobs.parseList(null));
    }

    @Test
    void omittedIncludeAcceptsEverythingUntilExclude() {
        var globs = PathGlobs.fromJarSection(Map.of("exclude", "module-info.class, **/Drop.class"));

        assertTrue(globs.hasFilters());
        assertTrue(globs.accepts("example/Keep.class"));
        assertTrue(globs.accepts("example/config.properties"));
        assertFalse(globs.accepts("module-info.class"));
        assertFalse(globs.accepts("example/Drop.class"));
    }

    @Test
    void includeRestrictsThenExcludeRemoves() {
        var globs = PathGlobs.fromJarSection(Map.of(
                "include", "**/*.class, **/*.properties",
                "exclude", "module-info.class"));

        assertTrue(globs.accepts("example/Keep.class"));
        assertTrue(globs.accepts("example/config.properties"));
        assertFalse(globs.accepts("module-info.class"));
        assertFalse(globs.accepts("META-INF/services/example.Api"));
    }

    @Test
    void missingSectionHasNoFiltersAndAcceptsAll() {
        var globs = PathGlobs.fromJarSection(null);

        assertFalse(globs.hasFilters());
        assertTrue(globs.accepts("anything.class"));
    }
}
