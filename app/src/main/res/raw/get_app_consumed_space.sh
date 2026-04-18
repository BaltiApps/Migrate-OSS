#!/sbin/sh

PACKAGE_NAME="$1"

APK_PATH_BASE="$2"
DATA_PATH_BASE="$3"

EXTERNAL_DATA_PATH="$4"
EXTERNAL_MEDIA_PATH="$5"
NULL_MARKER="$6"
END_MARKER="$7"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

apk_bytes=0
data_bytes=0
external_data_bytes=0
external_media_bytes=0

# Calculate APK size
if [ "$APK_PATH_BASE" != "$NULL_MARKER" ] && [ -d "$APK_PATH_BASE" ]; then
  # du -sb returns size in bytes.
  apk_size=$(du -sb "$APK_PATH_BASE" 2>/dev/null | awk '{print $1}')
  if [ -n "$apk_size" ]; then
    apk_bytes=$apk_size
  fi
fi

# Calculate Data size
if [ "$DATA_PATH_BASE" != "$NULL_MARKER" ] && [ -d "$DATA_PATH_BASE" ]; then
  data_size=$(du -sb "$DATA_PATH_BASE" 2>/dev/null | awk '{print $1}')
  if [ -n "$data_size" ]; then
    data_bytes=$data_size
  fi
fi

# Calculate External Data size
if [ "$EXTERNAL_DATA_PATH" != "$NULL_MARKER" ] && [ -d "$EXTERNAL_DATA_PATH" ]; then
  ext_data_size=$(du -sb "$EXTERNAL_DATA_PATH" 2>/dev/null | awk '{print $1}')
  if [ -n "$ext_data_size" ]; then
    external_data_bytes=$ext_data_size
  fi
fi

# Calculate External Media size
if [ "$EXTERNAL_MEDIA_PATH" != "$NULL_MARKER" ] && [ -d "$EXTERNAL_MEDIA_PATH" ]; then
  ext_media_size=$(du -sb "$EXTERNAL_MEDIA_PATH" 2>/dev/null | awk '{print $1}')
  if [ -n "$ext_media_size" ]; then
    external_media_bytes=$ext_media_size
  fi
fi

echo "$PACKAGE_NAME:APK:$apk_bytes:DATA:$data_bytes:EXTERNAL_DATA:$external_data_bytes:EXTERNAL_MEDIA:$external_media_bytes"

print_end_marker
