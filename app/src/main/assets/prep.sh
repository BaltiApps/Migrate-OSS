#!sbin/sh

# parameters:

TEMP_DIR_NAME=$1
TIMESTAMP=$2

# In case of errors, system app path and build.prop path can be manually mentioned under:
# /tmp/migrate/SYSTEM_MANUAL and /tmp/migrate/BUILDPROP_MANUAL files respectively.

OUTFD="/dev/null"
SYSTEM=""
SAR="$(cat /tmp/migrate/SAR)"

for FD in `ls /proc/$$/fd`; do
    if readlink /proc/$$/fd/$FD | grep -q pipe; then
        if ps | grep -v grep | grep -q " 3 $FD "; then
            OUTFD=${FD}
            break
        fi
    fi
done

echoIt() {
    if [[ ${OUTFD} != "/dev/null" || ! -z ${OUTFD} ]]; then
    echoIt "$1" >> /proc/self/fd/${OUTFD};
    else
    echo "FD:: $1"
    fi
}

exitNow() {

    # unmount data to signal that error has occurred
    umount /data

    sleep 2s
    exit 1
}

echoIt " "
echoIt "Checking parameters..."
echoIt " "

if [[ -d /system_root/system/app ]]; then
    SYSTEM=/system_root/system

elif [[ -d /system_root/app ]]; then
    SYSTEM=/system_root

elif [[ -d /system/system/app ]]; then
    SYSTEM=/system/system

elif [[ -d /system/app ]]; then
    SYSTEM=/system

fi

manual_entry_system="$(cat /tmp/migrate/SYSTEM_MANUAL)"

if [[ ${SYSTEM} != "" ]]; then
    echoIt "System app confirmed under: $SYSTEM"
elif [[ -n "$manual_entry_system" ]]; then
    echoIt "Manual system app location: $manual_entry_system"
    SYSTEM=${manual_entry_system}
else
    echoIt "System app directory detection failed. Trying from parameters"

    # Check SAR
    if [[ "$SAR" == "true" ]]
    then
        echoIt "System-as-root detected!"
        SYSTEM=/system_root
    else
        echoIt "Non System-as-root."
    fi

    ab_device="$(cat /proc/cmdline | grep slot_suffix)";
    if [[ -n "$ab_device" ]];
    then
        echoIt "A/B device!"
        SYSTEM=${SYSTEM}/system
    else
        echoIt "Only-A device."
    fi

    echoIt "Calculated system app under: $SYSTEM"

fi

echoIt " "

sleep 1s

# Check if ROM is present
build_prop="$SYSTEM/build.prop"
manual_entry_buildprop="$(cat /tmp/migrate/BUILDPROP_MANUAL)"

if [[ -n "$manual_entry_buildprop" ]]; then
    echoIt "Manual build.prop location: $manual_entry_buildprop"
    build_prop=${manual_entry_buildprop}
fi

if [[ -e "$build_prop" ]]; then
    echoIt "ROM is present."
else

    echo "DEBUG:: --- Contents in $SYSTEM ---"
    echo "$(find / -name build.prop)"
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

            rom_sdk="$(cat ${build_prop} | grep ro.build.version.sdk | cut -d "=" -f2)"

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
                echo "$(cat ${build_prop} | grep ro.build.version.sdk)"
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

# export variables
echo "${OUTFD}" > /tmp/migrate/OUTFD
echo "${SYSTEM}" > /tmp/migrate/SYSTEM

rm /tmp/prep.sh