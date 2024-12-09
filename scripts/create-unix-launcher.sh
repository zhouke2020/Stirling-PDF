#!/bin/bash
# scripts/create-unix-launcher.sh

cat > launcher.sh << 'EOF'
#!/bin/bash

# Configuration
APP_NAME="Stirling-PDF"
MIN_JAVA_VERSION="17"
PREFERRED_JAVA_VERSION="21"
JAVA_DOWNLOAD_URL="https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz"
BROWSER_OPEN="true"

# Check Java version
if type -p java > /dev/null; then
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
    _java="$JAVA_HOME/bin/java"
else
    echo "Java not found. Please install Java 21."
    xdg-open "$JAVA_DOWNLOAD_URL"
    exit 1
fi

version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [[ "$version" -lt "$MIN_JAVA_VERSION" ]]; then
    echo "Java version $version detected. Please install Java 21."
    xdg-open "$JAVA_DOWNLOAD_URL"
    exit 1
fi

# Run application
exec java -jar "$(dirname "$0")/Stirling-PDF.jar" "$@"
EOF

chmod +x launcher.sh
