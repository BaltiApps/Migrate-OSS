#!sbin/sh

chmod +x /tmp/busybox

# SAR detection kanged from Rockstar kernel zip of Violet
# And F-Droid scripts


SAR="false"

SAR_PROP="$(getprop ro.build.system_root_image)"
if [[ "$SAR_PROP" == "true" ]] || [[ -d /system_root && ! -f /system/build.prop ]]
then
    echo "Debug:: SAR device!"
	SAR="true"
fi

echo "DEBUG:: --- fstab ---"
echo "$(cat /etc/fstab)"
echo "DEBUG:: --- end of fstab ---"

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
		res="$(cat /etc/fstab | grep $1 | awk '{print $1}')"
		mount "$res" "$1"
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
		res="$(cat /proc/cmdline | grep slot_suffix)";
		if [[ -n "$res" ]]; then
			umountIt "/system_root/system"
		fi
		umountIt "/system_root"
	else
		umountIt "/system"
	fi
	umountIt "/data"
fi
