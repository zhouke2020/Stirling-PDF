#!/bin/bash
# scripts/create-mac-launcher.sh

# Create app structure
APP_NAME="Stirling-PDF"
APP_BUNDLE="$APP_NAME.app"
mkdir -p "$APP_BUNDLE/Contents/"{MacOS,Resources,Java}

# Convert icon
sips -s format icns "src/main/resources/static/favicon.ico" --out "$APP_BUNDLE/Contents/Resources/AppIcon.icns"

# Create Info.plist
cat > "$APP_BUNDLE/Contents/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>$APP_NAME</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundleIdentifier</key>
    <string>com.frooodle.stirlingpdf</string>
    <key>CFBundleName</key>
    <string>$APP_NAME</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.10.0</string>
    <key>LSEnvironment</key>
    <dict>
        <key>BROWSER_OPEN</key>
        <string>true</string>
    </dict>
</dict>
</plist>
EOF

# Create launcher script
cat > "$APP_BUNDLE/Contents/MacOS/$APP_NAME" << 'EOF'
#!/bin/bash

# Configuration
MIN_JAVA_VERSION="17"
PREFERRED_JAVA_VERSION="21"
JAVA_DOWNLOAD_URL="https://download.oracle.com/java/21/latest/jdk-21_macos-x64_bin.dmg"

# Check Java version
if type -p java > /dev/null; then
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
    _java="$JAVA_HOME/bin/java"
else
    osascript -e 'display dialog "Java not found. Please install Java 21." buttons {"Download Java", "Cancel"} default button "Download Java"' \
    && open "$JAVA_DOWNLOAD_URL"
    exit 1
fi

version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [[ "$version" -lt "$MIN_JAVA_VERSION" ]]; then
    osascript -e 'display dialog "Wrong Java version. Please install Java 21." buttons {"Download Java", "Cancel"} default button "Download Java"' \
    && open "$JAVA_DOWNLOAD_URL"
    exit 1
fi

# Run application
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
exec java -jar "$DIR/../Java/Stirling-PDF.jar" "$@"
EOF

chmod +x "$APP_BUNDLE/Contents/MacOS/$APP_NAME"

# Copy JAR file
cp "build/libs/Stirling-PDF.jar" "$APP_BUNDLE/Contents/Java/"
