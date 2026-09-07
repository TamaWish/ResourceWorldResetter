# Contributing to ResourceWorldResetter

Thank you for helping improve ResourceWorldResetter. Contributions to code, tests, documentation, and translations are welcome.

## Code of conduct

This repository does not currently include a `CODE_OF_CONDUCT.md`. Be respectful, constructive, and patient in project discussions.

## Questions and support

Read the [README](README.md), [operator wiki](https://tamawish.github.io/ResourceWorldResetter/wiki.html), and [operations guide](docs/public/OPERATIONS_AND_MIGRATION.md) first. For usage questions, use the project's [Discord](https://discord.gg/kbKZzxDETU) or GitHub Discussions if it is available.

## Bug reports

Search existing issues before opening a new one. Include:

- The exact RWR JAR and version.
- Server software, Minecraft version, Java version, and world-provider version.
- Reproduction steps and expected versus actual behavior.
- Relevant logs and configuration with secrets removed.
- Whether the problem occurs on a clean test server.

Do not attach complete production server archives or disclose player data.

## Security vulnerabilities

> [!CAUTION]
> Never report a suspected vulnerability in a public issue, discussion, or Discord channel.

Use GitHub's private vulnerability reporting feature if it is available for the repository. Otherwise, contact the maintainer privately and ask for a secure reporting channel. Share only the minimum proof needed until a private channel is established.

## Feature requests

Explain the server-owner or player problem, the desired outcome, and how it fits RWR's guarded reset workflow. Avoid combining unrelated requests. This repository does not currently define feature-request labels or response-time commitments.

## Your first contribution

Small documentation corrections, focused regression tests, and locale improvements are good starting points. The repository does not maintain a guaranteed list of “good first issue” labels, so choose an open issue you understand or describe your proposed change before starting a large implementation.

For translations, preserve every YAML key, MiniMessage tag, and placeholder such as `<world>` or `<permission>`.

## Development setup

You need:

- Git.
- JDK 25 for the complete reactor and Paper/Folia dependency graph.
- Maven 3.9 or newer.
- Network access to the Maven repositories declared in `pom.xml`.

The Java compiler target is 21, but the full build and CI run on JDK 25.

```bash
git clone https://github.com/TamaWish/ResourceWorldResetter.git
cd ResourceWorldResetter
mvn clean verify
```

## Module responsibilities

| Module | Responsibility |
| --- | --- |
| `rwr-core` | Configuration, scheduling, reset coordination, history, and platform-independent teleport policy. |
| `rwr-bukkit-api-adapter` | Public snapshot mapping, Bukkit service registration, and API event publication. |
| `RWR-Spigot` | Multiverse integration, Bukkit scheduling, commands, GUIs, and localization. |
| `RWR-Paper-Folia` | Worlds integration, Folia scheduling, commands, GUIs, and localization. |

Keep provider-specific types out of `rwr-core` and the public API. Keep equivalent behavior aligned across both platform modules where their APIs allow it.

## Tests and formatting

Run the full CI-equivalent verification before opening a pull request:

```bash
mvn clean verify
```

Useful focused commands:

```bash
mvn -pl rwr-core test
mvn -pl RWR-Spigot,RWR-Paper-Folia -am clean package
mvn spotless:check
```

Add or update regression tests for changed behavior. Reset changes should cover failure safety, lock release, cancellation, and provider exceptions where applicable. Scheduling changes should cover reloads, duplicate requests, retries, and recovery. Paper/Folia work must keep global tasks on the global scheduler and entity work on entity schedulers.

> [!IMPORTANT]
> Automated tests do not replace a supervised test of the exact candidate JAR on the affected server platform.

## Branch and pull request workflow

1. Create a focused branch from the current `main`.
2. Make one coherent change and include its tests and documentation.
3. Run `mvn clean verify`.
4. Test the affected platform JAR on a disposable server when runtime behavior changes.
5. Open a pull request against `main`.
6. Describe the problem, solution, user-visible impact, and validation performed.

Keep pull requests focused. Do not commit generated `target/` directories, server worlds, local plugin data, IDE files, or logs.

The repository does not define a mandatory branch naming scheme, commit-message convention, CLA, or signed-off-by requirement.

## Documentation

Update [README.md](README.md) for installation, requirements, commands, or public behavior changes. Update the operator docs in `website/src/content/docs/` and relevant files under `docs/public/` when operations or migration behavior changes. Keep the Markdown and BBCode marketplace descriptions in [MARKETPLACE.md](MARKETPLACE.md) synchronized.

## License

By contributing, you agree that your contribution is distributed under the repository's [BSD 3-Clause License](LICENSE). No separate CLA is documented.
