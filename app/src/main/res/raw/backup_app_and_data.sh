#!/sbin/sh

echo " "

DESTINATION="$1"

APP_NAME="$2"
PACKAGE_NAME="$3"

APK_PATH_BASE="$4"
DATA_PATH_BASE="$5"

IGNORE_CACHE="$6"

NULL_MARKER="$7"
END_MARKER="$8"

print_end_marker() {
    echo "$END_MARKER"
    echo "$END_MARKER" >&2
}

echo "=== $APP_NAME ==="

if [ ! -e "$DESTINATION" ]; then
    echo "Destination for package $PACKAGE_NAME: $DESTINATION does not exist. Making..."
    mkdir -p "$DESTINATION" 2>/dev/null
fi

# backup APK
if [ "$APK_PATH_BASE" != "$NULL_MARKER" ]; then
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

# backup data
if [ "$DATA_PATH_BASE" != "$NULL_MARKER" ]; then
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