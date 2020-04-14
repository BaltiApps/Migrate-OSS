#!sbin/sh

chmod +x /tmp/busybox

# SAR detection kanged from Rockstar kernel zip of Violet
# And F-Droid scripts

MANUAL_CONFIG_DIR="$2"

if [[ -n ${MANUAL_CONFIG_DIR} ]]; then
    echo "DEBUG:: --- making config dir (/tmp/$MANUAL_CONFIG_DIR) if does not exist ---"
    mkdir -p /tmp/${MANUAL_CONFIG_DIR}
fi

SAR="false"

SAR_PROP="$(getprop ro.build.system_root_image)"
if [[ "$SAR_PROP" == "true" ]] || [[ -d /system_root && -n "$(cat /etc/fstab | grep system_root)" ]]
then
    echo "Debug:: SAR device!"
    SAR="true"
fi

# export SAR status
echo "${SAR}" > /tmp/${MANUAL_CONFIG_DIR}/SAR

echo "DEBUG:: --- fstab ---"
echo "$(cat /etc/fstab)"
echo "DEBUG:: --- end of fstab ---"

AWK1="awk"

if [[ ! -x "$(command -v ${AWK1})" ]]; then
    AWK1="/tmp/busybox awk"
    echo "DEBUG:: Using busybox awk....."
    sleep 1s
fi

mountIt()
{
    echo "Debug:: mount $1"

    echo "Debug:: Directly internal toybox/busybox"
    mount "$1"

    res="$(cat /proc/mounts | grep $1)"
    if [[ -z "$res" ]]; then
        echo "Debug:: Using external busybox"
        /tmp/busybox mount "$1"
    fi

    res="$(cat /proc/mounts | grep $1)"
    if [[ -z "$res" ]]; then
        echo "Debug:: Using fstab"
        res="$(cat /etc/fstab | grep $1 | ${AWK1} '{print $1}')"
        mount "$res" "$1"
    fi

    if [[ "$1" == "/data" ]]; then

        echo "Debug:: Creating Test file"

        testF="/data/data/aFile"
        rm ${testF} 2>/dev/null
        touch ${testF} 2>/dev/null

        if [[ ! -e ${testF} ]]; then
            echo "Debug:: Last attempt to mount data..."
            /tmp/busybox mount -o rw,remount "$1"
            touch ${testF} 2>/dev/null
            if [[ ! -e ${testF} ]]; then
                echo "Debug:: Mount failed data..."
            else
                rm ${testF}
            fi
        else
            rm ${testF}
        fi

    fi

}
umountIt()
{
    echo "Debug:: Unmount $1"

    echo "Debug:: Using internal toybox/busybox"
    umount "$1"

    res="$(cat /proc/mounts | grep $1)"
    if [[ -n "$res" ]]; then
        echo "Debug:: Using external busybox"
        /tmp/busybox umount "$1"
    fi
}

if [[ "$1" == "m" ]]; then
    if [[ "$SAR" == "true" ]]; then
        mountIt "/system_root"
    else
        mountIt "/system"
    fi
    mountIt "/data"
elif [[ "$1" == "u" ]]; then
    if [[ "$SAR" == "true" ]]; then
        umountIt "/system_root"
    else
        umountIt "/system"
    fi
    umountIt "/data"
fi
