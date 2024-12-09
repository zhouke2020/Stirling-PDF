#!/bin/bash

if [[ "$FAT_DOCKER" != "true" ]]; then
    /scripts/download-security-jar.sh
fi

if [[ -n "$LANGS" ]]; then
    /scripts/installFonts.sh $LANGS
fi

exec "$@"