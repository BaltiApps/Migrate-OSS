#!sbin/sh

# PARAMETERS: fileToCheck packageName destination busyBox
#                  1           2           3         4

FILE_TO_CHECK="$1"
PACKAGE_NAME="$2"
DESTINATION="$3"
BUSYBOX="$4"

echo " "

echoErr() { echo "$@" 1>&2; }
checkExistence(){
    if [[ -z $1 && $1 != "NULL" ]]; then
        if [[ -e "$DESTINATION/$1" ]]; then
            echo true
        else
            echo false
        fi
    fi
}

checkApp(){

    exists="$(checkExistence $1)"

    if [[ ${exists} == true ]]; then
        echo "--- APP $1 ---"
        if [[ ! -e "$DESTINATION/$PACKAGE_NAME.app/$FILE_TO_CHECK" ]]; then
            echo "--- ERROR - No base apk ---"
        else
            echo "--- OK $1 ---"
        fi
    fi

}

checkData(){

    exists="$(checkExistence $1)"

    if [[ ${exists} == true ]]; then
        echo "--- DATA $1 ---"
        err="$(${BUSYBOX} gzip -t "${DESTINATION}/$1" 2>&1)"
        if [[ ! -z "$err" ]]; then
            echoErr "--- ERROR - $1 - $err ---"
        else
            echo "--- OK $1 ---"
        fi
    fi

}