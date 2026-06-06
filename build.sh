#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
CLASSES="$ROOT/build/classes"
LIB="$ROOT/lib"

resolve_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jar" ]; then
    printf '%s' "$JAVA_HOME"
    return
  fi

  JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 | awk -F' = ' '/java\.home/ { print $2; exit }')"
  if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/jar" ]; then
    printf '%s' "$JAVA_HOME"
    return
  fi

  echo "Cannot find a JDK. Install Java or set JAVA_HOME." >&2
  exit 1
}

JAVA_HOME="$(resolve_java_home)"
JAVAC="$JAVA_HOME/bin/javac"
JAR="$JAVA_HOME/bin/jar"

mkdir -p "$CLASSES" "$LIB"

SOURCES="$(find "$ROOT/src" -name '*.java' | tr '\n' ' ')"
# shellcheck disable=SC2086
"$JAVAC" -d "$CLASSES" $SOURCES
"$JAR" cfm "$LIB/ladle.jar" "$ROOT/manifest/MANIFEST.MF" -C "$CLASSES" .

echo "Built $LIB/ladle.jar"
