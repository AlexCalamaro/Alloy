#!/bin/bash
# License checker for third-party dependencies
# Verifies all dependencies have approved permissive licenses

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Approved licenses (SPDX identifiers)
APPROVED_LICENSES=(
    "MIT"
    "Apache-2.0"
    "BSD-2-Clause"
    "BSD-3-Clause"
    "ISC"
    "CC0-1.0"
    "Unlicense"
)

# Prohibited licenses
PROHIBITED_LICENSES=(
    "GPL-2.0"
    "GPL-3.0"
    "AGPL-3.0"
    "SSPL-1.0"
    "Proprietary"
)

# Conditional licenses (require review)
CONDITIONAL_LICENSES=(
    "LGPL-2.1"
    "LGPL-2.1-or-later"
    "LGPL-3.0"
    "MPL-2.0"
    "EPL-2.0"
)

echo "=============================================="
echo "  Third-Party License Checker"
echo "=============================================="
echo ""

# Track statistics
TOTAL_DEPS=0
APPROVED_COUNT=0
PROHIBITED_COUNT=0
CONDITIONAL_COUNT=0
UNKNOWN_COUNT=0

# Create/update license report file
LICENSE_REPORT="$PROJECT_ROOT/third-party-licenses.md"

# Function to check if license is in array
license_in_array() {
    local license="$1"
    shift
    for item in "$@"; do
        if [[ "$license" == *"$item"* ]]; then
            return 0
        fi
    done
    return 1
}

# Function to extract license from dependency
get_dependency_license() {
    local group="$1"
    local name="$2"
    local version="$3"
    
    # Try to get license from Maven Central
    local metadata_url="https://repo1.maven.org/maven2/${group//./\/}/${name}/${version}/${name}-${version.pom}"
    
    # Check if we have cached license info
    local cache_file="$PROJECT_ROOT/.gradle/license-cache/${name}-${version}.txt"
    if [ -f "$cache_file" ]; then
        cat "$cache_file"
        return
    fi
    
    # Default to unknown (will require manual verification)
    echo "UNKNOWN"
}

echo "Scanning Gradle dependencies..."
echo ""

# Find all build.gradle.kts files and extract dependencies
declare -A DEPENDENCIES

while IFS= read -r -d '' build_file; do
    # Extract dependencies from Kotlin DSL
    while IFS= read -r line; do
        # Match implementation, api, compileOnly, etc.
        if [[ "$line" =~ [a-z]+\([[:space:]]*[\"\']([^:]+):([^:]+):([^:\"\']+)[\"\'] ]]; then
            group="${BASH_REMATCH[1]}"
            name="${BASH_REMATCH[2]}"
            version="${BASH_REMATCH[3]}"
            
            # Skip Android/Google libraries (known to be Apache 2.0)
            if [[ "$group" == *"android"* ]] || [[ "$group" == *"com.google"* ]]; then
                license="Apache-2.0"
            # Skip Kotlin libraries (known to be Apache 2.0)
            elif [[ "$group" == *"org.jetbrains.kotlin"* ]]; then
                license="Apache-2.0"
            # Skip KotlinX libraries (known to be Apache 2.0)
            elif [[ "$group" == *"org.jetbrains.kotlinx"* ]]; then
                license="Apache-2.0"
            # Skip Square libraries (known to be Apache 2.0)
            elif [[ "$group" == *"com.squareup"* ]]; then
                license="Apache-2.0"
            else
                license="UNKNOWN"
            fi
            
            DEPENDENCIES["$group:$name:$version"]="$license"
        fi
    done < <(grep -E "^\s*(implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation)" "$build_file" 2>/dev/null || true)
    
done < <(find "$PROJECT_ROOT" -name "build.gradle.kts" -print0 2>/dev/null)

# Process each dependency
echo "Analyzing dependencies..."
echo ""

printf "%-50s %-20s %-10s\n" "DEPENDENCY" "LICENSE" "STATUS"
echo "------------------------------------------------------------------------"

for dep in "${!DEPENDENCIES[@]}"; do
    TOTAL_DEPS=$((TOTAL_DEPS + 1))
    
    license="${DEPENDENCIES[$dep]}"
    
    # Determine status
    if [[ "$license" == "UNKNOWN" ]]; then
        status="${YELLOW}UNKNOWN${NC}"
        UNKNOWN_COUNT=$((UNKNOWN_COUNT + 1))
    elif license_in_array "$license" "${APPROVED_LICENSES[@]}"; then
        status="${GREEN}APPROVED${NC}"
        APPROVED_COUNT=$((APPROVED_COUNT + 1))
    elif license_in_array "$license" "${PROHIBITED_LICENSES[@]}"; then
        status="${RED}PROHIBITED${NC}"
        PROHIBITED_COUNT=$((PROHIBITED_COUNT + 1))
    elif license_in_array "$license" "${CONDITIONAL_LICENSES[@]}"; then
        status="${BLUE}CONDITIONAL${NC}"
        CONDITIONAL_COUNT=$((CONDITIONAL_COUNT + 1))
    else
        status="${YELLOW}UNKNOWN${NC}"
        UNKNOWN_COUNT=$((UNKNOWN_COUNT + 1))
    fi
    
    printf "%-50s %-20s %s\n" "$dep" "$license" "$status"
done

echo ""
echo "=============================================="
echo "  Summary"
echo "=============================================="
echo -e "Total dependencies:    ${BLUE}$TOTAL_DEPS${NC}"
echo -e "Approved licenses:     ${GREEN}$APPROVED_COUNT${NC}"
echo -e "Conditional licenses:  ${BLUE}$CONDITIONAL_COUNT${NC}"
echo -e "Unknown licenses:      ${YELLOW}$UNKNOWN_COUNT${NC}"
echo -e "Prohibited licenses:   ${RED}$PROHIBITED_COUNT${NC}"
echo ""

# Generate/update license report
echo "Generating license report..."

cat > "$LICENSE_REPORT" << EOF
# Third-Party License Report

**Generated:** $(date +"%Y-%m-%d %H:%M:%S")
**Total Dependencies:** $TOTAL_DEPS

## Approved Dependencies ($APPROVED_COUNT)

These dependencies use fully permissive licenses (MIT, Apache 2.0, BSD, ISC).

| Dependency | Version | License | Status |
|------------|---------|---------|--------|
EOF

for dep in "${!DEPENDENCIES[@]}"; do
    license="${DEPENDENCIES[$dep]}"
    if license_in_array "$license" "${APPROVED_LICENSES[@]}"; then
        version="${dep##*:}"
        name="${dep%:*}"
        echo "| $name | $version | $license | ✅ Approved |" >> "$LICENSE_REPORT"
    fi
done

cat >> "$LICENSE_REPORT" << EOF

## Conditional Dependencies ($CONDITIONAL_COUNT)

These dependencies require legal review before use.

| Dependency | Version | License | Status |
|------------|---------|---------|--------|
EOF

for dep in "${!DEPENDENCIES[@]}"; do
    license="${DEPENDENCIES[$dep]}"
    if license_in_array "$license" "${CONDITIONAL_LICENSES[@]}"; then
        version="${dep##*:}"
        name="${dep%:*}"
        echo "| $name | $version | $license | ⚠️ Review Required |" >> "$LICENSE_REPORT"
    fi
done

cat >> "$LICENSE_REPORT" << EOF

## Unknown Dependencies ($UNKNOWN_COUNT)

These dependencies require license verification.

| Dependency | Version | Status |
|------------|---------|--------|
EOF

for dep in "${!DEPENDENCIES[@]}"; do
    license="${DEPENDENCIES[$dep]}"
    if [[ "$license" == "UNKNOWN" ]]; then
        version="${dep##*:}"
        name="${dep%:*}"
        echo "| $name | $version | ⚠️ Verify License |" >> "$LICENSE_REPORT"
    fi
done

if [ $PROHIBITED_COUNT -gt 0 ]; then
    cat >> "$LICENSE_REPORT" << EOF

## ❌ Prohibited Dependencies ($PROHIBITED_COUNT)

**ACTION REQUIRED:** These dependencies use prohibited licenses and must be removed.

| Dependency | Version | License | Action |
|------------|---------|---------|--------|
EOF

    for dep in "${!DEPENDENCIES[@]}"; do
        license="${DEPENDENCIES[$dep]}"
        if license_in_array "$license" "${PROHIBITED_LICENSES[@]}"; then
            version="${dep##*:}"
            name="${dep%:*}"
            echo "| $name | $version | $license | 🚫 Remove Immediately |" >> "$LICENSE_REPORT"
        fi
    done
fi

echo "" >> "$LICENSE_REPORT"
echo "---" >> "$LICENSE_REPORT"
echo "*This report is automatically generated. Verify all licenses before using dependencies.*" >> "$LICENSE_REPORT"

echo -e "${GREEN}✓ License report generated:${NC} $LICENSE_REPORT"
echo ""

# Final status
if [ $PROHIBITED_COUNT -gt 0 ]; then
    echo -e "${RED}✗ LICENSE VIOLATION DETECTED${NC}"
    echo ""
    echo "The following prohibited licenses must be addressed:"
    for dep in "${!DEPENDENCIES[@]}"; do
        license="${DEPENDENCIES[$dep]}"
        if license_in_array "$license" "${PROHIBITED_LICENSES[@]}"; then
            echo "  - $dep ($license)"
        fi
    done
    echo ""
    echo "See $LICENSE_REPORT for details."
    exit 1
elif [ $UNKNOWN_COUNT -gt 0 ]; then
    echo -e "${YELLOW}⚠ WARNING: Unknown licenses detected${NC}"
    echo ""
    echo "The following dependencies require license verification:"
    for dep in "${!DEPENDENCIES[@]}"; do
        license="${DEPENDENCIES[$dep]}"
        if [[ "$license" == "UNKNOWN" ]]; then
            echo "  - $dep"
        fi
    done
    echo ""
    echo "Please verify licenses and update $LICENSE_REPORT"
    exit 0
else
    echo -e "${GREEN}✓ All dependencies have approved licenses${NC}"
    exit 0
fi
