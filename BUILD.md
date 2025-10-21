# Building BG Monitor APK

This guide shows you how to build the BG Monitor Android app without Android Studio.

## Prerequisites

You need to have the following installed on your machine:

1. **Java Development Kit (JDK)** - Version 17 or higher
   - Download from: https://adoptium.net/
   - Verify installation: `java -version`

2. **Android SDK Command Line Tools**
   - Download from: https://developer.android.com/studio#command-tools
   - Extract to a directory (e.g., `~/Android/Sdk`)
   - Set environment variable: `export ANDROID_HOME=~/Android/Sdk`

3. **Android SDK Components** (install via sdkmanager):
   ```bash
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```

## Building the APK

### Option 1: Using Gradle Wrapper (Recommended)

The project includes Gradle wrapper scripts, so you don't need to install Gradle separately.

1. **Clone the repository** (if you haven't already):
   ```bash
   git clone <your-repo-url>
   cd bg-monitor
   ```

2. **Make gradlew executable** (Linux/Mac):
   ```bash
   chmod +x gradlew
   ```

3. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

   On Windows:
   ```cmd
   gradlew.bat assembleDebug
   ```

4. **Find your APK**:
   The APK will be located at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Option 2: Using System Gradle

If you have Gradle installed system-wide:

```bash
gradle assembleDebug
```

### Build Options

**Debug Build** (for development/testing):
```bash
./gradlew assembleDebug
```

**Release Build** (optimized, requires signing):
```bash
./gradlew assembleRelease
```

Note: Release builds require signing configuration. See "Signing Your APK" below.

## Installing the APK

### Via USB (ADB)

1. Enable Developer Options on your Android device
2. Enable USB Debugging
3. Connect your device via USB
4. Install the APK:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Via File Transfer

1. Copy the APK to your phone
2. Open the APK file on your device
3. Allow installation from unknown sources if prompted

## Signing Your APK (for Release)

To create a signed release APK:

1. **Generate a keystore** (one-time setup):
   ```bash
   keytool -genkey -v -keystore bg-monitor.keystore -alias bg-monitor -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure signing** in `app/build.gradle.kts`:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("../bg-monitor.keystore")
               storePassword = "your_keystore_password"
               keyAlias = "bg-monitor"
               keyPassword = "your_key_password"
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               isMinifyEnabled = true
               proguardFiles(...)
           }
       }
   }
   ```

3. **Build signed release**:
   ```bash
   ./gradlew assembleRelease
   ```

## Troubleshooting

### Error: ANDROID_HOME not set
```bash
export ANDROID_HOME=/path/to/your/android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
```

### Error: SDK not found
Install the required SDK components:
```bash
sdkmanager --install "platforms;android-34" "build-tools;34.0.0"
```

### Error: Java version mismatch
Make sure you're using JDK 17 or higher:
```bash
java -version
```

### Clean Build
If you encounter build issues:
```bash
./gradlew clean
./gradlew assembleDebug
```

## Useful Gradle Commands

- `./gradlew tasks` - List all available tasks
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew assembleRelease` - Build release APK
- `./gradlew installDebug` - Build and install debug APK
- `./gradlew clean` - Clean build artifacts
- `./gradlew build` - Build both debug and release variants

## Testing with xDrip+

1. Install xDrip+ on the same device
2. Configure xDrip+ with your CGM
3. Install BG Monitor APK
4. Launch BG Monitor
5. Grant notification permissions when prompted
6. The app will automatically receive and display blood glucose data from xDrip+

## Additional Resources

- [Android Developer Guide](https://developer.android.com/studio/build/building-cmdline)
- [Gradle Build Documentation](https://docs.gradle.org/current/userguide/userguide.html)
- [xDrip+ Documentation](https://xdrip.readthedocs.io/)
