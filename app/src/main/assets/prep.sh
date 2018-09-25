#!sbin/sh

export OUTFD="/dev/null"

for FD in `ls /proc/$$/fd`; do
	if readlink /proc/$$/fd/$FD | grep -q pipe; then
		if ps | grep -v grep | grep -q " 3 $FD "; then
			OUTFD=$FD
			break
		fi
	fi
done

echo "ui_print  " >> /proc/self/fd/$OUTFD;
echo "ui_print Checking parameters..." >> /proc/self/fd/$OUTFD;
echo "ui_print  " >> /proc/self/fd/$OUTFD;


if [ -e /system/build.prop ]; then
	echo "ui_print ROM is present." >> /proc/self/fd/$OUTFD;
else
    echo "ui_print  " >> /proc/self/fd/$OUTFD;
	echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
	echo "ui_print No ROM is detected" >> /proc/self/fd/$OUTFD;
	echo "ui_print Please flash a ROM first" >> /proc/self/fd/$OUTFD;
	echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
	echo "ui_print  " >> /proc/self/fd/$OUTFD;
	umount /system
	sleep 2s
	exit 1
fi

if [ -e /tmp/package-data ]
then
	pd=/tmp/package-data
	while read -r line || [[ -n "$line" ]]; do
		key=$(echo "$line" | cut -d ' ' -f1)
		val=$(echo "$line" | cut -d ' ' -f2)
		if [ "$key" = "cpu_abi" ]; then
			cpu_arch="$(getprop ro.product.cpu.abi)"
			if [ "$cpu_arch" = "$val" ]; then
				echo "ui_print CPU architecture OK" >> /proc/self/fd/$OUTFD;
			else
				echo "ui_print  " >> /proc/self/fd/$OUTFD;
				echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
				echo "ui_print Required CPU architecture is $val" >> /proc/self/fd/$OUTFD;
				echo "ui_print Found: $cpu_arch" >> /proc/self/fd/$OUTFD;
				echo "ui_print Restoration of some apps MAY FAIL!" >> /proc/self/fd/$OUTFD;
				echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
				echo "ui_print  " >> /proc/self/fd/$OUTFD;
			fi
		elif [ "$key" = "data_required_size" ]; then
			data_free=$(df -k /data | tail -1 | awk '{print $3}')
			if [ $data_free -ge $val ]; then
				echo "ui_print Free space (/data) is OK" >> /proc/self/fd/$OUTFD;
			else
				echo "ui_print  " >> /proc/self/fd/$OUTFD;
				echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
				echo "ui_print Free space (/data): $data_free" >> /proc/self/fd/$OUTFD;
				echo "ui_print Required space is $val" >> /proc/self/fd/$OUTFD;
				echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
				echo "ui_print Restore cannot progress. You can try:" >> /proc/self/fd/$OUTFD;
				echo "ui_print * Wiping data" >> /proc/self/fd/$OUTFD;
				echo "ui_print * If you are flashing a backup from a different device, it is not recommended." >> /proc/self/fd/$OUTFD;
				echo "ui_print * Check if a Migrate package is previously flashed" >> /proc/self/fd/$OUTFD;
				echo "ui_print -- From TWRP main menu->'Mount'->'Data'" >> /proc/self/fd/$OUTFD;
				echo "ui_print -- Main menu->'Advanced'->'File Manager'" >> /proc/self/fd/$OUTFD;
				echo "ui_print -- Navigate to '/data/balti.migrate'" >> /proc/self/fd/$OUTFD;
				echo "ui_print -- Delete the directory" >> /proc/self/fd/$OUTFD;
				echo "ui_print  " >> /proc/self/fd/$OUTFD;
				umount /data
				sleep 2s
				exit 1
			fi
		elif [ "$key" = "system_required_size" ]; then
			system_free=$(df -k /system | tail -1 | awk '{print $3}')
			if [ $system_free -ge $val ]; then
				echo "ui_print Free space (/system) is OK" >> /proc/self/fd/$OUTFD;
			else
				echo "ui_print  " >> /proc/self/fd/$OUTFD;
				echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
				echo "ui_print Free space (/system): $system_free" >> /proc/self/fd/$OUTFD;
				echo "ui_print Required space is $val" >> /proc/self/fd/$OUTFD;
				echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
				echo "ui_print Restore cannot progress. You can try:" >> /proc/self/fd/$OUTFD;
				echo "ui_print * A new ROM" >> /proc/self/fd/$OUTFD;
				echo "ui_print * A smaller GApps package" >> /proc/self/fd/$OUTFD;
				echo "ui_print * If you are flashing a backup from a different device, it is not recommended." >> /proc/self/fd/$OUTFD;
				echo "ui_print  " >> /proc/self/fd/$OUTFD;
				umount /system
				sleep 2s
				exit 1
			fi
		elif [ "$key" = "sdk" ]; then
			rom_sdk="$(cat /system/build.prop | grep ro.build.version.sdk | cut -d "=" -f2)"
			if [ $rom_sdk = $val ]; then
				echo "ui_print Android version is OK" >> /proc/self/fd/$OUTFD;
			else
				echo "ui_print  " >> /proc/self/fd/$OUTFD;
				echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
				echo "ui_print Original android version was: $val" >> /proc/self/fd/$OUTFD;
				echo "ui_print Current ROM android version: $cpu_arch" >> /proc/self/fd/$OUTFD;
				echo "ui_print Restoration of some apps MAY FAIL!" >> /proc/self/fd/$OUTFD;
				echo "ui_print ---------------------------------" >> /proc/self/fd/$OUTFD;
				echo "ui_print  " >> /proc/self/fd/$OUTFD;
			fi
		fi
	done < "$pd"
fi

echo "ui_print  " >> /proc/self/fd/$OUTFD;

mkdir -p /system/app/
mkdir -p /system/priv-app/
mkdir -p /data/app/
mkdir -p /data/data/
rm -r /system/app/PermissionFixer/
rm -r /data/data/balti.migratehelper/

mkdir -p /data/balti.migrate
mv /tmp/package-data /data/balti.migrate/package-data

rm /tmp/prep.sh
