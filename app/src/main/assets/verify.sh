#!sbin/sh

# parameters

FILE_LIST=$1
TEMP_DIR_NAME=$2

OUTFD="$(cat /tmp/migrate/OUTFD)"
SYSTEM="$(cat /tmp/migrate/SYSTEM)"

echoIt() {
    if [[ ${OUTFD} != "/dev/null" || ! -z ${OUTFD} ]]; then
    echoIt "$1" >> /proc/self/fd/${OUTFD};
    else
    echo "FD:: $1"
    fi
}

echoIt " "
echoIt "Verifying backup..."

ext_helper_apk=/sdcard/Android/data/balti.migratehelper/helper/MigrateHelper.apk

if [[ ! -e ${SYSTEM}/app/MigrateHelper/MigrateHelper.apk ]]; then
    echoIt " "
    echoIt "------------!!!!!!!!!!------------"
    echoIt "Helper not installed successfully!"

    if [[ ! -e ${ext_helper_apk} ]]; then
        echoIt "Helper apk is also present under:"
        echoIt "${ext_helper_apk}"
        echoIt " Please install the app manually"
    else
        echoIt "Please contact our telegram group"
        echoIt "     https://t.me/migrateApp     "
    fi

    echoIt "------------!!!!!!!!!!------------"
    echoIt " "
    sleep 1s
fi

echoIt " "
echoIt "Checking files..."

anyError="false"
if [[ -e /tmp/${FILE_LIST} ]]; then
    ed=/tmp/${FILE_LIST}
    while read -r line || [[ -n "$line" ]]; do
        case ${line} in
            *tar.gz)
            if [[ ! -e /data/data/${line} ]]; then
                echoIt "$line was not unpacked"
                anyError="true"
            fi
            ;;
            *)
            if [[ ! -e ${TEMP_DIR_NAME}/${line} ]]; then
                echoIt "$line was not unpacked"
                anyError="true"
            fi
            ;;
        esac
    done < "$ed"
fi

if [[ ${anyError} == "true" ]]; then
    echoIt " "
    echoIt "------------!!!!!!!!!!------------"
    echoIt "  Some files were not extracted   "
    echoIt "     Restore maybe incomplete     "
    echoIt "------------!!!!!!!!!!------------"
    echoIt " "
    sleep 1s
fi

echoIt "Verification complete"
