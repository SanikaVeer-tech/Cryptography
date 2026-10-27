# 🔐 Cryptography — Image, Audio & Video Steganography

An Android application built with **Java** that explores secure media sharing through a combination of encryption-related workflows and steganography concepts. The app provides separate screens for image, audio, and video operations, and uses Firebase services to store and retrieve associated media data and keys.

> **Project note:** This is an educational project. Review the implementation carefully before relying on it for real confidential data. In the current source, some media workflows encode or store media data and messages, but Base64 encoding or combining files is not the same as cryptographic encryption or robust steganography.

## ✨ Features

### 🖼️ Image
- Choose an image from the device.
- Enter a message associated with the image.
- Generate a unique key to retrieve the saved record.
- Store image data and the associated message in Firebase Realtime Database.
- Display/share the key and save/share the image from the device.
- Look up an image record and its associated message using a key.

### 🎵 Audio
- Select audio files for the audio workflow.
- Enter a message associated with the audio.
- Generate and share a lookup key.
- Store and retrieve audio-related data through the configured Firebase workflow.
- Save audio output locally where supported by the implementation.

### 🎬 Video
- Select video files through the app's video screens.
- Enter a message associated with the video.
- Generate and share a lookup key.
- Upload video files and store associated metadata using Firebase services in supported workflows.
- Retrieve video-related data and display/play media in the app.

### ☁️ Firebase Integration
- Firebase Realtime Database for storing key-associated media data and metadata.
- Firebase Storage for video uploads/downloads in the relevant workflow.
- Firebase Firestore dependency is included in the Android app configuration.

## 🛠️ Tech Stack

- **Language:** Java
- **Platform:** Android
- **IDE:** Android Studio
- **Build system:** Gradle (Kotlin DSL)
- **UI:** XML layouts, AndroidX, Material Components
- **Media handling:** Android media pickers, Bitmap, MediaStore, VideoView, Base64 utilities
- **Cloud services:** Firebase Realtime Database, Firebase Storage, Firebase Firestore dependency
- **Local database helper:** SQLite via `SQLiteOpenHelper`
- **Minimum Android SDK:** API 24 (Android 7.0)
- **Compile / target SDK:** API 34

## 🗂️ Project Structure

```text
Cryptography/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/cryptography/
│   │   │   ├── MainActivity.java
│   │   │   ├── ImageEncryptActivity.java
│   │   │   ├── ImageDecryptActivity.java
│   │   │   ├── AudioEncryptActivity.java
│   │   │   ├── AudioDecryptActivity.java
│   │   │   ├── VideoEncryptActivity.java
│   │   │   ├── VideoDecryptActivity.java
│   │   │   ├── EncryptVideoActivity.java
│   │   │   ├── DecryptVideoActivity.java
│   │   │   ├── DBHelper.java
│   │   │   └── MyApplication.java
│   │   ├── res/
│   │   │   ├── layout/       # XML screen layouts
│   │   │   ├── drawable/     # UI resources
│   │   │   └── values/       # App strings, colors, and themes
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew.bat
```

## 🚀 Getting Started

### Prerequisites

- Android Studio
- Android SDK API 34
- A JDK supported by your installed Android Gradle Plugin
- An Android device or emulator running Android 7.0 (API 24) or later
- A Firebase project with Realtime Database and Storage enabled for the workflows you want to test

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/SanikaVeer-tech/Cryptography.git
   ```

2. **Open the project**  
   In Android Studio, select **Open** and choose the cloned `Cryptography` directory.

3. **Configure Firebase**  
   In the Firebase console, create or select a project and register an Android app using the application ID `com.example.cryptography`. Download its `google-services.json` configuration file and place it in the `app/` directory. Enable and configure Realtime Database and Storage as required by the features you use.

4. **Review Firebase rules**  
   Configure appropriate authentication and database/storage rules for your environment. Do not use open read/write rules for production data.

5. **Sync Gradle**  
   Let Android Studio sync the project and download dependencies.

6. **Run the app**  
   Select an emulator or connect an Android device, grant requested media permissions when prompted, and click **Run**.

## 🔑 How the Key-Based Workflow Works

1. Choose the media type and select the required file(s).
2. Enter the message, where the selected screen requests one.
3. Run the corresponding operation.
4. The app generates or displays a key used to locate the corresponding record.
5. Share the key with the intended recipient through a separate, appropriate channel.
6. On the corresponding retrieval screen, enter the key to retrieve the associated data.

Keep in mind that a lookup key is not necessarily a cryptographic encryption key. Treat access keys as sensitive because anyone who can access a record may be able to read its associated data depending on the Firebase rules.

## ⚠️ Security and Implementation Notes

- **Base64 is encoding, not encryption.** It does not protect the contents of an image, audio file, video, or message.
- **A combined video file is not automatically steganography.** Appending or combining media bytes does not produce a reliable playable video with hidden content.
- **Protect Firebase data.** The app uses Firebase services; ensure Realtime Database and Storage rules prevent unauthorized access. Do not put service-account credentials or private secrets in source control.
- **Review public configuration.** The repository currently contains `app/google-services.json`. Firebase client configuration is normally used by Android apps, but review project settings, API-key restrictions, and database/storage security rules before sharing or deploying.
- **Large media files can use substantial memory.** Test with small files first, and consider streaming large files rather than loading entire files into memory.
- **Storage behavior varies by Android version.** Check media permissions and storage behavior on the device versions you intend to support.

## 🧭 Possible Improvements

- Implement authenticated access and strict Firebase Security Rules.
- Use a vetted cryptographic design with authenticated encryption, such as AES-GCM, for any data that truly requires confidentiality and integrity.
- Use secure key management and never store secrets directly alongside encrypted payloads.
- Implement a defined, tested steganography method for each media type instead of simply encoding or combining bytes.
- Add progress indicators and size limits for large media operations.
- Add validation, error handling, and tests for encryption/decryption and media retrieval.
- Document the expected workflow for each screen and remove duplicate or experimental screens when the final flow is chosen.

## 👩‍💻 Author

**Sanika Veer**  
GitHub: [SanikaVeer-tech](https://github.com/SanikaVeer-tech)

---

This project is intended for learning and experimentation with Android development, media processing, and cryptography concepts.
