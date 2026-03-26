# Shell Environment Guidelines

## WSL Preference (Windows)

When running on Windows, **prefer WSL (Windows Subsystem for Linux)** for executing commands, especially:

- Maven builds (`mvn clean install`, `mvn test`, etc.)
- Shell scripts (`.sh` files)
- Commands requiring Unix utilities (`head`, `tail`, `grep`, `sed`, `awk`, `find`, `xargs`)
- Git operations
- Docker commands

### Command Execution Pattern

For commands that should run in WSL, use the `wsl` prefix:

```bash
# Maven commands
wsl -e bash -c "cd /mnt/c/Users/James/IdeaProjects/language-learning/backend && mvn clean install"

# Running shell scripts
wsl -e bash -c "cd /mnt/c/Users/James/IdeaProjects/language-learning && ./scripts/update-openapi-spec.sh"

# Using Linux utilities
wsl -e bash -c "cd /mnt/c/Users/James/IdeaProjects/language-learning && find . -name '*.java' | head -20"
```

### Path Translation

- Windows path: `C:/Users/James/IdeaProjects/language-learning`
- WSL path: `/mnt/c/Users/James/IdeaProjects/language-learning`

### When to Use Native Windows CMD

- Running IDE-specific commands
- Windows-native tools without WSL equivalents
- Quick file operations where WSL overhead isn't justified

### Available Linux Utilities in WSL

Useful commands for development:

| Command | Description | Example |
|---------|-------------|---------|
| `head` | View first N lines | `head -n 50 file.txt` |
| `tail` | View last N lines | `tail -n 50 file.txt` |
| `tail -f` | Follow log output | `tail -f logfile.log` |
| `grep` | Search patterns | `grep -r "pattern" --include="*.java"` |
| `find` | Find files | `find . -name "*.md" -type f` |
| `wc` | Count lines/words | `wc -l file.txt` |
| `diff` | Compare files | `diff file1 file2` |
| `sed` | Stream editing | `sed -i 's/old/new/g' file.txt` |
| `awk` | Text processing | `awk '{print $1}' file.txt` |
| `xargs` | Build commands | `find . -name "*.tmp" \| xargs rm` |

### Maven in WSL

When executing Maven commands via WSL:

```bash
# Build all modules
wsl -e bash -c "cd /mnt/c/Users/James/IdeaProjects/language-learning/backend && mvn clean install"

# Run tests
wsl -e bash -c "cd /mnt/c/Users/James/IdeaProjects/language-learning/backend && mvn test"

# Run specific module
wsl -e bash -c "cd /mnt/c/Users/James/IdeaProjects/language-learning/backend && mvn -pl api-module spring-boot:run"

# Skip tests
wsl -e bash -c "cd /mnt/c/Users/James/IdeaProjects/language-learning/backend && mvn install -DskipTests"
```

### Environment Variables

If environment variables are needed, pass them in the bash command:

```bash
wsl -e bash -c "export JAVA_HOME=/usr/lib/jvm/java-21-openjdk && cd /mnt/c/Users/James/IdeaProjects/language-learning/backend && mvn clean install"
```

Or source a profile:

```bash
wsl -e bash -c "source ~/.bashrc && cd /mnt/c/Users/James/IdeaProjects/language-learning/backend && mvn clean install"