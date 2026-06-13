#!/usr/bin/env sh
set -eu

INI_FILE="build.ini"
if [ $# -gt 0 ]; then
  case "$1" in
    -h|--help)
      cat <<EOF
Usage: $(basename "$0") [<ini-file>]

Download and install JAR dependencies into dependencies/ for the current project.
Reads [dependencies] and [testdependencies] from build.ini (default: build.ini).

Uses a local ladle checkout when present, otherwise LADLE_HOME or ladle on PATH.
EOF
      exit 0
      ;;
    *)
      INI_FILE="$1"
      ;;
  esac
fi

if [ ! -f "$INI_FILE" ]; then
  echo "Cannot read $INI_FILE" >&2
  exit 2
fi

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

run_ladle() {
  "$@" dependency "$INI_FILE"
}

if [ -f "$SCRIPT_DIR/ladle" ]; then
  run_ladle "$SCRIPT_DIR/ladle"
  exit $?
fi

if [ -f "$SCRIPT_DIR/../lib/ladle.jar" ]; then
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
  else
    JAVA="java"
  fi
  run_ladle "$JAVA" -jar "$SCRIPT_DIR/../lib/ladle.jar"
  exit $?
fi

if [ -n "${LADLE_HOME:-}" ] && [ -f "$LADLE_HOME/bin/ladle" ]; then
  run_ladle "$LADLE_HOME/bin/ladle"
  exit $?
fi

if command -v ladle >/dev/null 2>&1; then
  run_ladle ladle
  exit $?
fi

echo "Cannot find ladle. Install it or set LADLE_HOME." >&2
exit 1
