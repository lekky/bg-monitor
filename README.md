# BG Monitor

An Android app that displays real-time blood glucose values from xDrip+ broadcasts.

## Features

- **Real-time BG Display** - Shows current blood glucose reading in large, easy-to-read format
- **Multiple Units** - Toggle between mg/dL and mmol/L with a single tap
- **Trend Arrows** - Visual indicators for glucose trends (rising, falling, stable)
- **Delta Values** - Shows rate of change in your preferred units
- **Color-Coded Alerts** - Different colors for low, normal, high, and very high glucose levels
- **Persistent Notification** - Always-visible notification with current BG value
- **Background Monitoring** - Foreground service ensures continuous monitoring
- **Debug Mode** - Built-in testing and logging to troubleshoot xDrip+ connection

## Color Coding

- **Red**: Low (< 70 mg/dL / 3.9 mmol/L) or Very High (> 250 mg/dL / 13.9 mmol/L)
- **Green**: Normal (70-180 mg/dL / 3.9-10.0 mmol/L)
- **Orange**: High (180-250 mg/dL / 10.0-13.9 mmol/L)

## Requirements

- Android 7.0 (API 24) or higher
- xDrip+ installed and configured

## Installation

### Download Pre-built APK

The APK is automatically built by GitHub Actions on every push:

1. Go to the [Actions tab](../../actions) in this repository
2. Click on the latest successful workflow run
3. Scroll down to "Artifacts"
4. Download `bg-monitor-debug`
5. Extract the ZIP file to get `app-debug.apk`
6. Transfer to your Android device and install
7. Enable "Install from Unknown Sources" in Android settings if prompted
8. Grant notification permissions when the app launches

### Build from Source

See [BUILD.md](BUILD.md) for detailed build instructions if you prefer to build it yourself.

## How It Works

BG Monitor listens for broadcasts from xDrip+ with the action `com.eveningoutpost.dexdrip.BgEstimate`. When xDrip+ broadcasts new glucose data, BG Monitor:

1. Receives the broadcast via `XDripReceiver`
2. Updates the persistent notification
3. Updates the main UI if the app is open
4. Color-codes the display based on glucose ranges

## Configuration

No configuration needed! Just install and run. The app will automatically:
- Start monitoring when launched
- Request notification permissions (Android 13+)
- Display data as soon as xDrip+ broadcasts it

## Project Structure

```
bg-monitor/
├── app/
│   ├── src/main/
│   │   ├── java/com/bgmonitor/app/
│   │   │   ├── MainActivity.kt          # Main UI
│   │   │   ├── XDripReceiver.kt         # Broadcast receiver
│   │   │   ├── BGMonitorService.kt      # Foreground service
│   │   │   └── BGData.kt                # Data model
│   │   ├── res/                         # Resources (layouts, strings, colors)
│   │   └── AndroidManifest.xml          # App configuration
│   └── build.gradle.kts                 # App-level build config
├── build.gradle.kts                     # Project-level build config
├── settings.gradle.kts                  # Gradle settings
└── BUILD.md                             # Build instructions
```

## Building from Source

See [BUILD.md](BUILD.md) for complete build instructions.

Quick start:
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Development

Built with:
- Kotlin
- Android SDK 34
- Gradle 8.2
- Material Design 3

## License

MIT License - feel free to use and modify as needed.

## Contributing

Contributions welcome! Please feel free to submit issues or pull requests.

## Support

For issues with:
- **BG Monitor**: Open an issue in this repository
- **xDrip+**: Visit [xDrip+ documentation](https://xdrip.readthedocs.io/)

## Disclaimer

This app is for informational purposes only. Always verify blood glucose readings with your primary monitoring device. Do not use this app to make medical decisions without consulting your healthcare provider.