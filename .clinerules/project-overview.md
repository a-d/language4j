# Language Learning Meta-Project Overview

## Project Vision
A deployable, AI-powered language learning platform that adapts to user configuration. Users specify their native language, target language, and AI provider credentials to receive a personalized learning experience.

## Technology Stack
- **Backend**: Java 21+ with Spring Boot 3.x and Spring AI
- **Frontend**: Vanilla HTML/CSS/JavaScript (no frameworks)
- **Database**: PostgreSQL for structured data (users, progress, goals)
- **Content Storage**: Markdown files for LLM-generated content (lessons, scenarios, vocabulary)
- **AI Integration**: Multi-provider support via Spring AI (OpenAI, Anthropic, Ollama, etc.)
- **Deployment**: Docker Compose for local deployment
- **MCP Integration**: Model Context Protocol server for tool-assisted learning

## Core Modules
1. **config-module**: User configuration, AI provider settings
2. **core-module**: Domain models, entities, shared utilities
3. **llm-module**: Spring AI abstraction for multi-provider support
4. **api-module**: Spring Boot REST API
5. **content-service**: Learning content generation and management
6. **learning-service**: Lesson planning, progress tracking, skill assessment
7. **speech-service**: Audio transcription and text-to-speech integration
8. **image-service**: Learning card image generation
9. **frontend**: Vanilla JS web application
10. **mcp-server**: MCP server for tool integration

## Key Features
- Personalized learning plan generation via LLM
- Goal setting (daily, weekly, monthly, yearly)
- Skill level assessment and adaptive difficulty
- Learning cards with generated images
- Text completion exercises
- Listen-and-repeat audio exercises
- Drag-and-drop sentence construction
- Roleplay scenarios
- Progress tracking and analytics

## Directory Structure
```
language-learning/
├── .clinerules/              # Project-specific Cline rules
├── backend/                  # Java Spring Boot backend
│   ├── config-module/
│   ├── core-module/
│   ├── llm-module/
│   ├── api-module/
│   ├── content-service/
│   ├── learning-service/
│   ├── speech-service/
│   └── image-service/
├── frontend/                 # Vanilla JS web application
├── docker/                  # Docker configurations (production)
├── local-deployment/        # Local AI deployment (Piper TTS, Whisper, SD)
├── docs/                    # Documentation
└── README.md                # Project readme
