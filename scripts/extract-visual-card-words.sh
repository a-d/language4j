#!/bin/bash
# Extract all words from visual-cards JSON files

cd /mnt/c/Users/James/IdeaProjects/language-learning/frontend/demo-data/content/visual-cards

for f in *.json; do
    echo "=== ${f%.json} ==="
    jq -r '.[].word' "$f"
done