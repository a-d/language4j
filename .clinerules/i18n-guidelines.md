# Internationalization (i18n) Guidelines

## File Structure

The i18n system uses files in `backend/api-module/src/main/resources/i18n/`:

### Required Files
1. **`messages.json`** - Master definition file with context and metadata (always required)

### Bundled Translation Files
The directory contains bundled translation files that are shipped with the application. **These files are dynamic** - more languages may be added over time as the project grows:

- **`en.json`** - English translations (reference language)
- **`de.json`** - German translations
- *(Additional `{languageCode}.json` files may be added)*

When a new bundled language file is added to this directory:
1. Add the language code to the list of bundled languages in `I18nServiceImpl.java`
2. Ensure all keys from `messages.json` have corresponding translations

### LLM-Generated Languages
Languages not bundled in the resources directory are generated dynamically via LLM:
- Generated translations are cached to `data/i18n/{code}.json`
- Use context from `messages.json` for accurate translations

## Synchronization Rules

### When Adding New Translation Keys

**ALL THREE FILES MUST BE UPDATED TOGETHER:**

1. **`messages.json`** - Add the key with context, parameters, and examples
2. **`en.json`** - Add the English translation value
3. **`de.json`** - Add the German translation value

### Example: Adding a New Key

**1. In `messages.json`:**
```json
"users.newFeature": {
  "context": "Description of what this string is for and where it appears",
  "parameters": ["paramName"],  // if applicable
  "example": "Example output"   // helpful for translators
}
```

**2. In `en.json`:**
```json
"users.newFeature": "The English text with {paramName}"
```

**3. In `de.json`:**
```json
"users.newFeature": "Der deutsche Text mit {paramName}"
```

## Key Naming Conventions

- Use dot notation for namespacing: `category.subcategory.name`
- Common prefixes:
  - `nav.*` - Navigation items
  - `dashboard.*` - Dashboard page
  - `lessons.*`, `vocabulary.*`, `cards.*`, `exercises.*`, `progress.*`, `settings.*` - Page-specific
  - `profile.*` - User profile related
  - `goals.*` - Goal management
  - `toast.*` - Toast notifications
  - `misc.*` - Generic/shared strings
  - `lang.*` - Language names
  - `level.*` - CEFR skill levels
  - `users.*` - Multi-user management
  - `chat.*` - Chat coach interface
  - `action.*` - Quick action buttons
  - `activity.*` - Activity log entries

## Parameter Syntax

- Use `{paramName}` for dynamic values
- Document parameters in `messages.json` with the `parameters` array
- Keep parameter names descriptive: `{name}`, `{count}`, `{target}`, `{native}`

## Quality Checklist

Before committing i18n changes:

- [ ] New key added to `messages.json` with context
- [ ] New key added to `en.json` with English value
- [ ] New key added to `de.json` with German value
- [ ] All parameter placeholders use `{paramName}` format
- [ ] Key follows naming conventions
- [ ] No duplicate keys

## Frontend Usage

The frontend loads translations via:
- `t('key.path')` - Get translated string
- `t('key.path', { param: value })` - Get translated string with parameters
- `getLanguageName('code')` - Get language name (e.g., `getLanguageName('de')` → "German")

## LLM-Generated Translations

For languages not bundled (not `en` or `de`):
1. Frontend requests translations via `/api/v1/i18n/languages/{code}`
2. Backend uses LLM to translate from English using `messages.json` context
3. Generated translations are cached to `data/i18n/{code}.json`
4. Frontend caches in localStorage for offline resilience

## Regenerating Translations

To regenerate LLM translations for a specific language:
```
POST /api/v1/i18n/languages/{languageCode}/generate
```

Note: This does NOT work for bundled languages (en, de).