#!/sbin/sh

echo " "

DESTINATION="$1"

APP_NAME="$2"
PACKAGE_NAME="$3"

EXTERNAL_DATA_PATH="$4"
EXTERNAL_MEDIA_PATH="$5"

NULL_MARKER="$6"
END_MARKER="$7"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

echo "=== $APP_NAME (External) ==="

if [ ! -e "$DESTINATION" ]; then
  echo "Destination for package $PACKAGE_NAME: $DESTINATION does not exist. Making..."
  mkdir -p "$DESTINATION" 2>/dev/null
fi

if [ "$EXTERNAL_DATA_PATH" != "$NULL_MARKER" ]; then
  if [ -d "$EXTERNAL_DATA_PATH" ]; then
    outFile="$DESTINATION/${PACKAGE_NAME}.ext.data.tar.gz"
    cd "$EXTERNAL_DATA_PATH" || { echo "Failed to cd into $EXTERNAL_DATA_PATH" >&2; print_end_marker; exit 1; }

    echo
    echo "Backup external data"
    echo

    tar -vczpf "$outFile" .
  fi
fi

if [ "$EXTERNAL_MEDIA_PATH" != "$NULL_MARKER" ]; then
  if [ -d "$EXTERNAL_MEDIA_PATH" ]; then
    outFile="$DESTINATION/${PACKAGE_NAME}.ext.media.tar.gz"
    cd "$EXTERNAL_MEDIA_PATH" || { echo "Failed to cd into $EXTERNAL_MEDIA_PATH" >&2; print_end_marker; exit 1; }

    echo
    echo "Backup external media"
    echo

    tar -vczpf "$outFile" .
  fi
fi

print_end_marker
