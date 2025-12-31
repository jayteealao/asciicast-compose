# GitHub Actions Workflows

This directory contains CI/CD workflows for the asciicast-compose project.

## Workflows Overview

### 🔨 CI (`ci.yml`)

**Triggers**: Push to `main`/`develop`, Pull Requests

**Jobs**:
- **build-and-test**: Compile all modules and run unit tests
  - Builds: vt-api, formats, streaming-alis, player-core, renderer-compose, sample-app
  - Runs: All JUnit tests (currently 24+ tests)
  - Uploads: Test results and build artifacts

- **lint**: Code style validation
  - Runs: ktlint checks on all Kotlin code
  - Uploads: Lint reports on failure

- **build-rust** (TODO - commented out)
  - Will build vt-avt Rust library when implemented
  - Targets: armeabi-v7a, arm64-v8a, x86_64

**Artifacts**:
- Test results (XML + HTML reports) - 7 days retention
- Build artifacts (JARs, AARs) - 7 days retention
- Lint reports - 7 days retention

---

### 🔍 PR Checks (`pr-checks.yml`)

**Triggers**: Pull Request opened/updated

**Jobs**:
- **validate**: Comprehensive PR validation
  - Checks for merge conflicts
  - Builds all modules
  - Runs tests with coverage
  - ktlint checks
  - Lists TODO/FIXME in modified files
  - Comments test results on PR

- **size-check**: APK/AAR size tracking
  - Builds release artifacts
  - Reports artifact sizes in PR summary
  - Helps track binary size bloat

**Benefits**:
- Early feedback on PR quality
- Prevents broken code from merging
- Size regression detection

---

### 📦 Publish (`publish.yml`)

**Triggers**:
- Release published (automatic)
- Manual workflow dispatch

**Targets**:
- **Local Maven**: For local testing (`publishToMavenLocal`)
- **GitHub Packages**: For internal/preview releases
- **Maven Central** (TODO): For public releases

**Steps**:
1. Build and test all modules
2. Publish based on selected target
3. Create GitHub Release assets
4. Upload artifacts to release

**Manual Usage**:
```bash
# Via GitHub UI: Actions → Publish → Run workflow
# Select publish target: local, github, or maven-central
```

**Secrets Required** (for Maven Central):
- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_KEY` (GPG private key)
- `SIGNING_PASSWORD` (GPG key passphrase)

---

### 🚀 Release (`release.yml`)

**Triggers**: Git tag push matching `v*.*.*` (e.g., `v0.1.0`)

**Process**:
1. Extract version from tag
2. Build release artifacts (AARs, JARs)
3. Run full test suite
4. Generate changelog from commits
5. Create GitHub Release with:
   - Changelog
   - Installation instructions
   - SHA256 checksums
   - All library artifacts

**Creating a Release**:
```bash
# Tag the release
git tag -a v0.1.0 -m "Release version 0.1.0"
git push origin v0.1.0

# Workflow automatically creates GitHub Release
```

**Release Artifacts**:
- `asciicast-vt-api-*.jar`
- `asciicast-formats-*.jar`
- `asciicast-streaming-alis-*.jar`
- `asciicast-player-core-*.jar`
- `asciicast-renderer-compose-*.aar`
- `asciicast-vt-avt-*.aar`
- `*.sha256` checksum files

---

### 📊 Dependencies (`dependencies.yml`)

**Triggers**:
- Weekly schedule (Monday 9 AM UTC)
- Manual workflow dispatch

**Purpose**:
- Checks for dependency updates
- Creates GitHub issue if updates found
- Helps maintain security and compatibility

**TODO**:
- Add `gradle-versions-plugin` to enable automatic checking
- Configure update notifications

---

## Workflow Status

Add these badges to README.md:

```markdown
![CI](https://github.com/jayteealao/asciicast-compose/workflows/CI/badge.svg)
![Release](https://github.com/jayteealao/asciicast-compose/workflows/Release/badge.svg)
```

## Local Testing

### Test workflows locally with `act`:

```bash
# Install act: https://github.com/nektos/act
brew install act  # macOS
# or
sudo apt install act  # Linux

# Run CI workflow
act push

# Run PR checks
act pull_request

# Run specific job
act -j build-and-test
```

## Secrets Configuration

### GitHub Repository Settings → Secrets and variables → Actions

**Required for Publishing**:
1. `MAVEN_CENTRAL_USERNAME` - Sonatype OSSRH username
2. `MAVEN_CENTRAL_PASSWORD` - Sonatype OSSRH password
3. `SIGNING_KEY` - GPG private key (base64 encoded)
4. `SIGNING_PASSWORD` - GPG key passphrase

**Generating GPG Key**:
```bash
# Generate key
gpg --gen-key

# Export private key (base64 encoded)
gpg --export-secret-keys YOUR_KEY_ID | base64 > signing_key.txt

# Add to GitHub Secrets (paste content of signing_key.txt)
```

## Troubleshooting

### CI Fails on Build
- Check Java version (must be 17)
- Verify Gradle wrapper is executable
- Check for merge conflicts

### ktlint Failures
```bash
# Auto-fix locally
./gradlew ktlintFormat

# Check what will fail
./gradlew ktlintCheck
```

### Publish Fails
- Verify secrets are configured
- Check Maven Central credentials
- Ensure GPG key is valid

### Release Not Created
- Verify tag format: `v*.*.*` (e.g., `v0.1.0`)
- Check workflow permissions (needs `contents: write`)
- Review workflow logs

## Future Enhancements

- [ ] Code coverage reporting (JaCoCo)
- [ ] Dependency vulnerability scanning (Dependabot)
- [ ] Automated changelog generation
- [ ] Docker image builds (for server-side rendering)
- [ ] Performance benchmarking
- [ ] Screenshot/snapshot testing
- [ ] Multi-platform builds (iOS, Desktop)
