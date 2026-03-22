# Development Workflow Guidelines

## Getting Started

### Prerequisites
- Java 21+ (LTS)
- Maven 3.9+
- Docker and Docker Compose
- Node.js 20+ (for frontend tooling, if needed)
- PostgreSQL 16+ (or via Docker)

### Initial Setup
```bash
# Clone and navigate
git clone <repository-url>
cd language-learning

# Start infrastructure (PostgreSQL)
docker-compose -f docker/docker-compose.yml up -d postgres

# Build all modules
cd backend && mvn clean install

# Run the application
cd backend && mvn -pl api-module spring-boot:run
```

## Development Process

### Branch Strategy
- `main` - Production-ready code
- `develop` - Integration branch
- `feature/*` - New features
- `fix/*` - Bug fixes
- `refactor/*` - Code improvements

### Commit Messages
Follow conventional commits:
```
type(scope): description

[optional body]

[optional footer]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
```
feat(learning-service): add daily goal tracking
fix(llm-module): handle timeout in OpenAI provider
docs(readme): update deployment instructions
```

## Testing Strategy

### Unit Tests
- Located in `src/test/java` of each module
- Use JUnit 5 + Mockito
- Naming: `{ClassName}Test.java`
- Coverage target: 80%+

### Integration Tests
- Suffix: `*IT.java`
- Use `@SpringBootTest` for full context
- Use WireMock for external API mocking
- Use Testcontainers for PostgreSQL

### Test Categories
```java
@Tag("unit")        // Fast, isolated tests
@Tag("integration") // Spring context tests
@Tag("e2e")         // End-to-end tests
```

### Running Tests
```bash
# All tests
mvn test

# Unit tests only
mvn test -Dgroups=unit

# Integration tests
mvn verify -Dgroups=integration

# Skip tests
mvn install -DskipTests
```

## Module Development

### Adding a New Module
1. Create directory under `backend/`
2. Add `pom.xml` with parent reference
3. Register in parent `pom.xml` modules section
4. Follow package naming: `dev.languagelearning.{module}`

### Package Structure per Module
```
src/main/java/dev/languagelearning/{module}/
├── config/           # Configuration classes
├── domain/           # Domain entities (if module-specific)
├── repository/       # Data access
├── service/          # Business logic
│   └── impl/        # Service implementations
├── client/           # External API clients
├── exception/        # Module-specific exceptions
└── util/             # Utility classes
```

## AI Provider Development

### Adding New AI Provider
1. Implement provider interface in `llm-module`
2. Add configuration properties
3. Register in provider factory
4. Write integration tests with WireMock
5. Document in provider-specific markdown

### Prompt Management
- Store prompts in `resources/prompts/`
- Use `.txt` or `.md` files
- Support variable interpolation: `{variable}`
- Version prompts with semantic versioning

## Content Management

### Markdown File Guidelines
- Use front-matter for metadata:
```markdown
---
id: lesson-001
type: lesson
language-pair: de-fr
level: A1
created: 2024-01-15
---

# Lesson Title
...
```

### Content Templates
Templates for generated content are managed by the content-service module and stored as resources or generated dynamically by the LLM.

## Database Management

### Migrations
- Use Flyway for schema migrations
- Location: `backend/api-module/src/main/resources/db/migration`
- Naming: `V{version}__{description}.sql`
- Never modify existing migrations

### Entity Guidelines
- All entities extend `BaseEntity` (id, createdAt, updatedAt)
- Use `@Version` for optimistic locking
- Soft delete with `deletedAt` field where appropriate

## Frontend Development

### File Organization
```
frontend/
├── index.html
├── css/
│   ├── main.css
│   ├── components/
│   └── themes/
├── js/
│   ├── app.js
│   ├── api/          # API client modules
│   ├── components/   # UI components
│   ├── services/     # Business logic
│   └── utils/        # Utilities
└── assets/
    ├── images/
    └── audio/
```

### JavaScript Guidelines
- ES6+ modules (no transpilation required for modern browsers)
- Use `fetch` for API calls
- Prefer `async/await` over `.then()`
- Document public functions with JSDoc

## Docker Development

### Building Images
```bash
# Build backend image
docker build -t language-learning-backend:latest -f docker/Dockerfile.backend .

# Build frontend image
docker build -t language-learning-frontend:latest -f docker/Dockerfile.frontend .
```

### Local Development
```bash
# Start all services
docker-compose -f docker/docker-compose.yml up

# Start specific service
docker-compose -f docker/docker-compose.yml up backend

# View logs
docker-compose -f docker/docker-compose.yml logs -f backend
```

## Debugging

### Backend Debugging
- Remote debug port: 5005
- Enable with: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
- Use IDE remote debug configuration

### Logging Levels
Configure in `application.yml`:
```yaml
logging:
  level:
    dev.languagelearning: DEBUG
    org.springframework.ai: DEBUG
```

## Code Review Checklist

### Before Submitting PR
- [ ] All tests pass
- [ ] No compiler warnings
- [ ] Code follows style guidelines
- [ ] JavaDoc on public APIs
- [ ] Meaningful commit messages
- [ ] No hardcoded credentials
- [ ] Error handling is appropriate
- [ ] Logging is adequate

### PR Description Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Refactoring
- [ ] Documentation

## Testing
How was this tested?

## Screenshots (if applicable)

## Checklist
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No breaking changes
```

## Performance Considerations

### API Response Time Targets
- Simple reads: < 100ms
- Complex queries: < 500ms
- AI-generated content: < 5s (with streaming)
- Audio processing: < 2s

### Caching Strategy
- Use Redis for session data
- Cache AI prompts and templates
- Cache user preferences locally
- Invalidate on content updates

## Security Guidelines

### Credential Management
- Never commit credentials
- Use environment variables
- Use Docker secrets in production
- Rotate API keys regularly

### API Security
- Rate limiting on all endpoints
- CORS configuration
- Input validation
- SQL injection prevention (use prepared statements)