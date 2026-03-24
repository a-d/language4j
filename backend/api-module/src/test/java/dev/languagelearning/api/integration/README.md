# Integration Tests

This directory contains integration tests for the Language Learning Platform API that use Testcontainers with PostgreSQL.

## Overview

Integration tests in this package test the full Spring Boot application with a real PostgreSQL database, ensuring that:
- REST API endpoints work correctly end-to-end
- Database operations (CRUD) function properly with Flyway migrations
- Service layer integrations work as expected

External AI services (ContentGenerationService, SpeechService, ImageService) are mocked to keep tests fast and deterministic.

## Prerequisites

**Docker Desktop must be running** for these tests to work. Testcontainers automatically:
1. Pulls the `postgres:16-alpine` Docker image
2. Starts a PostgreSQL container
3. Runs Flyway migrations
4. Tears down the container after tests complete

## Test Structure

```
integration/
├── BaseIntegrationTest.java    # Abstract base class with Testcontainers setup
├── UserControllerIT.java       # User API integration tests
├── GoalControllerIT.java       # Learning goals API integration tests
├── ExerciseControllerIT.java   # Exercise API integration tests (with mocked AI)
└── README.md                   # This file
```

## Running Integration Tests

### Run integration tests only:
```bash
# From project root
mvn -f backend/api-module/pom.xml failsafe:integration-test

# Or from backend directory
cd backend && mvn verify -pl api-module -am
```

### Run unit tests only (excludes integration tests):
```bash
mvn -f backend/api-module/pom.xml test
```

### Run all tests:
```bash
mvn -f backend/pom.xml verify
```

## Test Configuration

- **Profile**: `integration` (activated via `@ActiveProfiles("integration")`)
- **Configuration**: `src/test/resources/application-integration.yml`
- **Database**: PostgreSQL 16 Alpine via Testcontainers (isolated, ephemeral)

## Writing New Integration Tests

1. Extend `BaseIntegrationTest`:
   ```java
   class MyNewControllerIT extends BaseIntegrationTest {
       // Your tests here
   }
   ```

2. Use the injected `mockMvc` for HTTP requests:
   ```java
   mockMvc.perform(get("/api/v1/endpoint"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.field").value("expected"));
   ```

3. Mock AI services when needed:
   ```java
   when(contentGenerationService.generateLesson(any()))
           .thenReturn("mocked content");
   ```

## Troubleshooting

### "Could not find a valid Docker environment"
- Ensure Docker Desktop is running
- On Windows, ensure WSL2 is properly configured
- Check Docker Desktop settings for API access

### Tests are slow
- First run downloads PostgreSQL image (~50MB)
- Subsequent runs reuse the image
- Consider using `@DirtiesContext(classMode = AFTER_CLASS)` for test isolation

### Database state issues
- Each test class shares the same container by default
- Use `@Sql` annotations for data setup/cleanup if needed
- Consider `@Transactional` and `@Rollback` for automatic cleanup