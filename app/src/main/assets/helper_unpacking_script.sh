#!sbin/sh

# parameters

TEMP_UNPACK_DIR=$1
VERSION=$2
MANUAL_CONFIG_DIR=$3

OUTFD="$(cat /tmp/${MANUAL_CONFIG_DIR}/OUTFD)"
SYSTEM="$(cat /tmp/${MANUAL_CONFIG_DIR}/SYSTEM)"

echoIt() {
    if [[ -n "${OUTFD}" && "${OUTFD}" != "NULL" ]]; then
        echo "ui_print $1" >> /proc/self/fd/${OUTFD};
    else
        echo "FD $OUTFD:: $1"
    fi
}

HELPER_EXTRACT_DIR=""
if [[ -d ${SYSTEM}/product/app ]]; then
    HELPER_EXTRACT_DIR=${SYSTEM}/product
else
    HELPER_EXTRACT_DIR=${SYSTEM}
fi
helper_apk_dir=${HELPER_EXTRACT_DIR}/app/MigrateHelper
ext_helper_apk_dir=/sdcard/Migrate

echoIt "Helper extract dir: $HELPER_EXTRACT_DIR/app"
sleep 1s

if [[ -e ${helper_apk_dir}/MigrateHelper.apk ]]; then

    echoIt "Current helper version: $VERSION"

    if [[ -e ${helper_apk_dir}/v ]]; then
        last_helper_version="$(cat ${helper_apk_dir}/v)"
        echoIt "Last helper version: $last_helper_version"
    else
        last_helper_version="0"
    fi

    if [[ ${last_helper_version} -lt ${VERSION} ]]; then
        echoIt "Upgrading helper."
        rm -rf ${helper_apk_dir}
        rm -rf /data/data/balti.migrate.helper/
        cp -a ${TEMP_UNPACK_DIR}/app ${HELPER_EXTRACT_DIR}/
        echo "$VERSION" > ${helper_apk_dir}/v
        mkdir -p ${ext_helper_apk_dir}
        cp -a ${helper_apk_dir}/MigrateHelper.apk ${ext_helper_apk_dir}/helper.apk
    else
        echoIt "Helper already present. Skipping helper injection."
        rm -r ${TEMP_UNPACK_DIR}
    fi

else
    echoIt "Injecting helper."
    rm -r ${helper_apk_dir}
    rm -r /data/data/balti.migrate.helper/
    cp -a ${TEMP_UNPACK_DIR}/app ${HELPER_EXTRACT_DIR}/
    touch ${helper_apk_dir}/v
    echo "$VERSION" > ${helper_apk_dir}/v
    mkdir -p ${ext_helper_apk_dir}
    cp ${helper_apk_dir}/MigrateHelper.apk ${ext_helper_apk_dir}/helper.apk
fi

#chmod 777 ${helper_apk_dir}
chmod 777 ${ext_helper_apk_dir}

if [[ -e ${TEMP_UNPACK_DIR} ]] && [[ -f ${TEMP_UNPACK_DIR} ]]; then
    mv ${TEMP_UNPACK_DIR} ${TEMP_UNPACK_DIR}.bak
fi

echo "${HELPER_EXTRACT_DIR}" > /tmp/${MANUAL_CONFIG_DIR}/HELPER_EXTRACT_DIR

rm /tmp/helper_unpacking_script.sh
