# Release Process

## Releasing a New Version

`splunk-otel-android` is released via a private Splunk GitLab installation.

Follow these steps to perform a release:

1. **Prepare the release**
   - Ensure that all required changes are merged into the `develop` branch.
   - This includes updating the versions of upstream OpenTelemetry (OTel) libraries.

2. **Create a release branch**
   - Create a new branch named `release/X.Y.Z`, where `X.Y.Z` is the release version.

3. **Update the version**
   - In `Configurations.kt`, update the `sdkVersionName` to the release version.
   - Commit the change to the `release/X.Y.Z` branch.

4. **Update documentation**
   - Update relevant files (`CHANGELOG.md`, `README.md`, etc.) with release notes.
   - Commit the changes to the `release/X.Y.Z` branch.

5. **Update the issue template**
   - In the [bug report issue template](https://github.com/signalfx/splunk-otel-android/blob/develop/.github/ISSUE_TEMPLATE/bug.yml), ensure that the newly released version is **added to the top of the list** in the `Agent Version` section.
   - Commit this change to the `release/X.Y.Z` branch.

6. **Create a release PR**
   - Open a pull request from `release/X.Y.Z` → `main`.
   - Wait for the CI pipeline to complete successfully.

7. **Tag the release**
   - If CI passes, run the `script/tag-release.sh` script to create and push a **signed release tag**.
   - Note: The script assumes the remote is named `origin`. If your remote has a different name, you may need to push the tag manually.

8. **Publish the release**
   - Wait for GitLab to run the release job.
   - If successful, it will automatically close and release the “staging” repository.
   - The build will then be published to Sonatype and appear in Maven within a few hours (up to a day).

9. **Merge to main**
   - Once the release is verified, merge the PR into the `main` branch.

10. **Create GitHub Release**
    - Once this PR is merged, create a release in **GitHub** that points to the newly created version.
    - Make sure to provide release notes that at least mirror the contents of `CHANGELOG.md`.

11. **Sync with develop**
    - Create a new PR from `main` → `develop` to synchronize branches.
    - Resolve any conflicts if they occur and merge the PR.
