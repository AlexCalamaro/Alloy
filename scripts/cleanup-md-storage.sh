#!/bin/bash
# Cleanup script for .md-storage directory
# Archives files older than 90 days, deletes files older than 180 days

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
STORAGE_DIR="$PROJECT_ROOT/.md-storage"

# Thresholds (in days)
ARCHIVE_THRESHOLD=90
DELETE_THRESHOLD=180

echo "=============================================="
echo "  .md-storage Cleanup Script"
echo "=============================================="
echo ""

# Check if storage directory exists
if [ ! -d "$STORAGE_DIR" ]; then
    echo -e "${YELLOW}⚠ .md-storage directory does not exist${NC}"
    echo "Nothing to clean up."
    exit 0
fi

# Count statistics
TOTAL_FILES=0
ARCHIVE_COUNT=0
DELETE_COUNT=0
KEEP_COUNT=0

# Create archive directory if it doesn't exist
mkdir -p "$STORAGE_DIR/archived"

echo "Scanning files in $STORAGE_DIR..."
echo ""

# Find all .md files (excluding archived directory)
while IFS= read -r -d '' file; do
    TOTAL_FILES=$((TOTAL_FILES + 1))
    
    # Get file age in days
    FILE_AGE=$(($(date +%s) - $(stat -c %Y "$file")))
    FILE_AGE_DAYS=$((FILE_AGE / 86400))
    
    # Get filename for display
    FILENAME=$(basename "$file")
    REL_PATH="${file#$PROJECT_ROOT/}"
    
    # Determine action based on age
    if [ $FILE_AGE_DAYS -ge $DELETE_THRESHOLD ]; then
        ACTION="DELETE"
        DELETE_COUNT=$((DELETE_COUNT + 1))
    elif [ $FILE_AGE_DAYS -ge $ARCHIVE_THRESHOLD ]; then
        ACTION="ARCHIVE"
        ARCHIVE_COUNT=$((ARCHIVE_COUNT + 1))
    else
        ACTION="KEEP"
        KEEP_COUNT=$((KEEP_COUNT + 1))
    fi
    
    # Display file info
    printf "%-60s %s (%d days old)\n" "$REL_PATH" "$ACTION" "$FILE_AGE_DAYS"
    
    # Perform action (dry-run by default)
    if [ "$1" != "--dry-run" ]; then
        case $ACTION in
            "DELETE")
                rm -f "$file"
                echo -e "  ${RED}Deleted:${NC} $REL_PATH"
                ;;
            "ARCHIVE")
                # Move to archived directory, preserving subdirectory structure
                ARCHIVE_DEST="$STORAGE_DIR/archived/$(dirname "${file#$STORAGE_DIR/}")"
                mkdir -p "$ARCHIVE_DEST"
                mv "$file" "$ARCHIVE_DEST/"
                echo -e "  ${YELLOW}Archived:${NC} $REL_PATH -> archived/$(basename "${file#$STORAGE_DIR/}")"
                ;;
        esac
    fi
    
done < <(find "$STORAGE_DIR" -path "$STORAGE_DIR/archived" -prune -o -name "*.md" -type f -print0)

echo ""
echo "=============================================="
echo "  Summary"
echo "=============================================="
echo -e "Total files scanned: ${BLUE}$TOTAL_FILES${NC}"
echo -e "Files to keep:       ${GREEN}$KEEP_COUNT${NC}"
echo -e "Files archived:      ${YELLOW}$ARCHIVE_COUNT${NC}"
echo -e "Files deleted:       ${RED}$DELETE_COUNT${NC}"
echo ""

# Check for markdown files in root directory (excluding permanent docs)
echo "Checking for markdown files in project root..."
ROOT_MD_FILES=$(find "$PROJECT_ROOT" -maxdepth 1 -name "*.md" -type f | grep -v -E "(README|ANDROID_BEST_PRACTICES|LICENSE|CHANGELOG|CONTRIBUTING)" || true)

if [ -n "$ROOT_MD_FILES" ]; then
    echo -e "${YELLOW}⚠ Found generated markdown files in root:${NC}"
    echo "$ROOT_MD_FILES" | while read -r file; do
        echo "  - $(basename "$file")"
    done
    echo ""
    echo -e "${YELLOW}Recommendation: Move to .md-storage/ directory${NC}"
    echo "Run: ./scripts/move-to-storage.sh"
else
    echo -e "${GREEN}✓ No generated markdown files in root${NC}"
fi

echo ""
echo "=============================================="

# Final message
if [ "$1" = "--dry-run" ]; then
    echo -e "${BLUE}DRY RUN - No files were modified${NC}"
    echo "Run without --dry-run to apply changes"
else
    echo -e "${GREEN}Cleanup complete!${NC}"
fi

echo "=============================================="
