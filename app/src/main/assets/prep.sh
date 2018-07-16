#!sbin/sh

mkdir -p /system/app/
mkdir -p /system/priv-app/
mkdir -p /data/app/
mkdir -p /data/data/
rm -r /system/app/PermissionFixer/
rm -r /data/data/balti.migratehelper/
rm /cache/permissionList
rm /cache/prep.sh

rm -r /data/balti.migrate
mkdir -p /data/balti.migrate