#!sbin/sh

# PARAMETERS: packageName destination busyBox
#                 1           2          3

APP_NAME="$1.app"
DATA_NAME="$1.tar.gz"
PERMISSION_NAME="$1.perm"
JSON_NAME="$1.json"
ICON_NAME="$1.icon"
DESTINATION="$2"
BUSYBOX="$3"

echoErr() { echo "$@" 1>&2; }
checkFile() {
    if [[ -z $1 && $1 != "NULL" ]]; then
        if [[ -e "$DESTINATION/$1" ]]; then
            echo "---FOUND $1---"
            if [[ $1 == *"tar.gz" ]]; then
                echo "---VERIFYING $1---"
                ${BUSYBOX} gzip -t "$DESTINATION/$1"
            fi
        else
            echoErr "---NOT FOUND $1---"
        fi
    fi
}

checkFile ${APP_NAME}
checkFile ${DATA_NAME}
checkFile ${PERMISSION_NAME}
checkFile ${JSON_NAME}
checkFile ${ICON_NAME}

echo "---VERIFICATION COMPLETE---"
echoErr "---VERIFICATION COMPLETE---"