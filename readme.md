### Running Rust agents from executable
To run Rust agents locally from executable, you first need to build them. You can do this by cding into /rust/ and running cargo build with the release flag:

```shell
cd rust
cargo build --release
```

---

## CI/CD Automation

This repository uses a GitHub Actions workflow (`.github/workflows/build-and-push.yml`) that automatically builds, pushes, and publishes agents when changes are merged to `main`.

### Known issues

* Pushing to the marketplace is skipped when it shouldn't be if you re-run the task,

### How it works

1. **Change detection** — Each agent is evaluated independently using [dorny/paths-filter](https://github.com/dorny/paths-filter). Only agents whose files actually changed are built. For Koog agents, changes to `koog/shared/` also trigger a build since it is a shared dependency.

2. **Versioning** — [release-please](https://github.com/googleapis/release-please) runs in manifest mode to manage independent semantic versions for each agent. When [Conventional Commits](https://www.conventionalcommits.org/) are pushed to `main`, release-please opens (or updates) a release PR per agent. Merging that PR creates a GitHub Release and bumps the version in both `Cargo.toml` and `coral-agent.toml`.

3. **Docker build & push** — Rust agents are built into Docker images using their respective Dockerfiles (`rust/agent-<name>.Dockerfile`). Images are tagged with both the version from `coral-agent.toml` and `latest`, then pushed to Docker Hub.

4. **Marketplace publishing** — When release-please creates a new release for an agent, its `coral-agent.toml` manifest is POSTed to the Coral Marketplace API. Publishing only happens for newly released versions, not on every push.

### Commit conventions

This repo follows [Conventional Commits](https://www.conventionalcommits.org/) so that release-please can determine version bumps automatically:

- **`fix: ...`** → patch bump (e.g. `0.0.1` → `0.0.2`)
- **`feat: ...`** → minor bump (e.g. `0.1.0` → `0.2.0`)
- **`feat!: ...`** or a `BREAKING CHANGE` footer → major bump (e.g. `0.2.0` → `1.0.0`)

Because release-please operates per-path, a commit that only touches files under `rust/agent-firecrawl/` will only affect that agent's version. If a single commit touches multiple agents, each affected agent gets its own release PR entry.

### Release-please configuration

Two files at the repo root control release-please:

- **`release-please-config.json`** — Declares each agent as a package, its release type (`rust` or `java`), and extra files to update (e.g. `coral-agent.toml`).
- **`.release-please-manifest.json`** — Tracks the current released version of each agent. This file is updated automatically by release-please; do not edit it manually.

### Adding a new agent

1. Create the agent directory under `rust/` or `koog/` following the existing structure. Include a `coral-agent.toml` manifest.
2. For Rust agents, add a Dockerfile at `rust/agent-<name>.Dockerfile`.
3. Add an entry for the new agent in `release-please-config.json` under `packages`.
4. Add an initial version entry (e.g. `"0.0.1"`) in `.release-please-manifest.json`.
5. Add the agent to the `matrix` in `.github/workflows/build-and-push.yml`.