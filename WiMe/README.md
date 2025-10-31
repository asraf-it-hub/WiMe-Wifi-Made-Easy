# Wi-Fi AutoLogin App

This Android app automatically logs into the VIT-AP college Wi-Fi network by auto-filling credentials on the captive portal page.

## Features

- **Secure Credential Storage**: Uses Android Keystore for encrypted storage of username and password
- **Auto-login**: Automatically fills and submits login credentials when connected to college Wi-Fi
- **Network Monitoring**: Detects when connected to college Wi-Fi and triggers auto-login
- **First-time Setup**: Shows credentials input dialog on first launch
- **XSS Protection**: Properly escapes credentials to prevent JavaScript injection
- **Modern Android Support**: Compatible with Android 6.0+ with proper SSID detection

## How It Works

1. **First Launch**: User enters username and password in the dialog
2. **Secure Storage**: Credentials are encrypted using Android Keystore and stored securely
3. **Auto-login**: When connected to college Wi-Fi, the app loads the login page, auto-fills credentials, and submits the form
4. **Continuous Monitoring**: Monitors network changes and re-authenticates when needed
5. **Security**: Properly escapes credentials to prevent XSS attacks

## Files Included

- `MainActivity.java` - Main activity with WebView and auto-login logic
- `CredentialManager.java` - Secure credential storage implementation
- `NetworkMonitor.java` - Network connectivity monitoring
- `activity_main.xml` - Layout with WebView
- `AndroidManifest.xml` - App configuration and permissions
- `build.gradle` - Build configuration

## Build Instructions

1. Install Android Studio
2. Create a new Android project
3. Replace the generated files with the provided files
4. Build the project using Android Studio

## Permissions Required

- `INTERNET` - To access the login portal
- `ACCESS_NETWORK_STATE` - To monitor network changes
- `ACCESS_WIFI_STATE` - To detect Wi-Fi connections
- `CHANGE_WIFI_STATE` - To manage Wi-Fi connections (if needed)

## Usage

1. Install the app on your Android device
2. Connect to the college Wi-Fi network
3. Open the app to automatically log in
4. On first launch, enter your credentials when prompted
5. The app will remember your credentials and auto-login on subsequent connections

## Security

- Credentials are stored using Android's Keystore system with AES encryption
- No plain text credentials are stored on the device
- Uses secure encryption (AES/GCM/NoPadding) with proper IV handling
- XSS protection through proper JavaScript string escaping
- Proper SSID detection for modern Android versions (6.0+)

## Key Improvements Made

1. **XSS Protection**: Added proper JavaScript string escaping to prevent injection attacks
2. **Better SSID Detection**: Improved WiFi SSID detection for modern Android versions
3. **Null Safety**: Added proper null checks throughout the code
4. **Enhanced Permissions**: Added necessary permissions for modern Android
5. **Improved Error Handling**: Better error handling for network operations
</final_file_content>
