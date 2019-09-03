#!sbin/sh

# PARAMETERS: packageName destination apkPath apkName dataPath dataName busyBox
#                   1          2         3       4       5         6      7

echo " "

PACKAGE_NAME="$1"
DESTINATION="$2"
APK_PATH="$3"
APK_NAME="$4"
DATA_PATH="$5"
DATA_NAME="$6"
BUSYBOX="$7"

if [[ ! -e "$DESTINATION" ]]; then
    echo "Destination for package $PACKAGE_NAME: $DESTINATION does not exist. Making..."
    mkdir -p ${DESTINATION} 2>/dev/null
fi

# starting version 1.3 all apks are backed up in <packageName>.app directory
# these include split apks also

if [[ ${APK_PATH} != "NULL" && ${APK_NAME} != "NULL" ]]; then

    # make app directory
    appDir="$DESTINATION/$PACKAGE_NAME.app"
    mkdir -p ${appDir}

    # backup apk
    cd ${APK_PATH}; cp "$APK_NAME" "${appDir}/$PACKAGE_NAME.apk"
    if [[ -e "$2/$1.app/$1.apk" ]]; then
        echo "Apk copied"
    fi

    # copy split apks (new in v2.0)
    cp ${APK_PATH}/split_*.apk "${appDir}/" 2>/dev/null && echo "Copied split apks"

fi

# backup data
if [[ ${DATA_PATH} != "NULL" && ${DATA_NAME} != "NULL" ]]; then
    if [[ -e "$DATA_PATH/$DATA_NAME" ]]; then
        cd "$DATA_PATH"
        ${BUSYBOX} tar -vczpf "$DESTINATION/$PACKAGE_NAME.tar.gz" "$DATA_NAME"
    else
        echo "Data path : $DATA_PATH/$DATA_NAME does not exist"
    fi
fi