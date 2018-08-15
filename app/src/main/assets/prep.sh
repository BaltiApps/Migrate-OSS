#!sbin/sh

mkdir -p /system/app/
mkdir -p /system/priv-app/
mkdir -p /data/app/
mkdir -p /data/data/
rm -r /system/app/PermissionFixer/
rm -r /data/data/balti.migratehelper/

mkdir -p /data/balti.migrate

rm /tmp/prep.sh