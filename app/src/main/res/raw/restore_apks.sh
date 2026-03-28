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

  cd "$APK_LOCATION" || { echo "Failed to cd into $APK_LOCATION" >&2; print_end_marker; exit 1; }

  # stop package verification if required
  verification_state="$(settings get global package_verifier_enable)"
  case $verification_state in
    ""|null|0) ;;
    *) settings put global package_verifier_enable 0;;
  esac

  pm_command="pm install"

  if [ "$api" -ge 29 ]; then
    pm_command="${pm_command} --user $USER"
  fi

  base_apk="base.apk"

  if [ ! -f "$base_apk" ]; then
    echo "Base APK does not exist for $PACKAGE_NAME, exiting." >&2
    print_end_marker
    exit 1
  fi

  echo "Installing APK"

  if [ "$INSTALLER_NAME" ] && [ "$INSTALLER_NAME" != "$NULL_MARKER" ] && pm list packages "$INSTALLER_NAME" | grep -q .; then
    $pm_command -r -d -t -i "$INSTALLER_NAME" "$base_apk"
  else
    $pm_command -r -d -t "$base_apk"
  fi

  split_count=$(ls -1 . | grep "^split" | grep -c "\.apk$")

  if [ "${split_count}" -gt 0 ]; then
    ############## Made by Vijay ##############

    echo "Split: Creating Install Session"

    pm_command="pm install-create --user ${USER}"

    if [ -n "${INSTALLER_NAME}" ] && [ "${INSTALLER_NAME}" != "${NULL_MARKER}" ]; then
        session=$(${pm_command} -i "${INSTALLER_NAME}" -p "${PACKAGE_NAME}" | cut -d'[' -f2 | cut -d']' -f1)
    else
        session=$(${pm_command} -p "${PACKAGE_NAME}" | cut -d'[' -f2 | cut -d']' -f1)
    fi

    # add split apks
    echo "Split: Adding Split Apks to Session"

    for filename in split_*.apk; do
        size="$(wc -c < "${filename}")"
        pm install-write -S "${size}" "${session}" "${filename}" "${APK_LOCATION}/${filename}" 2>/dev/null && echo "Split: Added ${filename}"
    done

    echo "Split: Installing Split Session (Be patient, it may take longer)"
    pm install-commit "${session}"

    echo "Split: Done"
    ############## Made by Vijay ##############
  else
    echo "No split apks. Split count: $split_count"
  fi

  echo

  case $verification_state in
    ""|null|0) ;;
    *) settings put global package_verifier_enable "$verification_state";;
  esac
fi

print_end_marker