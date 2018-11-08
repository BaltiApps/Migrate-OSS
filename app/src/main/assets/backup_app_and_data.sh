#!sbin/sh

# PARAMETERS: packageName destination apkPath apkName dataParentPath dataName busyboxBinaryPath isPermission
#                   1           2        3       4          5           6              7              8

if [ ! -e "$2" ]; then
    echo "Destination for package $1: $2 does not exist. Making..."
    mkdir -p $2 2>/dev/null
fi

# backup permission
if [ "$8" = true ]; then
    dumpsys package $1 | grep android.permission | grep granted=true > "$2/$1.perm"
fi

# backup apk
cd $3; cp $4 "$2/$1.apk"
if [ -e "$2/$1.apk" ]; then
    echo "Apk copied"
fi

# backup data
if [ ! "$6" = "NULL" ]; then
    if [ -e "$5/$6" ]; then
        cd "$5"
        $7 tar -vczpf "$2/$1.tar.gz" "$6"
    else
        echo "Data path : $5/$6 does not exist"
    fi
fi