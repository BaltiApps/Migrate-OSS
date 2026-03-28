#!/sbin/sh

echo

APP_NAME="$1"
PACKAGE_NAME="$2"

APK_LOCATION="$3"

INSTALLER_NAME="$4"

USER="$5" # 0 for default user

NULL_MARKER="$6"
END_MARKER="$7"

print_end_marker() {
  echo "$END_MARKER"
  echo "$END_MARKER" >&2
}

api="$(getprop ro.build.version.sdk)"

echo "=== $APP_NAME (APK) ==="

if [ -z "$USER" ]; then
  USER=0
fi

if [ -d "$APK_LOCATION" ]; then

  mkdir -p /data/local/tmp

  cp -a "$APK_LOCATION" /data/local/tmp

  APK_DIR="${APK_LOCATION##*/}"
  APK_LOCATION="/data/local/tmp/${APK_DIR}"

  cd "$APK_LOCATION" || { echo "Failed to cd into $APK_LOCATION" >&2; print_end_marker; exit 1; }

  # stop package verification if required
  verification_state="$(settings get global package_verifier_enable)"
  case $verification_state in
    ""|null|0) ;;
    *) 
      echo "Disabling package verifier (current state: $verification_state)"
      settings put global package_verifier_enable 0
      ;;
  esac

  base_apk="base.apk"

  if [ ! -f "$base_apk" ]; then
    echo "Error: Base APK ($base_apk) does not exist for $PACKAGE_NAME" >&2
    print_end_marker
    exit 1
  fi

  echo "Starting install session for $PACKAGE_NAME..."

  # Build pm install-create command
  # -r: replace existing
  # -d: allow downgrade
  # -t: allow test packages
  create_cmd="pm install-create -r -d -t --user $USER"

  if [ -n "$INSTALLER_NAME" ] && [ "$INSTALLER_NAME" != "$NULL_MARKER" ] && pm list packages "$INSTALLER_NAME" | grep -q .; then
    echo "Using installer: $INSTALLER_NAME"
    create_cmd="$create_cmd -i $INSTALLER_NAME"
  else
    echo "Using default installer"
  fi

  # Start session
  session=$( $create_cmd | cut -d'[' -f2 | cut -d']' -f1 )

  if [ -z "$session" ]; then
    echo "Error: Failed to create install session" >&2
    print_end_marker
    exit 1
  fi

  echo "Session ID: $session"

  # add base apk
  size=$(wc -c < "$base_apk")
  echo "Adding $base_apk (Size: $size bytes)"
  pm install-write -S "$size" "$session" "$base_apk" "$base_apk" || { echo "Error: Failed to add $base_apk to session" >&2; print_end_marker; exit 1; }

  # add split apks
  for split in split_*.apk; do
    if [ -f "$split" ]; then
      size=$(wc -c < "$split")
      echo "Adding $split (Size: $size bytes)"
      pm install-write -S "$size" "$session" "$split" "$split" || { echo "Warning: Failed to add $split to session" >&2; }
    fi
  done

  echo
  echo "Committing install session (please be patient, it may take some minutes)..."
  echo

  pm install-commit "$session"

  if [ $? -eq 0 ]; then
    echo "Install successful for $PACKAGE_NAME"
  else
    echo "Error: Install failed for $PACKAGE_NAME" >&2
  fi

  echo "Restore process finished for $PACKAGE_NAME"

  rm -rf "${APK_LOCATION}"

  case $verification_state in
    ""|null|0) ;;
    *) 
      echo "Restoring package verifier state to: $verification_state"
      settings put global package_verifier_enable "$verification_state"
      ;;
  esac
else
  echo "Error: APK location $APK_LOCATION does not exist" >&2
fi

print_end_marker
