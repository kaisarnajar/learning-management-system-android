# Darse Quran Academy — Android Application 📖📱

Official native Android application for **Darse Quran Academy** ([darsequranacademy.com](https://darsequranacademy.com)), an online Islamic Learning Management System (LMS) offering structured courses in Quran recitation, Tajweed, Islamic Jurisprudence, Hadith, and Arabic language.

---

## 🚀 Key Features

- 🔐 **Student Authentication & Registration**: Complete Sign-In and Sign-Up flows (Email & Password registration, Google Sign-In support, form validation, password visibility toggle).
- 💾 **Secure JWT Session Management**: Persistent login state using **AndroidX Preferences DataStore** and auto-attaching Bearer token headers via OkHttp `AuthInterceptor`.
- 🌅 **Student Home Dashboard**:
  - **Daily Wisdom**: Quranic verses / Hadith cards with Arabic typography and English translations.
  - **Quick Class Join**: Instant Google Meet link button for active sessions (e.g. *Tajweed E Quran – Batch 3*).
  - **Enrolled Courses**: Quick view of active student enrollments and instructor details.
- 🎨 **Modern Islamic Aesthetic**: Custom Material 3 theme incorporating Darse Quran Academy's brand palette (Deep Emerald Green `#003527` & Islamic Warm Gold `#D4AF37`).

---

## 🛠 Tech Stack & Architecture

The app is built following **Android Clean Architecture & MVVM (Model-View-ViewModel)** guidelines:

| Layer | Library / Tool |
| :--- | :--- |
| **Language** | Kotlin 2.0 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Navigation** | Navigation Compose |
| **Networking** | Retrofit 2 + OkHttp 4 + Gson |
| **Session Storage** | AndroidX Preferences DataStore |
| **Async Operations** | Kotlin Coroutines + StateFlow |
| **Google Auth** | Credential Manager API |
| **Min SDK** | API 24 (Android 7.0) |
| **Target SDK** | API 35 (Android 15) |

---

## 📁 Project Structure

```
app/src/main/java/com/darsequran/academy/
├── MainActivity.kt
├── data/
│   ├── local/
│   │   └── TokenManager.kt           # DataStore session manager
│   ├── model/
│   │   └── AuthModels.kt             # Request/Response DTOs
│   ├── remote/
│   │   ├── AuthApi.kt                # Retrofit API endpoints
│   │   ├── AuthInterceptor.kt        # Bearer token HTTP interceptor
│   │   └── RetrofitClient.kt         # Base URL & OkHttp config
│   └── repository/
│       └── AuthRepository.kt         # Auth data source & network result handling
└── ui/
    ├── auth/
    │   ├── LoginScreen.kt            # Login UI composable
    │   ├── LoginViewModel.kt         # Auth ViewModel & state
    │   └── SplashScreen.kt           # Splash screen & token check
    ├── home/
    │   └── HomeScreen.kt             # Student Dashboard UI
    ├── navigation/
    │   ├── NavGraph.kt               # Jetpack Compose NavHost
    │   └── Screen.kt                 # Screen routes
    └── theme/
        ├── Color.kt                  # Brand color palette
        ├── Theme.kt                  # Material3 light/dark theme
        └── Type.kt                   # Typography definitions
```

---

## 🔧 Backend API Configuration

The application is pre-configured to toggle between Production and Local Emulator testing:

```kotlin
// Production API
const val PRODUCTION_BASE_URL = "https://staging-learning-management-system-teal-ten.vercel.app/api/v1/"

// Local Android Emulator API
const val EMULATOR_BASE_URL = "http://10.0.2.2:3000/api/v1/"
```

> 📄 **Complete API Specification**: See [`docs/api_contract.json`](docs/api_contract.json) for the full JSON schema of all endpoints.

---

## ⚙️ Getting Started

### Prerequisites
- **Android Studio**: Ladybug / Jellyfish (2024.1+) or newer
- **JDK**: OpenJDK 17 or 21

### Setup & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/kaisarnajar/learning-management-system-android.git
   ```
2. Open the project in **Android Studio**.
3. Allow Gradle to sync dependencies automatically.
4. Select an Android Emulator or physical device (Android 7.0+) and click **Run 'app'**.

---

## 🤝 Support & Contact

- **Website**: [darsequranacademy.com](https://darsequranacademy.com)

---

### 📄 License
Copyright © 2026 Darse Quran Academy. All rights reserved.
