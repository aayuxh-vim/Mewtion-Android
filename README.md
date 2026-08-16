## Installation

You can install the Mewtion Android companion app using one of the following methods:

### Method 1: Download Pre-built APK (Easiest)

1. Go to the [Releases](https://github.com/YOUR_USERNAME/Mewtion-Android/releases) page of this repository.
2. Download the latest `app-debug.apk` file.
3. Transfer the APK to your Android device and install it. Enable **"Install from unknown sources"** if prompted.

### Method 2: Build from Source (Android Studio)

1. Clone this repository:

   ```bash
   git clone https://github.com/YOUR_USERNAME/Mewtion-Android.git
   ```

2. Open the project folder in **Android Studio**.

3. Connect your Android device via USB with **USB Debugging** enabled.

4. Click the **Run** (`▶`) button in Android Studio to build and install the app directly onto your phone.

## Usage

1. Open the **Mewtion** app on your Android device. It will automatically start a foreground service to listen for sensor connections.

2. Connect your phone to your Linux laptop via a USB data cable.

3. On your laptop, set up ADB port forwarding to stream sensor data over TCP:

   ```bash
   adb forward tcp:8765 tcp:8765
   ```

4. Launch the desktop overlay (`Mewtion`) on your Linux machine to start receiving real-time motion cues.

