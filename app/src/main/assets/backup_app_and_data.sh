#!sbin/sh

# PARAMETERS: packageName destination apkPath apkName dataParentPath dataName busyboxBinaryPath isPermission
#                   1           2        3       4          5           6              7              8

if [ ! -e "$2" ]; then
    echo "Destination for package $1: $2 does not exist. Making..."
    mkdir -p $2 2>/dev/null
fi

chmod +w $2 2>/dev/null

# backup permission
if [ "$8" = true ]; then
    touch "$2/$1.perm" 2>/dev/null
    dumpsys package $2 | grep android.permission | grep granted=true > "$2/$1.perm"
fi

# backup apk
touch "$2/$1.apk" 2>/dev/null
cd $3; cp $4 "$2/$1.apk"
if [ -e "$2/$1.apk" ]; then
    echo "Apk copied"
fi

# backup data
if [ ! "$6" = "NULL" ] && [ -e "$5/$6" ]; then
    chmod +r $5/$6 2>/dev/null
    cd "$5"
    $7 tar -vczpf "$2/$1.tar.gz" "$6"
else
    echo "Data path : $5/$6 does not exist"
fi