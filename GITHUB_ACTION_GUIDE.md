# GitHub Action Setup Guide: Signed APK & GitHub Releases

A GitHub Actions workflow has been created in [`.github/workflows/build_release.yml`](file:///home/diego/Documentos/Personal/TattooAr/.github/workflows/build_release.yml). It automatically runs unit tests, compiles a signed release APK, and uploads it to GitHub Releases whenever a version tag is pushed (e.g. `v1.0.0`) or when triggered manually via the **Run workflow** button.

---

## 1. Required GitHub Repository Secrets

To enable automatic keystore signing, navigate to your GitHub Repository:
**Settings** $\rightarrow$ **Secrets and variables** $\rightarrow$ **Actions** $\rightarrow$ **New repository secret**

Add the following 4 secrets:

| Secret Name | Description | Example / Command to Generate |
|---|---|---|
| `KEYSTORE_BASE64` | Base64-encoded string of your `.jks` or `.keystore` file | `base64 -w 0 upload-keystore.jks` |
| `KEYSTORE_PASSWORD` | Password for your keystore file | `myStorePassword123` |
| `KEY_ALIAS` | Alias name of your signing key | `tattoo_alias` |
| `KEY_PASSWORD` | Password for your key alias | `myKeyPassword123` |

---

## 2. Generating an Android Keystore (If you don't have one)

Run the following command in your terminal to generate a production keystore:

```bash
keytool -genkey -v -keystore release-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias tattoo_preview_key
```

To encode it into Base64 format for GitHub Secrets:

```bash
# macOS / Linux
base64 -w 0 release-keystore.jks
```

Copy the printed Base64 string and save it as `KEYSTORE_BASE64` in your GitHub Repository Secrets.

---

## 3. How to Trigger a Release Build

### Option A: Push a Release Tag
```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### Option B: Manual Trigger (GitHub UI)
1. Go to your repository on GitHub.
2. Click on the **Actions** tab.
3. Select **Build & Release Signed APK** on the left menu.
4. Click **Run workflow** $\rightarrow$ enter tag name (e.g. `v1.0.0`) $\rightarrow$ Click **Run workflow**.

---

## 4. What the Workflow Produces
1. **GitHub Action Artifacts**: downloadable `TattooPreview-Release-APK` zip file containing the generated APK.
2. **GitHub Releases**: Creates a new release under the Releases section containing `TattooPreview-v1.0.0.apk` ready for installation on Android devices!
