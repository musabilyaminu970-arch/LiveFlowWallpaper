# Build the APK using only your phone

You do NOT need a laptop for this method.

This project includes a GitHub Actions build workflow. GitHub's servers do the Android build for you.

## One-time setup

1. On your phone, create/sign in to a GitHub account.
2. In your browser, create a new GitHub repository.
3. Upload the contents of this project to that repository.
4. Commit the files to the `main` branch.
5. Open the repository's **Actions** tab.
6. Select **Build Live Flow APK**.
7. Tap **Run workflow** if it has not already started.
8. Wait for the build to finish.
9. Open the completed workflow run.
10. At the bottom, under **Artifacts**, download `LiveFlow-debug-apk`.
11. Extract the downloaded artifact and install `app-debug.apk` on your Android phone.

The resulting APK is a debug-signed APK suitable for installing on your own phone.

## Important

The GitHub build is performed remotely. Your phone is only used to upload the project and download the finished APK.

The workflow installs Android SDK 35, Java 17 and Gradle 8.10.2 automatically.
