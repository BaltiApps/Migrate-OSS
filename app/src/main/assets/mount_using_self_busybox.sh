#!sbin/sh

chmod +x /tmp/busybox
if [ "$1" = "m" ]; then
    /tmp/busybox mount /system
    /tmp/busybox mount /data
elif [ "$1" = "u" ]; then
    /tmp/busybox umount /system
    /tmp/busybox umount /data
fi