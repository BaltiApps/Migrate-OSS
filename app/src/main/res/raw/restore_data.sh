#!/sbin/sh

echo " "

APP_NAME="$1"
PACKAGE_NAME="$2"

DATA_TAR_LOCATION="$3"

USER="$4" # 0 for default user

NOTIFICATION_FIX="$5" # pass true by default

END_MARKER="$6"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

echo "=== $APP_NAME (DATA) ==="

if [ -z "$USER" ]; then
  USER=0
fi

echo "Checking if $PACKAGE_NAME is installed..."
if ! pm list packages "${PACKAGE_NAME}" > /dev/null 2>&1; then
  echo "Error: App not installed! - $APP_NAME : $PACKAGE_NAME" >&2
  print_end_marker
  exit 1
fi

dataDir="/data/user/$USER/$PACKAGE_NAME"

echo "Fetching UID for $PACKAGE_NAME..."
app_uid=$(cmd package list packages -U --user "$USER" "$PACKAGE_NAME" | awk -F'uid:' '{print $2}')
echo "UID for $PACKAGE_NAME - $app_uid"

if [ -z "$app_uid" ]; then
  echo "Error: App uid not found for $PACKAGE_NAME" >&2
  print_end_marker
  exit 1
fi

echo "UID found: $app_uid"

echo "Force stopping $PACKAGE_NAME..."
am force-stop "$PACKAGE_NAME" 2>/dev/null

echo "Wiping stale data at $dataDir..."
if [ -d "$dataDir" ]; then
  rm -rf "${dataDir}"
  echo "Stale data removed."
else
  echo "No stale data found."
fi

echo "Extracting data from $DATA_TAR_LOCATION..."

if [ ! -f "$DATA_TAR_LOCATION" ]; then
  echo "Error: Backup file $DATA_TAR_LOCATION not found!" >&2
  print_end_marker
  exit 1
fi

mkdir -p "$dataDir"
tar -xvzpf "$DATA_TAR_LOCATION" -C "$dataDir" || { echo "Error: Failed to extract data for $PACKAGE_NAME" >&2; print_end_marker; exit 1; }

echo "Fixing contexts and permissions for $dataDir..."
chmod 771 "${dataDir}"
chown "${app_uid}":"${app_uid}" -Rf "${dataDir}"
restorecon -RF "${dataDir}" 2>/dev/null

API=$(getprop ro.build.version.sdk)
echo "Android API Level: $API"

if [ "$API" -ge 29 ]; then
  echo "Android 10+ detected, applying specific SELinux context..."
  chcon -Rh u:object_r:app_data_file:s0 "${dataDir}"
fi

# notification fix added in v3.0
if [ "${NOTIFICATION_FIX}" = "true" ]; then
  echo "Applying notification fix: checking for Google GMS files..."
  target_file="${dataDir}/shared_prefs/com.google.android.gms.appid.xml"
  if [ -f "$target_file" ]; then
    rm -f "$target_file"
    echo "Removed $target_file"
  else
    echo "GMS file not found, skipping."
  fi
fi

echo "Data restore finished for $PACKAGE_NAME"

print_end_marker
