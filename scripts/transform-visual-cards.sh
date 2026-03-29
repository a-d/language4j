#!/bin/bash
# Transform visual-cards JSON files to add translations, examples, and pronunciation
# Uses jq to process files without loading them fully into memory

BASE_DIR="/"
CARDS_DIR="$BASE_DIR/frontend/demo-data/content/visual-cards"
TRANSLATIONS="$BASE_DIR/scripts/visual-cards-translations.json"

echo "Starting visual cards transformation..."

for f in "$CARDS_DIR"/*.json; do
    filename=$(basename "$f")
    echo "Processing: $filename"
    
    # Create a temp file for output
    temp_file=$(mktemp)
    
    # Use jq to add the new fields based on translations
    jq --slurpfile trans "$TRANSLATIONS" '
        [.[] | . + (
            $trans[0][.word] // {
                "nativeWord": .word,
                "exampleSentence": null,
                "pronunciation": null
            }
        )]
    ' "$f" > "$temp_file"
    
    # Check if jq succeeded
    if [ $? -eq 0 ] && [ -s "$temp_file" ]; then
        mv "$temp_file" "$f"
        echo "  ✓ Updated $filename"
    else
        rm -f "$temp_file"
        echo "  ✗ Failed to update $filename"
    fi
done

echo "Done!"