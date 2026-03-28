#!/sbin/sh

echo " "

DESTINATION="$1"

APP_NAME="$2"
PACKAGE_NAME="$3"

DATA_PATH_BASE="$4"

IGNORE_CACHE="$5"

END_MARKER="$6"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

echo "=== $APP_NAME (Data) ==="

if [ ! -e "$DESTINATION" ]; then
  echo "Destination for package $PACKAGE_NAME: $DESTINATION does not exist. Making..."
  mkdir -p "$DESTINATION" 2>/dev/null
fi

# backup data
if [ -d "$DATA_PATH_BASE" ]; then
  dataBackupLocation="$DESTINATION/$PACKAGE_NAME.tar.gz"
  cd "$DATA_PATH_BASE" || { echo "Failed to cd into $DATA_PATH_BASE" >&2; print_end_marker; exit 1; }

  echo
  echo "Backup private data"
  echo

  if [ "$IGNORE_CACHE" = "true" ]; then
    tar -vczpf "$dataBackupLocation" "$DATA_PATH_BASE" --exclude="$DATA_PATH_BASE/cache"
  else
    tar -vczpf "$dataBackupLocation" "$DATA_PATH_BASE"
  fi
fi

print_end_marker
