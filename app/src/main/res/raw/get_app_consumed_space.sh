#!/sbin/sh

PACKAGE_NAME="$1"

APK_PATH_BASE="$2"
DATA_PATH_BASE="$3"

NULL_MARKER="$4"
END_MARKER="$5"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

total_bytes=0

# Calculate APK size
if [ "$APK_PATH_BASE" != "$NULL_MARKER" ] && [ -d "$APK_PATH_BASE" ]; then
  # du -sb returns size in bytes. We use cut to get only the first field (the size).
  apk_size=$(du -sb "$APK_PATH_BASE" 2>/dev/null | awk '{print $1}')
  if [ -n "$apk_size" ]; then
    total_bytes=$((total_bytes + apk_size))
  fi
fi

# Calculate Data size
if [ "$DATA_PATH_BASE" != "$NULL_MARKER" ] && [ -d "$DATA_PATH_BASE" ]; then
  data_size=$(du -sb "$DATA_PATH_BASE" 2>/dev/null | awk '{print $1}')
  if [ -n "$data_size" ]; then
    total_bytes=$((total_bytes + data_size))
  fi
fi

# Output in the requested format
echo "$PACKAGE_NAME:$total_bytes"

print_end_marker
