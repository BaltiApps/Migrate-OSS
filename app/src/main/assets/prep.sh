#!sbin/sh

# parameters:

TEMP_DIR_NAME=$1
TIMESTAMP=$2

OUTFD="/dev/null"
AB_DEVICE=""
SYSTEM=/system

for FD in `ls /proc/$$/fd`; do
	if readlink /proc/$$/fd/$FD | grep -q pipe; then
		if ps | grep -v grep | grep -q " 3 $FD "; then
			OUTFD=${FD}
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

exitNow() {

    # unmount data to signal that error has occurred
    umount /data

	if [[ -n "$AB_DEVICE" ]]; then
	    umount ${SYSTEM}
	fi

	sleep 2s
	exit 1
}

echoIt " "
echoIt "Checking parameters..."
echoIt " "

# Detect SAR
SAR_PROP="$(getprop ro.build.system_root_image)"
if [[ "$SAR_PROP" == "true" ]] || [[ -d /system_root && ! -f /system/build.prop ]]
then
    echoIt "System-as-root detected!"
	echoIt "Experimental support !!!"
	SYSTEM=/system_root
else
    echoIt "Non System-as-root."
fi

echoIt " "

# Detect A/B device
AB_DEVICE="$(cat /proc/cmdline | grep slot_suffix)";
if [[ -n "$AB_DEVICE" ]];
then
	echoIt "A/B device!"
	echoIt "Experimental support !!!"
	echoIt "Bind mounting system..."
	mount -o bind ${SYSTEM}/system ${SYSTEM}
else
	echoIt "Only-A device."
fi

echoIt " "

sleep 2s

# Check if ROM is present
if [[ -e "$SYSTEM/build.prop" ]]; then
	echoIt "ROM is present."
else

	echo "DEBUG:: --- Contents in $SYSTEM ---"
	echo "$(ls ${SYSTEM})"
	echo "DEBUG:: --- End of contents ---"

    echoIt " "
	echoIt "------------!!!!!!!!!!------------"
	echoIt "No ROM is detected"
	echoIt "Please flash a ROM first"
	echoIt "------------!!!!!!!!!!------------"
	echoIt " "

	exitNow

fi

# Check other parameters from backup
if [[ -e /tmp/package-data.txt ]]; then

	pd=/tmp/package-data.txt

	while read -r line || [[ -n "$line" ]]; do

		key=$(echo "$line" | cut -d ' ' -f1)
		val=$(echo "$line" | cut -d ' ' -f2)

		# check CPU arch
		if [[ "$key" == "cpu_abi" ]]; then

			cpu_arch="$(getprop ro.product.cpu.abi)"

			if [[ "$cpu_arch" = "$val" ]]; then
				echoIt "CPU ABI is OK ($cpu_arch)"
			else
				echoIt " "
				echoIt "----------------------------------"
				echoIt "Original CPU ABI was $val"
				echoIt "Found: $cpu_arch"
				echoIt "Restoration of some apps MAY FAIL!"
				echoIt "----------------------------------"
				echoIt " "
			fi

        # Check free space in /data
		elif [[ "$key" == "data_required_size" ]]; then

			data_free=$(df -k /data | tail -1 | awk '{print $4}')

            # sometimes, the above data command gets the percentage of data used
            # In that case, the data_free variable will contain % at the end
			case ${data_free} in
	            *%)
				echoIt "Using third argument..."
				data_free=$(df -k /data | tail -1 | awk '{print $3}')
				;;
			esac

			if [[ ${data_free} -ge ${val} ]]; then
				echoIt "Free space (/data) is OK ($data_free KB)"
			else

                echo "DEBUG:: --- df output /data ---"
	            echo "$(df -k /data)"
	            echo "DEBUG:: --- End of output ---"
	            echo "DEBUG:: data_free = $data_free"

				echoIt " "
				echoIt "------------!!!!!!!!!!------------"
				echoIt "Free space (/data): $data_free KB"
				echoIt "ui_print Required space is $val KB"
				echoIt "------------!!!!!!!!!!------------"
				echoIt "Restore cannot progress. You can try:"
				echoIt "* Wiping data"
				echoIt "* If you are flashing a backup from a different device, it is not recommended."
				echoIt "* Check if a Migrate package is previously flashed"
				echoIt "-- From TWRP main menu->'Mount'->'Data'"
				echoIt "-- Main menu->'Advanced'->'File Manager'"
				echoIt "-- Navigate to '/data/local/tmp/migrate_cache'"
				echoIt "-- Delete the directory"
				echoIt " "
				echoIt "Additionally delete all tar.gz files from under /data/data/"
				echoIt " "

				exitNow
			fi

		# check free space in /system
		elif [[ "$key" == "system_required_size" ]]; then

			system_free=$(df -k /system | tail -1 | awk '{print $4}')

			case ${system_free} in
	            *%)
				echoIt "Using third argument..."
				system_free=$(df -k /system | tail -1 | awk '{print $3}')
				;;
			esac

			if [[ ${system_free} -ge ${val} ]]; then
				echoIt "Free space (/system) is OK ($system_free KB)"
			else

                echo "DEBUG:: --- df output /system---"
	            echo "$(df -k /system)"
	            echo "DEBUG:: --- End of output ---"
	            echo "DEBUG:: system_free = $system_free"

				echoIt " "
				echoIt "----------------------------------"
				echoIt "Free space (/system): $system_free KB"
				echoIt "Required space is $val KB"
				echoIt "----------------------------------"
				echoIt "SOME SYSTEM APPS WILL NOT BE RESTORED. You can try:"
				echoIt "* A new ROM"
				echoIt "* A smaller GApps package"
				echoIt "* If you are flashing a backup from a different device, it is not recommended."
				echoIt " "
			fi

		# check ROM android version
		elif [[ "$key" == "sdk" ]]; then

			rom_sdk="$(cat ${SYSTEM}/build.prop | grep ro.build.version.sdk | cut -d "=" -f2)"

			if [[ ${rom_sdk} -ge 21 ]]; then

			    if [[ ${rom_sdk} == ${val} ]]; then
				    echoIt "Android version is OK (sdk $rom_sdk)"
			    else
				    echoIt " "
				    echoIt "----------------------------------"
				    echoIt "Original Android version was: $val"
				    echoIt "Current ROM Android version: $rom_sdk"
				    echoIt "Restoration of some apps MAY FAIL!"
				    echoIt "----------------------------------"
				    echoIt " "
			    fi

			else

			    echo "DEBUG:: rom_sdk line from build.prop ---"
			    echo "$(cat ${SYSTEM}/build.prop | grep ro.build.version.sdk)"
			    echo "DEBUG:: rom_sdk = $rom_sdk"

			    echoIt "------------!!!!!!!!!!------------"
			    echoIt "Migrate cannot be flashed below:"
			    echoIt "    Android Lollipop (sdk 21)   "
			    echoIt "Your current ROM version is sdk $rom_sdk"
			    echoIt "------------!!!!!!!!!!------------"

			    exitNow
			fi
		fi
	done < "$pd"
fi

echoIt " "

mkdir -p ${SYSTEM}/app/
mkdir -p ${SYSTEM}/priv-app/
mkdir -p /data/app/
mkdir -p /data/data/

mkdir -p ${TEMP_DIR_NAME}
cp /tmp/package-data.txt ${TEMP_DIR_NAME}/package-data${TIMESTAMP}.txt

rm /tmp/prep.sh