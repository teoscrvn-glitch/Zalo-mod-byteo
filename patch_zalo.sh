#!/usr/bin/env bash
# ==============================================================================
# patch_zalo.sh - Apple iOS Frosted Glass UI Modder & Patcher for Zalo Android
# Merges XAPK/Split APKs, embeds full ZaloXposed mod features, and signs APK.
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}====================================================================${NC}"
echo -e "${BLUE}  Zalo Apple iOS Frosted Glass UI Modder & APK Patcher            ${NC}"
echo -e "${BLUE}====================================================================${NC}"

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${PROJECT_DIR}/build/patched"
WORK_DIR="${PROJECT_DIR}/build/patch_workspace"
OUTPUT_APK="${OUTPUT_DIR}/zalo-apple-glass-mod.apk"
KEYSTORE="${PROJECT_DIR}/debug.keystore"

INPUT_ARG="${1:-}"
if [ -n "$INPUT_ARG" ]; then
    INPUT_APK="$INPUT_ARG"
else
    # Auto-detect any zalo apk or xapk in Downloads
    FOUND_PKG=$(find /Users/thuthuy/Downloads -maxdepth 1 \( -iname "*zalo*.apk" -o -iname "*zalo*.xapk" \) -type f | head -n 1 || true)
    if [ -n "$FOUND_PKG" ]; then
        INPUT_APK="$FOUND_PKG"
        echo -e "${YELLOW}[!] Auto-detected Zalo package in Downloads:${NC} ${INPUT_APK}"
    else
        INPUT_APK="/Users/thuthuy/Downloads/zalo.apk"
    fi
fi

if [ ! -f "$INPUT_APK" ]; then
    echo -e "${RED}[ERROR] Input APK/XAPK not found at: ${INPUT_APK}${NC}"
    echo "Usage: ./patch_zalo.sh [path/to/zalo.apk or path/to/zalo.xapk]"
    exit 1
fi

# 2. Locate Android SDK tools
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-"/Users/thuthuy/Library/Android/sdk"}}"
BUILD_TOOLS_DIR=$(find "${ANDROID_SDK_ROOT}/build-tools" -maxdepth 1 -type d | sort -V | tail -n 1)

if [ -z "$BUILD_TOOLS_DIR" ] || [ ! -d "$BUILD_TOOLS_DIR" ]; then
    echo -e "${RED}[ERROR] Android build-tools not found in ${ANDROID_SDK_ROOT}/build-tools${NC}"
    exit 1
fi

ZIPALIGN="${BUILD_TOOLS_DIR}/zipalign"
APKSIGNER="${BUILD_TOOLS_DIR}/apksigner"

echo -e "${GREEN}[+] Using Build Tools:${NC} ${BUILD_TOOLS_DIR}"
echo -e "${GREEN}[+] Using zipalign:${NC}    ${ZIPALIGN}"
echo -e "${GREEN}[+] Using apksigner:${NC}   ${APKSIGNER}"

# 3. Locate or download LSPatch
LSPATCH_JAR="${PROJECT_DIR}/tools/lspatch.jar"
if [ ! -f "$LSPATCH_JAR" ]; then
    mkdir -p "${PROJECT_DIR}/tools"
    echo -e "${YELLOW}[!] Downloading LSPatch standalone engine...${NC}"
    curl -sSL -o "$LSPATCH_JAR" "https://github.com/LSPosed/LSPatch/releases/download/v0.6/jar-v0.6-398-release.jar"
fi
echo -e "${GREEN}[+] Using LSPatch:${NC}     ${LSPATCH_JAR}"

# 4. Prepare workspace
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
mkdir -p "$OUTPUT_DIR"

# 5. Extract XAPK if necessary
TARGET_BASE_APK="$INPUT_APK"
XAPK_EXTRACT_DIR="${WORK_DIR}/xapk_extracted"
SPLITS_CONTENT_DIR="${WORK_DIR}/splits_content"

if [[ "$INPUT_APK" == *.xapk ]]; then
    echo -e "\n${BLUE}[*] Unpacking XAPK container...${NC}"
    mkdir -p "$XAPK_EXTRACT_DIR"
    unzip -q "$INPUT_APK" -d "$XAPK_EXTRACT_DIR"
    
    if [ -f "${XAPK_EXTRACT_DIR}/com.zing.zalo.apk" ]; then
        TARGET_BASE_APK="${XAPK_EXTRACT_DIR}/com.zing.zalo.apk"
    elif [ -f "${XAPK_EXTRACT_DIR}/base.apk" ]; then
        TARGET_BASE_APK="${XAPK_EXTRACT_DIR}/base.apk"
    else
        TARGET_BASE_APK=$(find "$XAPK_EXTRACT_DIR" -maxdepth 2 -name "*.apk" -type f -exec ls -S {} + | head -n 1)
    fi
    echo -e "${GREEN}[+] Base APK identified:${NC} ${TARGET_BASE_APK}"
fi

# 6. Ensure ZaloXposed module APK is built
MODULE_APK="${PROJECT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$MODULE_APK" ]; then
    echo -e "${YELLOW}[!] Compiling ZaloXposed module APK...${NC}"
    "${PROJECT_DIR}/gradlew" assembleDebug
fi
echo -e "${GREEN}[+] Using ZaloXposed Module:${NC} ${MODULE_APK}"

# 7. Embed module via LSPatch
LSPATCH_OUT_DIR="${WORK_DIR}/lspatch_out"
mkdir -p "$LSPATCH_OUT_DIR"

echo -e "\n${BLUE}[*] Embedding full ZaloXposed mod features (Apple Frosted Glass + Anti-Recall)...${NC}"
java -jar "$LSPATCH_JAR" "$TARGET_BASE_APK" -m "$MODULE_APK" -l 2 -f -o "$LSPATCH_OUT_DIR"

PATCHED_BASE_APK=$(find "$LSPATCH_OUT_DIR" -maxdepth 1 -name "*-lspatched.apk" -type f | head -n 1 || true)
if [ -z "$PATCHED_BASE_APK" ] || [ ! -f "$PATCHED_BASE_APK" ]; then
    echo -e "${RED}[ERROR] LSPatch failed to generate output APK.${NC}"
    exit 1
fi

# 8. Merge native libraries & split assets if input was an XAPK
if [ -d "$XAPK_EXTRACT_DIR" ]; then
    echo -e "\n${BLUE}[*] Merging native libraries and density resources into standalone APK...${NC}"
    mkdir -p "$SPLITS_CONTENT_DIR"
    for split_apk in "$XAPK_EXTRACT_DIR"/config.*.apk; do
        if [ -f "$split_apk" ]; then
            unzip -q -o "$split_apk" -d "$SPLITS_CONTENT_DIR" || true
        fi
    done
    rm -f "$SPLITS_CONTENT_DIR/AndroidManifest.xml"
    (cd "$SPLITS_CONTENT_DIR" && zip -r -u -q "$PATCHED_BASE_APK" lib/ res/ 2>/dev/null || true)
    echo -e "${GREEN}[+] Successfully merged native libs and resources into standalone APK!${NC}"
fi

# 9. Align APK (4-byte boundary)
ALIGNED_APK="${WORK_DIR}/zalo_aligned.apk"
echo -e "\n${BLUE}[*] Aligning APK with zipalign (4-byte page alignment)...${NC}"
"$ZIPALIGN" -p -f 4 "$PATCHED_BASE_APK" "$ALIGNED_APK"

# 10. Sign APK with keystore
if [ ! -f "$KEYSTORE" ]; then
    echo -e "${YELLOW}[!] Generating self-signed debug keystore...${NC}"
    keytool -genkey -v -keystore "$KEYSTORE" -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

echo -e "${BLUE}[*] Signing APK with apksigner (v1, v2, v3, v4)...${NC}"
"$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --ks-key-alias androiddebugkey \
    --out "$OUTPUT_APK" \
    "$ALIGNED_APK"

# 11. Verify signature
echo -e "${BLUE}[*] Verifying APK signature...${NC}"
"$APKSIGNER" verify --verbose "$OUTPUT_APK"

# Also copy to Downloads for user convenience
cp -f "$OUTPUT_APK" "/Users/thuthuy/Downloads/zalo-apple-glass-mod.apk" 2>/dev/null || true

echo -e "\n${GREEN}====================================================================${NC}"
echo -e "${GREEN}  SUCCESS: Patched APK generated cleanly!                          ${NC}"
echo -e "${GREEN}  Output (Workspace): ${OUTPUT_APK}${NC}"
echo -e "${GREEN}  Output (Downloads): /Users/thuthuy/Downloads/zalo-apple-glass-mod.apk${NC}"
echo -e "${GREEN}====================================================================${NC}"
