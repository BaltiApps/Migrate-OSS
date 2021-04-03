#!sbin/sh

PACKAGE_NAME=$1
APK_FULL_PATH=$2
DATA_PATH=$3
DATA_NAME=$4
COPY_PATH=$5
BUSYBOX_PATH=$6

echo

# display PID
echo "--- PID: $$"

# remount
#mount -o rw,remount /data

# copy apk and data
cp -v ${APK_FULL_PATH} "${COPY_PATH}/$PACKAGE_NAME.apk"
cd ${DATA_PATH}
chmod +x "$BUSYBOX_PATH"
${BUSYBOX_PATH} tar -vczpf "$COPY_PATH/$PACKAGE_NAME.tar.gz" ${DATA_NAME}

echo "--- Test done ---"