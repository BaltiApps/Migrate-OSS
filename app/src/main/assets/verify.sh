#!sbin/sh

# parameters

FILE_LIST=$1
MIGRATE_CACHE=$2
TIMESTAMP=$3

OUTFD="$(cat /tmp/migrate/OUTFD)"
HELPER_EXTRACT_DIR="$(cat /tmp/migrate/HELPER_EXTRACT_DIR)"

echoIt() {
    if [[ ${OUTFD} != "/dev/null" || ! -z ${OUTFD} ]]; then
        echo "ui_print $1" >> /proc/self/fd/${OUTFD};
    else
        echo "FD $OUTFD:: $1"
    fi
}

echoIt " "
echoIt "Verifying backup..."

ext_helper_apk=/sdcard/Migrate/helper.apk

if [[ ! -e ${HELPER_EXTRACT_DIR}/app/MigrateHelper/MigrateHelper.apk ]]; then
    echoIt " "
    echoIt "------------!!!!!!!!!!------------"
    echoIt "Helper not installed successfully!"

    if [[ ! -e ${ext_helper_apk} ]]; then
        echoIt "Helper apk is also present under:"
        echoIt "${ext_helper_apk}"
        echoIt "Please install the app manually"
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
            *app_sys)
                # ignore system apps
                echo "Debug:: verify.sh - ignoring $line"
            ;;
            *tar.gz)
            if [[ ! -e /data/data/${line} ]]; then
                echoIt "$line was not unpacked"
                anyError="true"
            fi
            ;;
            *)
            if [[ ! -e ${MIGRATE_CACHE}/${line} ]]; then
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

chmod -R 777 ${MIGRATE_CACHE}
cp /tmp/${FILE_LIST} ${MIGRATE_CACHE}/"$FILE_LIST"${TIMESTAMP}.txt && echoIt "Copied file list"

echoIt "Verification complete"

rm /tmp/verify.sh
