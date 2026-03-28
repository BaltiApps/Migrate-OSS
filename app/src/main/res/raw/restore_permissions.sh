#!/sbin/sh

echo

APP_NAME="$1"
PACKAGE_NAME="$2"

USER="$3" # 0 for default user

GRANTED_PERMISSIONS="$4" # space separated

END_MARKER="$5"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

echo "=== $APP_NAME (PERMISSIONS) ==="

if [ -z "$USER" ]; then
  USER=0
fi

echo "Checking if $PACKAGE_NAME is installed..."
if ! pm list packages "${PACKAGE_NAME}" > /dev/null 2>&1; then
  echo "Error: App not installed! - $APP_NAME : $PACKAGE_NAME" >&2
  print_end_marker
  exit 1
fi

if [ -n "$GRANTED_PERMISSIONS" ]; then
  echo "Permissions to grant: $GRANTED_PERMISSIONS"
  
  echo "Revoking all existing permissions for $PACKAGE_NAME (User: $USER)..."
  pm revoke --all-permissions --user "$USER" "$PACKAGE_NAME" 2>/dev/null

  echo "Granting permissions one by one..."

  for perm in $GRANTED_PERMISSIONS; do
    echo "Granting permission: $perm"
    pm grant --user "$USER" "$PACKAGE_NAME" "$perm" 2>&1 | sed 's/^/  /'
  done
  
  echo "Permission restore finished for $PACKAGE_NAME"
else
  echo "No permissions to restore for $PACKAGE_NAME found."
fi

print_end_marker
