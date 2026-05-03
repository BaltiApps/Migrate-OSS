#!/sbin/sh

echo " "

APP_NAME="$1"
PACKAGE_NAME="$2"

EXT_DATA_TAR="$3"
EXT_MEDIA_TAR="$4"

USER="$5"

NULL_MARKER="$6"
END_MARKER="$7"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

echo "=== $APP_NAME (External) ==="

if [ -z "$USER" ]; then
  USER=0
fi

if [ "$EXT_DATA_TAR" != "$NULL_MARKER" ]; then
  if [ -f "$EXT_DATA_TAR" ]; then
    destDir="/sdcard/Android/data/$PACKAGE_NAME"
    mkdir -p "$destDir" 2>/dev/null
    cd "$destDir" || { echo "Failed to cd into $destDir" >&2; print_end_marker; exit 1; }

    echo
    echo "Restore external data"
    echo

    tar -xvzpf "$EXT_DATA_TAR"

    echo "Fetching UID for $PACKAGE_NAME..."
    app_uid=$(cmd package list packages -U --user "$USER" "$PACKAGE_NAME" | awk -F'uid:' '{print $2}')
    if [ -n "$app_uid" ]; then
      echo "Setting ownership of $destDir to uid $app_uid..."
      chown "${app_uid}":"ext_data_rw" -Rf "$destDir"
      chcon -Rh u:object_r:fuse:s0 "$destDir"
    else
      echo "ERROR: Could not determine UID for $PACKAGE_NAME, skipping ownership fix" >&2
    fi
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

    tar -xvzpf "$EXT_MEDIA_TAR"
    chcon -Rh u:object_r:fuse:s0 "$destDir"

    echo "Triggering media scan..."
    am broadcast -a android.intent.action.MEDIA_MOUNTED -d file:///sdcard > /dev/null 2>&1
    echo "Media scan triggered."
  else
    echo "External media tar not found: $EXT_MEDIA_TAR" >&2
  fi
fi

print_end_marker
