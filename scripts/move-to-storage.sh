#!/bin/bash
# Move generated markdown files from root to .md-storage

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STORAGE_DIR="$PROJECT_ROOT/.md-storage"

echo "=============================================="
echo "  Move Generated MD Files to Storage"
echo "=============================================="
echo ""

# Create storage directories
mkdir -p "$STORAGE_DIR/audit-reports"
mkdir -p "$STORAGE_DIR/planning"
mkdir -p "$STORAGE_DIR/summaries"
mkdir -p "$STORAGE_DIR/temporary"

# Define file categories based on filename patterns
move_file() {
    local file="$1"
    local category="$2"
    local filename=$(basename "$file")
    local dest="$STORAGE_DIR/$category"
    
    # Generate new filename with date if not present
    if [[ ! "$filename" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}- ]]; then
        local date_prefix=$(date +%Y-%m-%d)
        local base_name="${filename%.md}"
        filename="${date_prefix}-${base_name}.md"
    fi
    
    # Move file
    mv "$file" "$dest/$filename"
    echo -e "${GREEN}✓${NC} Moved: $(basename "$file") -> .md-storage/$category/$filename"
}

# Find markdown files in root (excluding permanent docs)
FOUND_FILES=0

for file in "$PROJECT_ROOT"/*.md; do
    [ -f "$file" ] || continue
    
    filename=$(basename "$file")
    
    # Skip permanent documentation files
    if [[ "$filename" =~ ^(README|ANDROID_BEST_PRACTICES|LICENSE|CHANGELOG|CONTRIBUTING|CODE_OF_CONDUCT) ]]; then
        echo -e "${BLUE}Skipping permanent doc:${NC} $filename"
        continue
    fi
    
    FOUND_FILES=$((FOUND_FILES + 1))
    
    # Categorize based on filename patterns
    case "$filename" in
        *WORK_PLAN*|*PLAN*|*TASK*)
            move_file "$file" "planning"
            ;;
        *SUMMARY*|*METRIC*|*QUALITY*)
            move_file "$file" "summaries"
            ;;
        *AUDIT*|*ANTIPATTERN*|*REVIEW*|*ANALYSIS*)
            move_file "$file" "audit-reports"
            ;;
        *TEMP*|*WIP*|*DRAFT*)
            move_file "$file" "temporary"
            ;;
        *)
            # Default to temporary for unknown files
            move_file "$file" "temporary"
            ;;
    esac
done

echo ""
echo "=============================================="

if [ $FOUND_FILES -eq 0 ]; then
    echo -e "${GREEN}✓ No generated markdown files to move${NC}"
else
    echo -e "${GREEN}✓ Moved $FOUND_FILES file(s) to .md-storage/${NC}"
fi

echo "=============================================="
