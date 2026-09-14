# Releasing

SQLiter publishes `co.touchlab:sqliter-driver` to Maven Central. Releases are cut by pushing a release
tag.

## Conventions

- Tags are the bare version, no `v` prefix — `1.4.0`, not `v1.4.0`.
- The tag and `VERSION_NAME` must be identical.
- `main` sits on a `-SNAPSHOT` between releases.

## Choosing the version

Standard semver against the published API surface:

- **Patch** — bug fixes, no public declarations added or changed.
- **Minor** — public declarations added.
- **Major** — public declarations removed or changed incompatibly.

## Releasing

Steps 1–6 are local. Nothing is published until you push the tag in step 7.

1. **Confirm the working tree is clean and `main` is current.**
2. **Set the release version** in `gradle.properties`:

   ```
   VERSION_NAME=X.Y.Z
   ```

3. **Commit.**

   ```bash
   git commit -am "Prepare version X.Y.Z"
   ```

4. **Tag that commit.**

   ```bash
   git tag -a X.Y.Z -m "Version X.Y.Z"
   ```

5. **Set the next development version.** Bump the patch and re-add the suffix in
   `gradle.properties`:

   ```
   VERSION_NAME=<next patch>-SNAPSHOT
   ```

   So releasing `1.4.1` leaves `main` on `VERSION_NAME=1.4.2-SNAPSHOT`.

6. **Commit.**

   ```bash
   git commit -am "Prepare next development version"
   ```

7. **Push the branch first, then the tag.** Pushing the tag triggers the release, so push it last:

   ```bash
   git push origin main
   git push origin X.Y.Z
   ```

   Pushing requires bypassing main branch protections, if you're not set up to do this, you probably shouldn't be 
   publishing anyway

