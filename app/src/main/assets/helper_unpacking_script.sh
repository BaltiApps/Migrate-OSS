#!sbin/sh

# parameters

TEMP_UNPACK_DIR=$1
VERSION=$2

OUTFD="/dev/null"
SYSTEM=/system

for FD in `ls /proc/$$/fd`; do
	if readlink /proc/$$/fd/$FD | grep -q pipe; then
		if ps | grep -v grep | grep -q " 3 $FD "; then
			OUTFD=$FD
			break
		fi
	fi
done

echoIt() {
    if [[ ${OUTFD} != "/dev/null" ]]; then
        echoIt "$1" >> /proc/self/fd/${OUTFD};
    else
        echo "FD:: $1"
    fi
}

helper_apk_dir=${SYSTEM}/app/MigrateHelper
ext_helper_apk_dir=/sdcard/Android/data/balti.migratehelper/helper

# Detect SAR
SAR_PROP="$(getprop ro.build.system_root_image)"
if [[ "$SAR_PROP" == "true" ]] || [[ -d /system_root && ! -f /system/build.prop ]]
then
	SYSTEM=/system_root
fi

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
        rm -r ${helper_apk_dir}
        rm -r /data/data/balti.migratehelper/
        mv ${TEMP_UNPACK_DIR} ${helper_apk_dir}
        echo "$VERSION" > ${helper_apk_dir}/v
        mkdir -p ${ext_helper_apk_dir}
        cp ${helper_apk_dir}/MigrateHelper.apk ${ext_helper_apk_dir}/MigrateHelper.apk
    else
        echoIt "Helper already present. Skipping helper injection."
        rm -r ${TEMP_UNPACK_DIR}
    fi

else
    echoIt "ui_print Injecting helper."
    rm -r ${helper_apk_dir}
    rm -r /data/data/balti.migratehelper/
    mv ${TEMP_UNPACK_DIR} ${helper_apk_dir}
    touch ${helper_apk_dir}/v
    echo "$VERSION" > ${helper_apk_dir}/v
    mkdir -p ${ext_helper_apk_dir}
    cp ${helper_apk_dir}/MigrateHelper.apk ${ext_helper_apk_dir}/MigrateHelper.apk
fi

chmod 777 ${helper_apk_dir}
chmod 777 ${ext_helper_apk_dir}

if [[ -e ${TEMP_UNPACK_DIR} ]] && [[ -f ${TEMP_UNPACK_DIR} ]]; then
    mv ${TEMP_UNPACK_DIR} ${TEMP_UNPACK_DIR}.bak
fi
