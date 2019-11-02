#!sbin/sh

# parameters:

SOURCE=$1
DEFAULT_MIGRATE_CACHE=$2
MANUAL_CONFIG_DIR=$3

read_migrate_cache="$(cat /tmp/${MANUAL_CONFIG_DIR}/MIGRATE_CACHE)"

if [[ -z "${read_migrate_cache}" ]]; then
    DESTINATION=${DEFAULT_MIGRATE_CACHE}/
else
    DESTINATION=${read_migrate_cache}/
fi

echo "DEBUG:: --- Moving $SOURCE $DESTINATION ---"
mv ${SOURCE} ${DESTINATION}