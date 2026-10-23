# KryptUp - Unified Fintech Super-App

![Status](https://img.shields.io/badge/Status-Completed-brightgreen)
![Platform](https://img.shields.io/badge/Platform-Android-green)
![Language](https://img.shields.io/badge/Language-Kotlin-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

> A unified Android fintech "Super-App" bridging fiat currency and cryptocurrency systems, featuring AI-assisted voice operations, biometric hardware security, salted transaction PIN authorization, and Web3 testnet transfers.

---

## 📖 Abstract

KryptUp is an Android-based fintech application designed to unify fiat banking and cryptocurrency vault management into a single platform. The app allows users to securely log in using Google or email/password authentication, link bank accounts (simulated with OTP verification and once-per-user deposit bonus), generate a non-custodial BIP-39 HD crypto wallet on the Ethereum Sepolia testnet, and execute P2P transfers using QR codes protected by hardware-backed facial biometric verification or a 4-digit salted transaction PIN. A built-in spot trading module provides live cryptocurrency prices via CoinGecko REST APIs, while an AI-powered voice assistant offers tap-to-talk hands-free financial commands, balance inquiries, and NLP intent classification.

---

## 🎯 Project Objectives

1. **Unified Financial Experience** – Single app managing both fiat and cryptocurrency assets
2. **Secure P2P Transfers** – Fast, validated transfers via QR codes, facial biometrics, and salted PIN lockout protection
3. **Spot Trading Desk** – Live cryptocurrency market charts and spot transaction logging
4. **AI Voice Assistant** – Rule-based Financial NLP Engine supporting natural language commands
5. **Real-time Communication** – P2P chat messaging with transaction receipt logging
6. **Reward Program** – User engagement tracking with activity points

---

## ✨ Features

### 🔐 Authentication
- Google Sign-In and Email/Password authentication
- Profile management with unique KryptUp Tag and QR ID

### 🏦 Bank Account Linking
- Card input formatting and simulated OTP verification
- Enforced once-per-user initial bank deposit bonus (₹10,000)

### 💼 Crypto Vault
- Standard empty BIP-39 passphrase HD wallet creation and 12-word seed phrase import
- Multi-token support (ETH, USDT) on Ethereum Sepolia testnet
- Send/Receive transactions with Web3j integration and address validation

### 📷 QR Code System
- Generate unique profile QR code
- CameraX & ML Kit barcode scanning with input sanitization and URL-encoded navigation

### 🔒 Biometric & PIN Security
- 4-Digit Transaction PIN setup (`PinSecurityManager`) with SHA-256 hashing and 16-byte random salt
- Rate-limiting lockout: 5 consecutive invalid attempts trigger a 15-minute lockout
- Hardware Biometric Prompt (`BiometricPaymentSecurityManager`) with KeyStore TEE AES-256 cipher integration
- Enrollment invalidation protection (`setInvalidatedByBiometricEnrollment(true)`)
- Camera face scanner visual preview overlay (`CameraFaceScannerView`)

### 📊 Spot Trading
- Real-time prices and market charts powered by CoinGecko API
- Spot order execution and portfolio P&L tracking

### 🎙️ AI Voice Assistant
- Interactive tap-to-talk microphone activation
- Financial NLP Engine (`FinancialNlpEngine`) parsing amounts, recipients, and crypto tokens
- Command support: *"Send 0.5 ETH to Alice"*, *"What's my balance?"*, *"What's ETH price?"*
- Text-to-speech voice responses

### 💬 Communication & Rewards
- One-to-one real-time chat with transaction receipt logging
- Engagement reward points tracking

---

## 🛠️ Technology Stack

| Layer | Technology | Version / Details |
|-------|------------|-------------------|
| **Language** | Kotlin | 2.1.10 |
| **UI Framework** | Jetpack Compose | 2024.12.01 BOM / Material 3 |
| **Architecture** | MVVM + Clean Architecture | Hilt DI, Coroutines, StateFlow |
| **Backend** | Firebase Auth / Firestore / RTDB | Auth, `firestore.rules`, `database.rules.json` |
| **Blockchain** | Web3j | Ethereum Sepolia (`https://ethereum-sepolia-rpc.publicnode.com`) |
| **Security** | Android KeyStore / BiometricPrompt | Hardware TEE, Salted PIN, `FLAG_SECURE`, `allowBackup=false` |
| **Hardware** | CameraX & ML Kit | Barcode / QR Scanning |

---

## 🏗️ System Architecture

```
                                  +-----------------------+
                                  |   App User / Investor |
                                  +-----------+-----------+
                                              |
                                              v
                             +---------------------------------+
                             |   KryptUp Fintech Super-App     |
                             |   (Jetpack Compose + MVVM)      |
                             +----+-------+-------+-------+----+
                                  |       |       |       |
         +------------------------+       |       |       +------------------------+
         |                                |       |                                |
         v                                v       v                                v
+------------------+         +------------------+   +------------------+   +-------------------+
| Firebase Auth &  |         | Android KeyStore |   |  Ethereum Sepolia|   | CoinGecko REST    |
| Firestore Cloud  |         | & Biometrics TEE |   |  Web3 Blockchain |   | Financial API     |
+------------------+         +------------------+   +------------------+   +-------------------+
```

---

## 🔒 Security Architecture

1. **Salted 4-Digit PIN Security**:
   - SHA-256 hashed with 16-byte random salt per user in `EncryptedSharedPreferences`.
   - 5 invalid attempts enforce a 15-minute temporary lockout.

2. **Hardware Biometric KeyStore (TEE)**:
   - Uses `BiometricPrompt` backed by KeyStore AES-256 `CryptoObject`.
   - Invalidated if a new biometric is enrolled in Android system settings (`setInvalidatedByBiometricEnrollment(true)`).

3. **Application & Cloud Hardening**:
   - `FLAG_SECURE` window flags enabled to prevent screenshots on sensitive screens.
   - `allowBackup="false"` configured in manifest.
   - Cloud Firestore (`firestore.rules`) and Realtime Database (`database.rules.json`) security rules enforce strict ownership authorization.

---

## 📂 Project Structure

```
app/src/main/java/com/kryptup/kryptup/
├── data/
│   ├── model/           # User, TransactionRecord, BankAccount, Token models
│   ├── repository/      # AuthRepository, BankingRepository, TransactionRepository
│   └── wallet/          # WalletManager (BIP-39 seed, Web3j integration)
├── security/            # PinSecurityManager, BiometricPaymentSecurityManager
├── ui/
│   ├── navigation/      # NavHost, NavGraph, screen routes
│   └── screens/         # Auth, Banking, Home, Profile, Security, Trading, Wallet
└── voice/               # FinancialNlpEngine, VoiceAssistantManager
```

---

## 🚀 Build & Test Instructions

### Prerequisites
- **Android Studio**: Jellyfish (2024.1.1) or higher
- **JDK**: Java 17 | **Android SDK**: API 34 (Min API 26)

### Building the APK
```bash
./gradlew assembleDebug
```

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

Licensed under the MIT License.
