# YouTube by SunFlare94

A feature-rich YouTube client with powerful ad blocking, sponsor blocking, and a beautiful dark mode interface.

## Features

### Ad Blocking
- **Strong Ad Blocker**: Blocks all YouTube advertisements including video ads, banner ads, and overlay ads
- **Auto Skip Ads**: Automatically skips advertisements when they appear
- **Block Tracking**: Blocks analytics and tracking scripts

### Sponsor Block
- **Sponsor Block**: Skips sponsored segments in videos using the SponsorBlock API
- **Skip Intro**: Automatically skips video introductions
- **Skip Outro**: Automatically skips video outros
- **Skip Filler**: Skips non-essential filler content

### UI/UX
- **Full Dark Mode**: Complete dark theme throughout the app
- **Immersive Splash Screen**: Beautiful splash screen with "YouTube by SunFlare94" text
- **Material Design**: Modern Material Design interface

## Package Name
`com.youtube.app`

## Requirements
- Android 7.0 (API 24) or higher
- Internet connection

## Building the App

### Using Android Studio
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the YouTube folder and select it
4. Wait for Gradle sync to complete
5. Click Build > Build Bundle(s) / APK(s) > Build APK(s)

### Using Command Line
```bash
cd YouTube
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## Settings

Access settings by clicking the gear icon in the top right corner of the main screen.

### Ad Blocker Settings
- **Ad Blocker**: Enable/disable all ad blocking
- **Auto Skip Ads**: Enable/disable automatic ad skipping
- **Block Tracking**: Enable/disable tracking protection

### Sponsor Block Settings
- **Sponsor Block**: Enable/disable sponsor segment skipping
- **Skip Intro**: Enable/disable intro skipping
- **Skip Outro**: Enable/disable outro skipping
- **Skip Filler**: Enable/disable filler content skipping

## Technical Details

- **WebView-based**: Uses Android WebView for rendering YouTube
- **JavaScript Injection**: Injects ad blocking and sponsor block scripts
- **Domain Blocking**: Blocks ad and tracking domains at the network level
- **SponsorBlock API**: Integrates with the open-source SponsorBlock API

## Privacy

- No data is collected or sent to third parties
- All ad blocking happens locally on your device
- SponsorBlock queries are anonymous

## License

This project is for educational purposes only.

## ❤️ Support Development

If you enjoy this project and would like to support its development, you can make a contribution using any of the options below.

### 💙 GitHub Sponsors

Support the project through GitHub Sponsors:

**https://github.com/sponsors/SunFlare94**

### 💳 Razorpay

You can make a payment or contribution through Razorpay:

**https://razorpay.me/@SunFlare94**

### 📱 Pay via UPI

Scan the Razorpay QR code below using **Google Pay, PhonePe, Paytm, or any other UPI app**.

![Razorpay UPI QR Code](QrCode.jpeg)

Your support helps me continue developing and maintaining this project.

Thank you! ❤️
