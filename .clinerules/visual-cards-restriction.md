# Visual Cards File Restriction

## Critical Rule

**NEVER read file contents from files matching `/frontend/demo-data/content/visual-cards/*.json`**

These files contain base64-encoded image data that will exceed the context window and break prompting.

## Assumed File Format

When working with visual card files, assume all files in this directory have the following consistent JSON structure:

```json
[
  {
    "url": "data:image/png;base64,[...]",
    "revisedPrompt": "Create a simple, clear educational illustration for the French word 'au revoir'. The image should be suitable for a language learning flashcard. Style: clean, minimalist, colorful, no text in the image. Context: Au revoir, merci !",
    "size": "512x512",
    "word": "au revoir"
  }
]
```

### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `url` | string | Base64-encoded PNG image data (very large) |
| `revisedPrompt` | string | The prompt used to generate the image |
| `size` | string | Image dimensions (e.g., "512x512") |
| `word` | string | The vocabulary word this image represents |

## Allowed Operations

- ✅ List files in the directory (`list_files`)
- ✅ Check if files exist
- ✅ Reference the file structure in documentation
- ✅ Modify code that processes these files (without reading them)

## Forbidden Operations

- ❌ `read_file` on any `*.json` file in `frontend/demo-data/content/visual-cards/`
- ❌ Including file contents in prompts or analysis

## Alternative Approaches

If you need to understand what visual cards exist:
1. Use `list_files` to see available files
2. Assume file names correspond to topics (e.g., `greetings.json` contains greeting-related visual cards)
3. Use the schema above to understand the data structure