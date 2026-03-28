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

if [ -n "$GRANTED_PERMISSIONS" ] && [ "$GRANTED_PERMISSIONS" != "$NULL_MARKER" ]; then
  echo "Revoking existing permissions for $PACKAGE_NAME"
  pm revoke --all-permissions --user "$USER" "$PACKAGE_NAME"

  echo

  for perm in $GRANTED_PERMISSIONS; do
    echo "Granting permission - $perm"
    pm grant --user "$USER" "$PACKAGE_NAME" "$perm"
  done
  echo
  echo
fi

print_end_marker
