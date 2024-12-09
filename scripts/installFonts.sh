#!/bin/bash

LANGS=$1
FONT_DIR="$HOME/.local/share/fonts"
TEMP_DIR=$(mktemp -d)

# Create fonts directory if it doesn't exist
mkdir -p "$FONT_DIR"

# Function to get latest GitHub release
get_latest_release() {
    local repo=$1
    local api_url="https://api.github.com/repos/$repo/releases/latest"
    curl --silent "$api_url" | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/'
}

# Function to download and install a font
install_font() {
    local font_name=$1
    echo "Installing font package: $font_name"
    
    # Map font package names to actual font URLs and installation methods
    case $font_name in
        "font-dejavu")
            local version=$(get_latest_release "dejavu-fonts/dejavu-fonts")
            version=${version#version_} # Remove 'version_' prefix
            local url="https://github.com/dejavu-fonts/dejavu-fonts/releases/download/version_${version}/dejavu-fonts-ttf-${version}.tar.bz2"
            wget -q "$url" -P "$TEMP_DIR" && \
            tar xjf "$TEMP_DIR/dejavu-fonts-ttf-${version}.tar.bz2" -C "$TEMP_DIR" && \
            find "$TEMP_DIR" -name "*.ttf" -exec cp {} "$FONT_DIR/" \;
            ;;
            
        "font-noto")
            # Base Noto Sans and Serif
            wget -q "https://noto-website-2.storage.googleapis.com/pkgs/NotoSans-hinted.zip" -P "$TEMP_DIR" && \
            wget -q "https://noto-website-2.storage.googleapis.com/pkgs/NotoSerif-hinted.zip" -P "$TEMP_DIR" && \
            unzip -q "$TEMP_DIR/NotoSans-hinted.zip" -d "$TEMP_DIR/noto-sans" && \
            unzip -q "$TEMP_DIR/NotoSerif-hinted.zip" -d "$TEMP_DIR/noto-serif" && \
            cp "$TEMP_DIR/noto-sans"/*.ttf "$FONT_DIR/" && \
            cp "$TEMP_DIR/noto-serif"/*.ttf "$FONT_DIR/"
            ;;
            
        "font-noto-cjk")
            # Noto CJK
            wget -q "https://github.com/notofonts/noto-cjk/raw/main/Sans/OTF/Japanese/NotoSansCJKjp-Regular.otf" -P "$FONT_DIR"
            wget -q "https://github.com/notofonts/noto-cjk/raw/main/Sans/OTF/Korean/NotoSansCJKkr-Regular.otf" -P "$FONT_DIR"
            wget -q "https://github.com/notofonts/noto-cjk/raw/main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf" -P "$FONT_DIR"
            wget -q "https://github.com/notofonts/noto-cjk/raw/main/Sans/OTF/TraditionalChinese/NotoSansCJKtc-Regular.otf" -P "$FONT_DIR"
            ;;
            
        "font-noto-arabic")
            wget -q "https://github.com/notofonts/noto-fonts/raw/main/hinted/ttf/NotoNaskhArabic/NotoNaskhArabic-Regular.ttf" -P "$FONT_DIR"
            wget -q "https://github.com/notofonts/noto-fonts/raw/main/hinted/ttf/NotoKufiArabic/NotoKufiArabic-Regular.ttf" -P "$FONT_DIR"
            ;;
            
        "font-noto-devanagari")
            wget -q "https://github.com/notofonts/noto-fonts/raw/main/hinted/ttf/NotoSansDevanagari/NotoSansDevanagari-Regular.ttf" -P "$FONT_DIR"
            wget -q "https://github.com/notofonts/noto-fonts/raw/main/hinted/ttf/NotoSerifDevanagari/NotoSerifDevanagari-Regular.ttf" -P "$FONT_DIR"
            ;;
            
        "font-noto-thai")
            wget -q "https://github.com/notofonts/noto-fonts/raw/main/hinted/ttf/NotoSansThai/NotoSansThai-Regular.ttf" -P "$FONT_DIR"
            wget -q "https://github.com/notofonts/noto-fonts/raw/main/hinted/ttf/NotoSerifThai/NotoSerifThai-Regular.ttf" -P "$FONT_DIR"
            ;;
            
        "font-noto-hebrew")
            wget -q "https://github.com/notofonts/noto-fonts/raw/main/hinted/ttf/NotoSansHebrew/NotoSansHebrew-Regular.ttf" -P "$FONT_DIR"
            ;;
            
        "font-awesome")
            local version=$(get_latest_release "FortAwesome/Font-Awesome")
            wget -q "https://use.fontawesome.com/releases/v${version}/fontawesome-free-${version}-desktop.zip" -P "$TEMP_DIR" && \
            unzip -q "$TEMP_DIR/fontawesome-free-${version}-desktop.zip" -d "$TEMP_DIR" && \
            cp "$TEMP_DIR/fontawesome-free-${version}-desktop/otfs"/*.otf "$FONT_DIR/"
            ;;
            
        "font-source-code-pro")
            local version=$(get_latest_release "adobe-fonts/source-code-pro")
            wget -q "https://github.com/adobe-fonts/source-code-pro/releases/download/${version}/TTF-source-code-pro-${version}.zip" -P "$TEMP_DIR" && \
            unzip -q "$TEMP_DIR/TTF-source-code-pro-${version}.zip" -d "$TEMP_DIR/source-code-pro" && \
            cp "$TEMP_DIR/source-code-pro"/*.ttf "$FONT_DIR/"
            ;;
            
        "font-vollkorn")
            wget -q "https://github.com/FAlthausen/Vollkorn-Typeface/raw/main/fonts/TTF/Vollkorn-Regular.ttf" -P "$FONT_DIR"
            ;;
            
        "font-liberation")
            wget -q "https://github.com/liberationfonts/liberation-fonts/files/7261482/liberation-fonts-ttf-2.1.5.tar.gz" -P "$TEMP_DIR" && \
            tar xzf "$TEMP_DIR/liberation-fonts-ttf-2.1.5.tar.gz" -C "$TEMP_DIR" && \
            cp "$TEMP_DIR/liberation-fonts-ttf-2.1.5"/*.ttf "$FONT_DIR/"
            ;;
    esac
    
    echo "Completed installation attempt for $font_name"
}

# Enhanced language-specific font mappings
declare -A language_fonts=(
    ["ar_AR"]="font-noto-arabic"
    ["zh_CN"]="font-noto-cjk"
    ["zh_TW"]="font-noto-cjk"
    ["ja_JP"]="font-noto font-noto-cjk"
    ["ru_RU"]="font-noto font-liberation font-vollkorn"
    ["sr_LATN_RS"]="font-noto font-liberation"
    ["uk_UA"]="font-noto font-liberation"
    ["ko_KR"]="font-noto font-noto-cjk"
    ["el_GR"]="font-noto"
    ["hi_IN"]="font-noto-devanagari"
    ["bg_BG"]="font-noto font-liberation"
    ["th_TH"]="font-noto-thai"
    ["he_IL"]="font-noto-hebrew"
    ["GENERAL"]="font-noto font-dejavu font-liberation font-source-code-pro font-awesome"
)

# Install fonts based on specified languages
if [[ $LANGS == "ALL" ]]; then
    # Install all fonts from the language_fonts map
    declare -A installed_fonts
    for fonts in "${language_fonts[@]}"; do
        for font in $fonts; do
            if [[ -z "${installed_fonts[$font]}" ]]; then
                install_font "$font"
                installed_fonts[$font]=1
            fi
        done
    done
else
    # Split comma-separated languages and install necessary fonts
    declare -A installed_fonts
    IFS=',' read -ra LANG_CODES <<< "$LANGS"
    for code in "${LANG_CODES[@]}"; do
        fonts_to_install=${language_fonts[$code]}
        if [ ! -z "$fonts_to_install" ]; then
            for font in $fonts_to_install; do
                if [[ -z "${installed_fonts[$font]}" ]]; then
                    install_font "$font"
                    installed_fonts[$font]=1
                fi
            done
        fi
    done
fi

# Cleanup
rm -rf "$TEMP_DIR"

# Update font cache
if command -v fc-cache >/dev/null; then
    fc-cache -f "$FONT_DIR"
    echo "Font cache updated"
else
    echo "Warning: fc-cache not found. You may need to manually update your font cache"
fi

echo "Font installation completed. Fonts installed in: $FONT_DIR"