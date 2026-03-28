#!/sbin/sh

echo " "

DESTINATION="$1"

APP_NAME="$2"
PACKAGE_NAME="$3"

APK_PATH_BASE="$4"

END_MARKER="$6"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

echo "=== $APP_NAME (APK) ==="

if [ ! -e "$DESTINATION" ]; then
  echo "Destination for package $PACKAGE_NAME: $DESTINATION does not exist. Making..."
  mkdir -p "$DESTINATION" 2>/dev/null
fi

# backup APK
if [ -d "$APK_PATH_BASE" ]; then
  appBackupDir="$DESTINATION/$PACKAGE_NAME.app"
  mkdir -p "$appBackupDir"

  cd "$APK_PATH_BASE" || { echo "Failed to cd into $APK_PATH_BASE" >&2; print_end_marker; exit 1; }

  echo
  echo "Copy APKs"
  echo

  for f in *.apk; do
    cp -v "$f" "${appBackupDir}/$f"
  done
fi

print_end_marker
