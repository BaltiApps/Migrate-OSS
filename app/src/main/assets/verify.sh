#!sbin/sh

# parameters

# TEMP_DIR_NAME

OUTFD="/dev/null"

for FD in `ls /proc/$$/fd`; do
	if readlink /proc/$$/fd/$FD | grep -q pipe; then
		if ps | grep -v grep | grep -q " 3 $FD "; then
			OUTFD=$FD
			break
		fi
	fi
done

echo "ui_print  " >> /proc/self/fd/$OUTFD;
echo "ui_print Verifying extras..." >> /proc/self/fd/$OUTFD;

# Removed in v2.0. This is now done by prep.sh
# mv /tmp/package-data /data/balti.migrate/package-data

res="$(cat /proc/cmdline | grep slot_suffix)";

if [ ! -e /system/app/MigrateHelper/MigrateHelper.apk ]; then
    echo "ui_print  " >> /proc/self/fd/$OUTFD;
    echo "ui_print ------------!!!!!!!!!!------------" >> /proc/self/fd/$OUTFD;
    echo "ui_print Helper not installed successfully!" >> /proc/self/fd/$OUTFD;
    echo "ui_print  Please report to the developer!! " >> /proc/self/fd/$OUTFD;
    echo "ui_print  " >> /proc/self/fd/$OUTFD;
    echo "ui_print Deleting migrate cache..." >> /proc/self/fd/$OUTFD;
    echo "ui_print ------------!!!!!!!!!!------------" >> /proc/self/fd/$OUTFD;
    echo "ui_print  " >> /proc/self/fd/$OUTFD;
    #rm -rf /system/app/MigrateHelper
    #rm -rf $1
    unsuccessful_unpack=false
    sleep 2s
else
    unsuccessful_unpack=false
fi

if [ -e /tmp/extras-data ] && [ "$unsuccessful_unpack" = false ]
then
	ed=/tmp/extras-data
	while read -r line || [[ -n "$line" ]]; do
        if [ ! -e $1/${line} ]; then
            echo "ui_print $line was not unpacked" >> /proc/self/fd/$OUTFD;
        fi
    done < "$ed"
fi


ext_helper_apk=/sdcard/Android/data/balti.migratehelper/helper/MigrateHelper.apk
if [ -e ${ext_helper_apk} ]; then
    echo "ui_print  " >> /proc/self/fd/$OUTFD;
    echo "ui_print **********************************" >> /proc/self/fd/$OUTFD;
    echo "ui_print Helper apk is also present under:" >> /proc/self/fd/$OUTFD;
    echo "ui_print ${ext_helper_apk}" >> /proc/self/fd/$OUTFD;
    echo "ui_print  Please install the app manually" >> /proc/self/fd/$OUTFD;
    echo "ui_print   if TWRP could not install it" >> /proc/self/fd/$OUTFD;
    echo "ui_print **********************************" >> /proc/self/fd/$OUTFD;
    sleep 2s
fi

if [ -n "$res" ]; then
    umount /system
fi

if [ "$unsuccessful_unpack" = true ]; then
    umount /data
fi
