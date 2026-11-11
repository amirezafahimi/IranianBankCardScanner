# Publishing

How a release of CardScanner reaches Maven Central. Everything here is a one-time setup except the
last section.

Coordinates: `io.github.amirezafahimi:cardscanner`.

---

## 1. Claim the namespace

Maven Central deployments go through the [Central Portal](https://central.sonatype.com) — the old
OSSRH / `oss.sonatype.org` flow is retired, so ignore any guide that mentions it.

1. Sign in to <https://central.sonatype.com> with the GitHub account that owns this repository.
2. Open **Namespaces → Add Namespace** and enter `io.github.amirezafahimi`.
3. Because the namespace mirrors a GitHub username, verification is automatic for a GitHub login —
   the portal may ask you to create a short-lived public repository with a generated name to prove
   ownership. Delete it once the namespace shows **Verified**.

The namespace is why the group id is `io.github.amirezafahimi` and not the `com.github.…` form
JitPack serves: `com.github.*` cannot be verified on the portal, since nobody owns `github.com` but
GitHub.

## 2. Generate a user token

Still on the portal: **your avatar → View Account → Generate User Token**. You get a username and a
password that are *not* your login credentials. Store them where Gradle finds them — in
`~/.gradle/gradle.properties`, never in this repository:

```properties
mavenCentralUsername=<token username>
mavenCentralPassword=<token password>
```

## 3. Create a signing key

Central requires a detached PGP signature for every published file, and the public key must be
resolvable from a public keyserver.

```bash
# Generate a key. Pick RSA 4096, no expiry, and use the email of your GitHub account.
gpg --full-generate-key

# Note the long key id.
gpg --list-secret-keys --keyid-format=long

# Publish the public key — Central checks these three.
gpg --keyserver keys.openpgp.org      --send-keys <KEY_ID>
gpg --keyserver keyserver.ubuntu.com  --send-keys <KEY_ID>
gpg --keyserver pgp.mit.edu           --send-keys <KEY_ID>
```

Keys uploaded to `keys.openpgp.org` only carry the identity after you confirm the verification
email it sends you.

Then add the key to `~/.gradle/gradle.properties`, in the in-memory form, which is the same form CI
uses:

```properties
signingInMemoryKey=<paste the ASCII-armored private key, newlines replaced by \n>
signingInMemoryKeyPassword=<key passphrase>
```

Export it with:

```bash
gpg --armor --export-secret-keys <KEY_ID> | awk 'NR>2 && !/^-----/ {printf "%s", $0}'
```

**Back the key up somewhere safe.** Losing it does not break published versions, but you cannot sign
a new release without it, and a new key has to be re-verified.

## 4. Add the CI secrets

`.github/workflows/release.yml` publishes on a version tag, so the key never has to leave GitHub.
Under **Settings → Secrets and variables → Actions** add:

| Secret | Value |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | token username from step 2 |
| `MAVEN_CENTRAL_PASSWORD` | token password from step 2 |
| `SIGNING_KEY` | full ASCII-armored private key, `-----BEGIN` line included |
| `SIGNING_PASSWORD` | key passphrase |

---

## Cutting a release

1. Bump `version` in `cardscanner/build.gradle.kts` and add a `CHANGELOG.md` entry. Central is
   immutable: a published version can never be replaced, only superseded.
2. Commit, then tag and push:

   ```bash
   git tag v1.0.0 && git push origin v1.0.0
   ```

3. The workflow runs the tests, builds the AAR, sources jar and Dokka javadoc jar, signs everything
   and uploads it as a deployment.
4. Open <https://central.sonatype.com/publishing/deployments>. The deployment sits at **VALIDATED**.
   Check the file list, then press **Publish**.
5. It is on Maven Central within ~15 minutes and searchable on `central.sonatype.com` within a few
   hours. Switch `publishToMavenCentral()` to `publishToMavenCentral(automaticRelease = true)` to
   skip step 4 on later releases.

### Publishing from your machine instead

```bash
./gradlew :cardscanner:publishToMavenCentral --no-configuration-cache
```

Needs steps 2 and 3 done locally, and direct network access to `central.sonatype.com`.

### Checking before you publish

```bash
./gradlew :cardscanner:publishToMavenLocal   # inspect ~/.m2/repository/io/github/amirezafahimi
```

A deployment that fails validation names the reason — a missing javadoc jar, an unsigned file, an
unverifiable key, or a POM missing `name`, `description`, `url`, a license, a developer or `scm`.
All of those are configured in `cardscanner/build.gradle.kts`.
