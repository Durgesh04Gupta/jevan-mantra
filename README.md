# JevanMantra — Vedic Astrology, AI Kundali, Kundli Milan & Palmistry App

**JevanMantra** is an Android application built with Kotlin, Jetpack Compose (Material 3), Firebase Authentication (Google Sign-In via Credential Manager), Cloud Firestore, and Firebase AI Logic (`gemini-3.8-flash`).

## Features
- **Sign In & Vedic Profile Registration**: Sign in with Google or register your birth details to automatically generate and save your Janam Kundali in Cloud Firestore.
- **Daily Rashifal & Vedic Panchang**: 12 Rashis (*Mesha* to *Meena*), daily *Tithi/Nakshatra/Yoga/Abhijit Muhurat*, and streaming AI Personalized Predictions.
- **Free Janam Kundali & Unlimited AI Jyotish Q&A**: Custom North-Indian Diamond *Lagna* Chart and interactive AI Jyotish Acharya chat.
- **Ashta-Koota 36-Guna Kundli Milan**: Full 8-Koota marriage compatibility calculator, Mangal Dosha check, and AI marriage report.
- **AI Hand & Palmistry Analysis (*Samudrika Shastra Hastrekha*)**: Multimodal palm line analysis (*Jeevan, Hridaya, Mastishka, Bhagya Rekha* & Planetary Mounts) via Photo Picker, Camera, or instant sample palm scan.
- **Dark Mode & Push Notifications**: Instant Cosmic Dark / Sacred Parchment Light theme toggle and scheduled morning Rashifal notifications.

## How to Export to GitHub or Download the APK

### Option 1: Directly in Google AI Studio
1. Click the **Settings / Export** menu in the top-right corner of Google AI Studio.
2. Select **Push to GitHub** to create or sync a GitHub repository directly, or choose **Download APK / Export ZIP** to install the app on your Android phone.

### Option 2: Automatic APK Build on GitHub Actions
This project includes a pre-configured GitHub Actions workflow (`.github/workflows/android-build.yml`):
1. Push this project to GitHub using the **Push to GitHub** button in AI Studio.
2. Open your repository on GitHub and go to the **Actions** tab.
3. Click the **Build JevanMantra Android APK** workflow run and download **`JevanMantra-Debug-APK`** from the **Artifacts** section.
