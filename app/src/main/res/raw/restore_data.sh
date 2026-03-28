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

if ! pm list packages "${PACKAGE_NAME}"; then
  echo "App not installed! - $APP_NAME : $PACKAGE_NAME" >&2
  print_end_marker
  exit 1
fi

dataDir="/data/user/$USER/$PACKAGE_NAME"
app_uid=$(cmd package list packages -U --user "$USER" "$PACKAGE_NAME" | awk -F'uid:' '{print $2}' | tr -d '[:space:]')

if [ -z "$app_uid" ]; then
  echo "App uid not found: $PACKAGE_NAME" >&2
  print_end_marker
  exit 1
fi

echo "Force stopping app"

am force-stop "$PACKAGE_NAME" 2>/dev/null

echo "Wiping stale data"

[ -d "$dataDir" ] && rm -rf "${dataDir}"

echo "Extracting data"

parentDir=$(dirname "$dataDir")

tar -xzpf "$DATA_TAR_LOCATION" -C "$parentDir" || { echo "Failed extract data for $PACKAGE_NAME" >&2; print_end_marker; exit 1; }

printf "\nFixing contexts and permissions\n"

chmod 771 "${dataDir}"
chown "${app_uid}":"${app_uid}" -Rf "${dataDir}"
restorecon -RF "${dataDir}" 2>/dev/null

API=$(getprop ro.build.version.sdk)

if [ "$API" -ge 29 ]; then
  echo "A10+ fixing context"
  chcon -Rh u:object_r:app_data_file:s0 "${dataDir}"
fi

# notification fix added in v3.0
if [ "${NOTIFICATION_FIX}" = "true" ]; then
  echo "Removing gms file under $PACKAGE_NAME/shared_prefs"
  cd "${dataDir}" && rm -f shared_prefs/com.google.android.gms.appid.xml || echo "Notification fix - failed to cd into $dataDir" >&2
fi

print_end_marker
