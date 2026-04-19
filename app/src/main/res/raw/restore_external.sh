#!/sbin/sh

echo " "

APP_NAME="$1"
PACKAGE_NAME="$2"

EXT_DATA_TAR="$3"
EXT_MEDIA_TAR="$4"

NULL_MARKER="$5"
END_MARKER="$6"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

echo "=== $APP_NAME (External) ==="

if [ "$EXT_DATA_TAR" != "$NULL_MARKER" ]; then
  if [ -f "$EXT_DATA_TAR" ]; then
    destDir="/sdcard/Android/data/$PACKAGE_NAME"
    mkdir -p "$destDir" 2>/dev/null
    cd "$destDir" || { echo "Failed to cd into $destDir" >&2; print_end_marker; exit 1; }

    echo
    echo "Restore external data"
    echo

    tar -xzpf "$EXT_DATA_TAR"
  else
    echo "External data tar not found: $EXT_DATA_TAR" >&2
  fi
fi

if [ "$EXT_MEDIA_TAR" != "$NULL_MARKER" ]; then
  if [ -f "$EXT_MEDIA_TAR" ]; then
    destDir="/sdcard/Android/media/$PACKAGE_NAME"
    mkdir -p "$destDir" 2>/dev/null
    cd "$destDir" || { echo "Failed to cd into $destDir" >&2; print_end_marker; exit 1; }

    echo
    echo "Restore external media"
    echo

    tar -xzpf "$EXT_MEDIA_TAR"
  else
    echo "External media tar not found: $EXT_MEDIA_TAR" >&2
  fi
fi

print_end_marker
