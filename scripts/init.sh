#!/bin/bash
# Copy the original tesseract-ocr files to the volume directory without overwriting existing files
echo "Copying original files without overwriting existing files"
cp -rn /usr/share/tessdata-original/* /usr/share/tessdata 2>/dev/null || true

# Copy additional tessdata if available
if [ -d /usr/share/tesseract-ocr/4.00/tessdata ]; then
    cp -rn /usr/share/tesseract-ocr/4.00/tessdata/* /usr/share/tessdata 2>/dev/null || true
fi

if [ -d /usr/share/tesseract-ocr/5/tessdata ]; then
    cp -rn /usr/share/tesseract-ocr/5/tessdata/* /usr/share/tessdata 2>/dev/null || true
fi

# Check if TESSERACT_LANGS environment variable is set and is not empty
if [[ -n "$TESSERACT_LANGS" ]]; then
    # Convert comma-separated values to a space-separated list
    TES_LANGS=$(echo $TESSERACT_LANGS | tr ',' ' ')
    pattern='^[a-zA-Z]{2,4}(_[a-zA-Z]{2,4})?$'

    # Log available languages
    echo "Currently installed languages:"
    tesseract --list-langs

    echo "Requested additional languages: $TES_LANGS"
    
    # Instead of apk add, download language files from a known source
    for LANG in $TES_LANGS; do
        if [[ $LANG =~ $pattern ]]; then
            # Download to user-writable directory
            wget -P /usr/share/tessdata/ "https://github.com/tesseract-ocr/tessdata/raw/main/${LANG}.traineddata" || \
            echo "Failed to download language pack for ${LANG}"
        else
            echo "Skipping invalid language code"
        fi
    done
fi

/scripts/init-without-ocr.sh "$@"