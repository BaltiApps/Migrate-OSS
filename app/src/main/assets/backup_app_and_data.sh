#!sbin/sh

# PARAMETERS: packageName destination apkPath apkName dataPath dataName busyBox ignoreCache apkNames
#                  1          2          3       4       5         6       7         8        9

echo " "

PACKAGE_NAME="$1"
DESTINATION="$2"
APK_PATH="$3"
APK_NAME="$4"
DATA_PATH="$5"
DATA_NAME="$6"
BUSYBOX="$7"
IGNORE_CACHE=$8
APK_NAMES_LIST_FILE=$9

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

    cd ${APK_PATH}

    # Loop over apk files and copy them.
    for f in *.apk; do

        display_name=""

        # Check if base apk and proceed accordingly.
        if [[ "$f" == "$APK_NAME" ]]; then
            display_name="$PACKAGE_NAME.apk"
            cp "$APK_NAME" "${appDir}/$display_name"
            [[ -e "$DESTINATION/$PACKAGE_NAME.app/$PACKAGE_NAME.apk" ]] && echo "Base apk copied" || echo "Base apk failed!"
        else
            display_name="$f"
            cp "$f" "${appDir}/" && echo "Copied split apk: $f"
        fi

        # Output the file name to APK_NAMES_LIST_FILE
        # Note that this actually checks the file in the destination and not the apk source.
        # Thus the result obtained is from the backed up file, hence verification is minimized.
        if [[ -n $APK_NAMES_LIST_FILE ]]; then
            echo -n ${PACKAGE_NAME}:${display_name}: >> $APK_NAMES_LIST_FILE

            file_location="$DESTINATION/${PACKAGE_NAME}.app/${display_name}"

            if [[ -e $file_location ]]; then
                ls -al $file_location | awk '{printf "%s\n", $5}' >> $APK_NAMES_LIST_FILE
            else
                echo -1 >> $APK_NAMES_LIST_FILE
            fi
        fi

    done

fi

# backup data
chmod +x ${BUSYBOX}
if [[ ${DATA_PATH} != "NULL" && ${DATA_NAME} != "NULL" ]]; then
    if [[ -e "$DATA_PATH/$DATA_NAME" ]]; then
        cd "$DATA_PATH"
        if [[ "${IGNORE_CACHE}" == "true" ]]; then
            ${BUSYBOX} tar -vczpf "$DESTINATION/$PACKAGE_NAME.tar.gz" "$DATA_NAME" --exclude="$DATA_NAME/cache"
        else
            ${BUSYBOX} tar -vczpf "$DESTINATION/$PACKAGE_NAME.tar.gz" "$DATA_NAME"
        fi
    else
        echo "Data path : $DATA_PATH/$DATA_NAME does not exist"
    fi
fi