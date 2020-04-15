#!sbin/sh

# parameters:

#SOURCE=$1
DEFAULT_MIGRATE_CACHE=$1
MANUAL_CONFIG_DIR=$2

DESTINATION=""
SOURCE=${DEFAULT_MIGRATE_CACHE}

read_migrate_cache="$(cat /tmp/${MANUAL_CONFIG_DIR}/MIGRATE_CACHE)"

if [[ -n "${read_migrate_cache}" ]]; then
    DESTINATION=${read_migrate_cache}/
    echo "DEBUG:: --- Attempting to move contents from $SOURCE to $DESTINATION ---"
    mv ${SOURCE}/* ${DESTINATION}
fi
