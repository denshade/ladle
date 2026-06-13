#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PREFIX="${HOME}/.local"
USER_INSTALL=1

usage() {
  cat <<EOF
Usage: $(basename "$0") [--prefix DIR] [--system]

Install Ladle so the ladle command is available on your PATH.

Options:
  --prefix DIR   Install under DIR/ladle (default: ~/.local/ladle)
  --system       Install under /usr/local/ladle (requires write access to /usr/local)

After installation, add the bin directory to your PATH if the installer did not
already update your shell profile:

  export LADLE_HOME="$PREFIX/ladle"
  export PATH="\$LADLE_HOME/bin:\$PATH"
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --prefix)
      PREFIX="$2"
      USER_INSTALL=0
      shift 2
      ;;
    --system)
      PREFIX="/usr/local"
      USER_INSTALL=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

INSTALL_DIR="$PREFIX/ladle"

if [ ! -f "$ROOT/lib/ladle.jar" ]; then
  echo "Building ladle.jar..."
  "$ROOT/build.sh"
fi

mkdir -p "$INSTALL_DIR/bin" "$INSTALL_DIR/lib"
cp "$ROOT/bin/ladle" "$INSTALL_DIR/bin/ladle"
cp "$ROOT/bin/download.sh" "$INSTALL_DIR/bin/download.sh"
cp "$ROOT/lib/ladle.jar" "$INSTALL_DIR/lib/ladle.jar"
chmod +x "$INSTALL_DIR/bin/ladle"
chmod +x "$INSTALL_DIR/bin/download.sh"

PATH_LINE="export PATH=\"\$LADLE_HOME/bin:\$PATH\""
HOME_LINE="export LADLE_HOME=\"$INSTALL_DIR\""

add_to_profile() {
  profile="$1"
  if [ ! -f "$profile" ]; then
    return
  fi
  if grep -Fq "LADLE_HOME=\"$INSTALL_DIR\"" "$profile" 2>/dev/null; then
    return
  fi
  {
    echo ""
    echo "# Ladle"
    echo "$HOME_LINE"
    echo "$PATH_LINE"
  } >> "$profile"
  echo "Updated $profile"
}

if [ "$USER_INSTALL" -eq 1 ]; then
  add_to_profile "$HOME/.bashrc"
  add_to_profile "$HOME/.zshrc"
  add_to_profile "$HOME/.profile"
fi

cat <<EOF

Ladle installed to $INSTALL_DIR

Open a new terminal, or run:

  export LADLE_HOME="$INSTALL_DIR"
  export PATH="\$LADLE_HOME/bin:\$PATH"

Then use ladle from any directory:

  ladle build build.ini
  ladle dependency build.ini
EOF
