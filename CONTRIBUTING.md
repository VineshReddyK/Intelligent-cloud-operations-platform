# Contributing to Intelligent Cloud Operations Platform

Thanks for your interest in contributing!

## Getting Started

1. Fork the repository and clone your fork
2. Ensure you have Java 21, Maven 3.9+, Docker, and Docker Compose installed
3. Start the local stack: `docker-compose up -d`
4. Run tests: `mvn test` in the service you're modifying

## Branching

- Branch from `main`
- Use descriptive branch names: `feat/anomaly-threshold`, `fix/kafka-consumer-lag`, `chore/upgrade-spring-boot`

## Making Changes

- Keep changes focused — one feature or fix per PR
- Add or update tests for any logic changes
- Run `mvn verify` before opening a PR to ensure the full build passes
- Check that the CI pipeline passes (GitHub Actions will run automatically)

## Pull Request Guidelines

- Write a clear PR title and description explaining the **why**, not just the what
- Link any related issues with `Closes #<issue>`
- PRs require at least one review before merging

## Code Style

- Follow standard Java conventions (Google Java Style Guide)
- Use meaningful variable and method names
- Avoid unnecessary comments — write self-documenting code

## Reporting Issues

Open a GitHub Issue with:
- A clear title and description
- Steps to reproduce (for bugs)
- Expected vs actual behavior
- Environment details (Java version, OS, Docker version)

## Questions

Reach out via [email](mailto:vineshreddyy.k@gmail.com) or open a Discussion on GitHub.
