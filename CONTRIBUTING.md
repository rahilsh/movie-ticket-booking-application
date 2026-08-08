# Contributing

Contributions are welcome through GitHub issues and pull requests.

## Development setup

Requirements:

- Java 21
- Maven 3.9 or newer
- Docker, required by PostgreSQL integration tests

Run fast unit tests:

```bash
mvn clean test
```

Run the complete suite and coverage gates:

```bash
mvn clean verify
```

The complete suite starts PostgreSQL with Testcontainers. API integration tests start the application on a random port and call it through HTTP.

## Pull requests

- Open an issue first for substantial behavior or API changes.
- Keep changes focused and add tests for changed behavior.
- Run `mvn clean verify` before submitting.
- Update documentation when configuration or API behavior changes.
- Use Conventional Commits for commit messages.
- Do not commit credentials, personal data, generated build output, or IDE files.

By participating, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
