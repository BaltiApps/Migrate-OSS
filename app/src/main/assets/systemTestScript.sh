#!sbin/sh

PACKAGE_NAME=$1
APK_FULL_PATH=$2
DATA_PATH=$3
COPY_PATH=$4
BUSYBOX_PATH=$5

echo

# display PID
echo "--- PID: $$"

# remount
mount -o rw,remount /data

# copy apk and data
cp ${APK_FULL_PATH} "${COPY_PATH}/$PACKAGE_NAME.apk"
${BUSYBOX_PATH} tar -vczpf "$COPY_PATH/$PACKAGE_NAME.apk" ${DATA_PATH}

echo "--- Test done ---"