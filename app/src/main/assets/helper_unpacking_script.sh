#!sbin/sh

# parameters

# temp_unpack_dir
# version

OUTFD="/dev/null"

for FD in `ls /proc/$$/fd`; do
	if readlink /proc/$$/fd/$FD | grep -q pipe; then
		if ps | grep -v grep | grep -q " 3 $FD "; then
			OUTFD=$FD
			break
		fi
	fi
done

helper_apk_dir=/system/app/MigrateHelper
ext_helper_apk_dir=/sdcard/Android/data/balti.migratehelper/helper

if [ -e ${helper_apk_dir}/MigrateHelper.apk ]; then

    echo "ui_print Current helper version: $2" >> /proc/self/fd/$OUTFD;

    if [ -e ${helper_apk_dir}/v ]; then
        last_helper_version="$(cat ${helper_apk_dir}/v)"
        echo "ui_print Last helper version: $last_helper_version" >> /proc/self/fd/$OUTFD;
    else
        last_helper_version="0"
    fi

    if [ ${last_helper_version} -lt $2 ]; then
        echo "ui_print Upgrading helper." >> /proc/self/fd/$OUTFD;
        rm -r ${helper_apk_dir}
        rm -r /data/data/balti.migratehelper/
        mv $1 ${helper_apk_dir}
        echo "$2" > ${helper_apk_dir}/v
        mkdir -p ${ext_helper_apk_dir}
        cp ${helper_apk_dir}/MigrateHelper.apk ${ext_helper_apk_dir}/MigrateHelper.apk
    else
        echo "ui_print Helper already present. Skipping helper injection." >> /proc/self/fd/$OUTFD;
        rm -r $1
    fi
else
    echo "ui_print Injecting helper." >> /proc/self/fd/$OUTFD;
    rm -r ${helper_apk_dir}
    rm -r /data/data/balti.migratehelper/
    mv $1 ${helper_apk_dir}
    touch ${helper_apk_dir}/v
    echo "$2" > ${helper_apk_dir}/v
    mkdir -p ${ext_helper_apk_dir}
    cp ${helper_apk_dir}/MigrateHelper.apk ${ext_helper_apk_dir}/MigrateHelper.apk
fi

if [ -e $1 ] && [ -f $1 ]; then
    mv $1 $1.bak
fi
