# 🌍 Language Learning Platform

An AI-powered, deployable language learning platform that adapts to user configuration. Users specify their native language, target language, and AI provider credentials to receive a personalized learning experience.

## ✨ Features

- **AI Chat Coach** - Interactive learning assistant with streaming responses and embedded exercises
- **Personalized Learning Plans** - AI-generated learning paths based on your goals and skill level
- **Interactive Exercises** - Fill-in-the-blank, drag-and-drop, translation, listening, and speaking exercises
- **Flashcards with Images** - AI-generated vocabulary cards with visual aids
- **Progress Tracking** - Daily, weekly, monthly, and yearly goal tracking
- **Skill Assessment** - Automatic CEFR level assessment (A1-C2)
- **Multi-Provider AI Support** - Works with OpenAI, Anthropic, or local Ollama
- **Roleplay Scenarios** - Practice conversations in realistic situations

## 📸 Screenshots

<table>
  <tr>
    <td align="center"><strong>Dashboard</strong><br/><img src="https://github.com/user-attachments/assets/ed94484f-f8d0-4532-a555-c2e2a1422210" alt="Dashboard" /></td>
    <td align="center"><strong>AI Chat Coach</strong><br/><img src="https://github.com/user-attachments/assets/e3f69288-51e6-4d6b-89fe-600aad1fd299" alt="AI Chat Coach" /></td>
  </tr>
  <tr>
    <td align="center"><strong>Interactive Exercises</strong><br/><img src="https://github.com/user-attachments/assets/72f08ac6-e2d6-4474-ba21-c7fba41ebabb" alt="Interactive Exercises" /></td>
    <td align="center"><strong>Vocabulary Lists</strong><br/><img src="https://github.com/user-attachments/assets/5c4aab33-592e-4c17-a0f1-1a4efe22644e" alt="Vocabulary Lists" /></td>
  </tr>
  <tr>
    <td align="center"><strong>Flashcards</strong><br/><img src="https://github.com/user-attachments/assets/9433ea25-0374-45c8-899d-4d10642c086e" alt="Flashcards" /></td>
    <td align="center"><strong>Visual Cards with AI Images</strong><br/><img src="https://github.com/user-attachments/assets/a30ec4b7-98a3-4000-8351-7fab031f6c02" alt="Visual Cards with AI Images" /></td>
  </tr>
</table>

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose
- An API key from your preferred AI provider (OpenAI, Anthropic, or Ollama running locally)

### 1. Clone and Configure

```bash
# Clone the repository
git clone <repository-url>
cd language-learning

# Copy and edit environment configuration
cp .env.example .env
```

Edit `.env` with your settings:

```bash
# Your languages
NATIVE_LANGUAGE=de     # Your native language (e.g., de for German)
TARGET_LANGUAGE=fr     # Language you want to learn (e.g., fr for French)

# AI Configuration
LLM_PROVIDER=openai    # or anthropic, ollama
LLM_API_KEY=your-api-key-here
LLM_MODEL=gpt-4-turbo-preview
```

### 2. Start the Platform

```bash
# Start all services
cd docker
docker-compose up -d

# Or with specific environment variables
NATIVE_LANGUAGE=de TARGET_LANGUAGE=fr LLM_API_KEY=your-key docker-compose up -d
```

### 3. Access the Application

Open your browser to `http://localhost` to start learning!

## 📖 Configuration Options

### Language Codes (ISO 639-1)

| Code | Language   | Code | Language   |
|------|------------|------|------------|
| de   | German     | es   | Spanish    |
| fr   | French     | it   | Italian    |
| en   | English    | pt   | Portuguese |
| nl   | Dutch      | pl   | Polish     |
| ru   | Russian    | ja   | Japanese   |
| zh   | Chinese    | ko   | Korean     |

### AI Provider Configuration

#### OpenAI
```bash
LLM_PROVIDER=openai
LLM_API_KEY=sk-...
LLM_MODEL=gpt-4-turbo-preview  # or gpt-4, gpt-3.5-turbo
```

#### Anthropic
```bash
LLM_PROVIDER=anthropic
LLM_API_KEY=sk-ant-...
LLM_MODEL=claude-3-opus  # or claude-3-sonnet
```

#### Ollama (Local)
```bash
LLM_PROVIDER=ollama
LLM_BASE_URL=http://host.docker.internal:11434
LLM_MODEL=llama2  # or mistral, mixtral, etc.
```

### Speech Services (Optional)

The platform supports text-to-speech and speech-to-text for pronunciation practice:

```bash
# Uses LLM_API_KEY by default, or set separately
TTS_API_KEY=your-key
STT_API_KEY=your-key
TTS_VOICE=alloy  # OpenAI voices: alloy, echo, fable, onyx, nova, shimmer
```

### Image Generation (Optional)

Generate images for flashcards:

```bash
IMAGE_API_KEY=your-key
IMAGE_MODEL=dall-e-3
```

## 🏗️ Architecture

```
┌─────────────────┐
│   Frontend      │  Vanilla HTML/CSS/JS
│   (Nginx)       │  Port 80
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Backend API   │  Spring Boot + Spring AI
│   (Java 21)     │  Port 8080
└────────┬────────┘
         │
    ┌────┴─────┐
    ▼          ▼
┌────────┐ ┌───────┐
│Postgres│ │  AI   │
│  DB    │ │  APIs │
└────────┘ └───────┘
```

### Module Structure

```
backend/
├── config-module/      # Configuration properties
├── core-module/        # Domain entities and repositories
├── llm-module/         # Spring AI LLM integration
├── api-module/         # REST API controllers
├── chat-service/       # AI chat coach with embedded activities
├── content-service/    # Lesson and content generation
├── learning-service/   # Progress and goal tracking
├── speech-service/     # TTS/STT integration
└── image-service/      # Image generation
```

## 🛠️ Development

### Prerequisites for Development

- Java 21+
- Maven 3.9+
- Node.js 20+ (optional, for frontend tooling)
- PostgreSQL 16+ (or use Docker)

### Local Development

```bash
# Start PostgreSQL
cd docker
docker-compose up -d postgres

# Build backend
cd ../backend
mvn clean install

# Run the application
mvn -pl api-module spring-boot:run

# Access at http://localhost:8080
```

### Frontend Development

The frontend is vanilla HTML/CSS/JS - just open `frontend/index.html` in your browser or use a local server:

```bash
cd frontend
python -m http.server 3000
# or
npx serve .
```

## 📚 Learning Features

### Exercise Types

1. **Fill in the Blanks** - Complete sentences with missing words
2. **Word Order** - Drag and drop words to form correct sentences
3. **Translation** - Translate sentences between languages
4. **Listening** - Listen and transcribe audio
5. **Speaking** - Record yourself and get pronunciation feedback

### CEFR Levels

The platform adapts content to your skill level:

- **A1** - Beginner
- **A2** - Elementary
- **B1** - Intermediate
- **B2** - Upper Intermediate
- **C1** - Advanced
- **C2** - Proficient

### Goal Tracking

Set and track goals at multiple levels:
- **Daily** - e.g., "Learn 10 words"
- **Weekly** - e.g., "Complete 5 lessons"
- **Monthly** - e.g., "Reach A2 level"
- **Yearly** - e.g., "Achieve conversational fluency"

## 🔧 API Reference

### OpenAPI Specification

📄 **[docs/openapi.yaml](docs/openapi.yaml)** - Complete REST API specification with all endpoints, request/response schemas, and validation rules.

The specification is auto-generated from the code using springdoc-openapi. Use this file for:
- API documentation and reference
- Client code generation (openapi-generator)
- Testing tools integration (Postman, Insomnia)

When the application is running, you can also access:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/api-docs.yaml`

To regenerate the spec file after API changes:
```bash
./scripts/update-openapi-spec.sh
# Or: cd backend && mvn verify -Pgenerate-openapi -DskipTests
```

### Main Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/chat/session` | Get or create active chat session |
| POST | `/api/v1/chat/session/{id}/messages` | Send message to chat coach |
| POST | `/api/v1/chat/session/{id}/messages/stream` | Stream chat response (SSE) |
| GET | `/api/v1/users/me` | Get current user |
| PUT | `/api/v1/users/me` | Update current user |
| GET | `/api/v1/goals` | List learning goals |
| POST | `/api/v1/goals` | Create a new goal |
| GET | `/api/v1/goals/daily/active` | Get active daily goals |
| PATCH | `/api/v1/goals/{id}/progress` | Update goal progress |
| POST | `/api/v1/content/lessons/generate` | Generate a lesson |
| POST | `/api/v1/content/vocabulary/generate` | Generate vocabulary |
| POST | `/api/v1/content/flashcards/generate` | Generate flashcards |
| POST | `/api/v1/content/scenarios/generate` | Generate roleplay scenario |
| POST | `/api/v1/exercises/generate` | Generate exercises (unified API with `type` parameter) |
| POST | `/api/v1/exercises/evaluate` | Evaluate user response |

See full API documentation at Swagger UI when running.

## 🐳 Docker Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down

# Rebuild after changes
docker-compose up -d --build

# Reset database
docker-compose down -v
docker-compose up -d
```

## 📝 Environment Variables Reference

| Variable | Default | Description                            |
|----------|---------|----------------------------------------|
| `NATIVE_LANGUAGE` | `de` | (Default user) native language     |
| `TARGET_LANGUAGE` | `fr` | (Default user) language to learn       |
| `LLM_PROVIDER` | `openai` | AI provider (openai, anthropic, ollama) |
| `LLM_API_KEY` | - | API key for LLM provider               |
| `LLM_MODEL` | `gpt-4-turbo-preview` | Model name                             |
| `DATABASE_URL` | `jdbc:postgresql://postgres:5432/language_learning` | Database URL                           |
| `SERVER_PORT` | `8080` | Backend server port                    |

## 🤝 Contributing

Contributions are welcome! Please read the [Development Workflow](.clinerules/development-workflow.md) guidelines.

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

This means:
- ✅ You can use, modify, and distribute this software freely
- ✅ You must keep the source code open if you distribute it
- ✅ Any derivative works must also be licensed under GPL-3.0
- ✅ Changes must be documented

---

**Happy Learning! 🎓**

*Created with ❤️ using Spring Boot, Spring AI, and vanilla JavaScript*