#!sbin/sh

# parameters:

MIGRATE_CACHE_DEFAULT=$1
TIMESTAMP=$2
PACKAGE_DATA_NAME=$3
#TEMP_UNPACK_DIR=$4
MANUAL_CONFIG_DIR=$4
ZIP_NAME="$5"

OUTFD="NULL"
SYSTEM=""
MIGRATE_CACHE=""

AWK1="awk"
READLINK1="readlink"

# In case of errors, system app path and build.prop path can be manually mentioned under:
# /tmp/${MANUAL_CONFIG_DIR}/SYSTEM_MANUAL and /tmp/${MANUAL_CONFIG_DIR}/BUILDPROP_MANUAL files respectively.
# Also a manual location for migrate cache can be placed in the file /tmp/${MANUAL_CONFIG_DIR}/MIGRATE_CACHE_MANUAL

SAR="$(cat /tmp/${MANUAL_CONFIG_DIR}/SAR)"

if [[ ! -x "$(command -v ${READLINK1})" ]]; then
    READLINK1="/tmp/busybox readlink"
    echo "DEBUG:: Using busybox readlink....."
    sleep 1s
fi

if [[ ! -x "$(command -v ${AWK1})" ]]; then
    AWK1="/tmp/busybox awk"
    echo "DEBUG:: Using busybox awk....."
    sleep 1s
fi

for FD in `ls /proc/$$/fd`; do
    if ${READLINK1} /proc/$$/fd/$FD | grep -q pipe; then
        if ps | grep -v grep | grep -q " 3 $FD "; then
            OUTFD=${FD}
            break
        fi
    fi
done

if [[ "$OUTFD" == "NULL" ]]; then
    echo "DEBUG:: Manually trying to find FD....."
    echo "DEBUG:: --- start of ps ---"
    ps
    echo "DEBUG:: --- end of ps ---"
    ps_line="$(ps | grep -v grep | grep ${ZIP_NAME} | head -n 1)"
    echo "DEBUG:: ps_line: $ps_line"
    out="$(echo ${ps_line} | $AWK1 '{print $(NF-1)}')"
    echo "DEBUG:: FD detected: $out....."
    if [[ "$out" -eq "$out" ]]; then 
        OUTFD=${out}
    else
        echo "DEBUG:: FD not a number"
    fi
fi

echoIt() {
    if [[ -n "${OUTFD}" && "${OUTFD}" != "NULL" ]]; then
        echo "ui_print $1" >> /proc/self/fd/${OUTFD};
    else
        echo "FD $OUTFD:: $1"
    fi
}

exitNow() {

    # unmount data to signal that error has occurred
    umount /data

    # delete temp dir
    #if [[ -n "${TEMP_UNPACK_DIR}" && ${TEMP_UNPACK_DIR} != "/" && ${TEMP_UNPACK_DIR} != "/sdcard" ]]; then
    #    rm -rf ${TEMP_UNPACK_DIR}
    #fi

    sleep 2s
    exit 1
}

echoIt " "

manual_migrate_cache="$(cat /tmp/${MANUAL_CONFIG_DIR}/MIGRATE_CACHE_MANUAL)" 2>/dev/null

if [[ -n "${manual_migrate_cache}" ]]; then
    MIGRATE_CACHE="$manual_migrate_cache"
    echoIt "Manual migrate_cache: $MIGRATE_CACHE"
    sleep 1s
else
    MIGRATE_CACHE=${MIGRATE_CACHE_DEFAULT}
    echoIt "migrate_cache: $MIGRATE_CACHE"
fi

#mkdir -p ${TEMP_UNPACK_DIR}

echoIt " "
echoIt "Checking parameters..."
echoIt " "

manual_entry_system="$(cat /tmp/${MANUAL_CONFIG_DIR}/SYSTEM_MANUAL)" 2>/dev/null

if [[ -n "$manual_entry_system" ]]; then
    echoIt "Manual system app location: $manual_entry_system"
    SYSTEM=${manual_entry_system}

elif [[ -d /system_root/system/app ]]; then
    SYSTEM=/system_root/system

elif [[ -d /system_root/app ]]; then
    SYSTEM=/system_root

elif [[ -d /system/system/app ]]; then
    SYSTEM=/system/system

elif [[ -d /system/app ]]; then
    SYSTEM=/system

fi

if [[ ${SYSTEM} != "" ]]; then
    echoIt "System app confirmed under: $SYSTEM"
    sleep 1s
else
    echoIt "System app directory detection failed. Trying from parameters"

    echo "DEBUG:: --- Contents of root (level 0) ---"
    echo "$(ls -l /)"
    echo "DEBUG:: --- End of contents ---"

    # Check SAR
    if [[ "$SAR" == "true" ]]
    then
        echoIt "System-as-root detected!"
        SYSTEM=/system_root
    else
        echoIt "Non System-as-root."
        SYSTEM=/system
    fi

    echo "DEBUG:: --- Contents of level 1 system ---"
    echo "$(ls -l ${SYSTEM})"
    echo "DEBUG:: --- End of contents ---"

    # Check A/B device
    ab_device="$(cat /proc/cmdline | grep slot_suffix)";
    if [[ -n "$ab_device" ]];
    then
        echoIt "A/B device!"
        SYSTEM=${SYSTEM}/system
    else
        echoIt "Only-A device."
    fi

    echo "DEBUG:: --- Contents of level 2 system ---"
    echo "$(ls -l ${SYSTEM})"
    echo "DEBUG:: --- End of contents ---"

    # If android 10, the app directory is always under /system/system/
    if [[ -z "$ab_device" ]] && [[ ! -e ${SYSTEM}/build.prop ]]; then
        SYSTEM=${SYSTEM}/system
    fi

    echoIt "Calculated system app under: $SYSTEM"
    sleep 1s

fi

echoIt " "

sleep 1s

# Check if ROM is present
build_prop="$SYSTEM/build.prop"
manual_entry_buildprop="$(cat /tmp/${MANUAL_CONFIG_DIR}/BUILDPROP_MANUAL)" 2>/dev/null

if [[ -n "$manual_entry_buildprop" ]]; then
    echoIt "Manual build.prop location: $manual_entry_buildprop"
    build_prop=${manual_entry_buildprop}
fi

if [[ -e "$build_prop" ]]; then
    echoIt "ROM is present."
else

    echo "DEBUG:: --- Possible build.prop locations ---"
    echo "$(find / -name build.prop)"
    echo "DEBUG:: --- End of contents ---"

    echoIt " "
    echoIt "------------!!!!!!!!!!------------"
    echoIt "No ROM detected"
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

            data_free=$(df -k /data | tail -1 | ${AWK1} '{print $4}')

    # sometimes, the above data command gets the percentage of data used
    # In that case, the data_free variable will contain % at the end
            case ${data_free} in
                *%)
                echoIt "Using third argument..."
                data_free=$(df -k /data | tail -1 | ${AWK1} '{print $3}')
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
                echoIt "-- Navigate to '$MIGRATE_CACHE'"
                echoIt "-- Delete the directory"
                echoIt " "
                echoIt "Additionally delete all tar.gz files from under /data/data/"
                echoIt " "

                exitNow
            fi

        # check free space in /system
        elif [[ "$key" == "system_required_size" ]]; then

            system_free=$(df -k ${SYSTEM} | tail -1 | ${AWK1} '{print $4}')

            case ${system_free} in
                *%)
                echoIt "Using third argument..."
                system_free=$(df -k /system | tail -1 | ${AWK1} '{print $3}')
                ;;
            esac

            if [[ ${system_free} -ge ${val} ]]; then
                echoIt "Free space ($SYSTEM) is OK ($system_free KB)"

            else

                echo "DEBUG:: --- df output /system---"
                echo "$(df -k ${SYSTEM})"
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
mkdir -p ${MIGRATE_CACHE}
mkdir -p ${MIGRATE_CACHE_DEFAULT}
cp /tmp/${PACKAGE_DATA_NAME} ${MIGRATE_CACHE}/"$PACKAGE_DATA_NAME"${TIMESTAMP}.txt && echoIt "Copied package data"

# export variables
echo "${OUTFD}" > /tmp/${MANUAL_CONFIG_DIR}/OUTFD
echo "${SYSTEM}" > /tmp/${MANUAL_CONFIG_DIR}/SYSTEM
echo "${MIGRATE_CACHE}" > /tmp/${MANUAL_CONFIG_DIR}/MIGRATE_CACHE

rm /tmp/prep.sh
