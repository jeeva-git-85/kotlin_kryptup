# KryptUp - Unified Fintech Super-App

![Status](https://img.shields.io/badge/Status-Completed-brightgreen)
![Platform](https://img.shields.io/badge/Platform-Android-green)
![Language](https://img.shields.io/badge/Language-Kotlin-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

> A unified fintech "Super-App" that bridges traditional fiat currency and cryptocurrency systems, enhanced with AI-powered voice automation, biometric hardware security, transaction PIN enforcement, and integrated communication.

---

## 📖 Abstract

KryptUp is an Android-based fintech application designed to unify the fragmented financial ecosystem by providing a single platform for banking, cryptocurrency management, trading, communication, and AI-assisted financial tasks.

The rapid growth of digital finance has created a fragmented ecosystem where users must juggle multiple applications for banking, cryptocurrency management, trading, and peer-to-peer transfers. Traditional banking apps lack crypto support, while crypto platforms often ignore fiat integration and user-friendly automation. **KryptUp** addresses this gap by seamlessly bridging traditional fiat currency and cryptocurrency systems in one application.

The app allows users to securely log in using Google or email/password authentication, link bank accounts (simulated with OTP verification), create and manage a non-custodial crypto wallet on the Ethereum Sepolia testnet, and execute peer-to-peer transactions by scanning QR codes followed by facial biometric verification or 4-digit transaction PIN. A built-in trading module provides real-time price data and a simulated order book for spot trading with market, limit, stop-loss, and take-profit orders. An AI-powered voice assistant enables hands-free operations such as sending crypto, placing trades, analyzing spending patterns, and providing EMI reminders. Users can chat with each other in real-time, with transaction receipts automatically reflected in the conversation. A reward points system incentivizes user engagement for transactions, trading activity, and daily logins.

The project demonstrates the practical application of modern technologies including Kotlin, Jetpack Compose, Firebase, Web3j blockchain integration, CameraX, ML Kit, BiometricPrompt, and voice recognition, showcasing full-stack mobile development skills.

---

## 🎯 Project Objectives

1. **Unified Financial Experience** – Provide a single app for both fiat and crypto management
2. **Secure P2P Transfers** – Enable fast, secure transfers using QR codes, facial biometric verification, and PIN authorization
3. **Integrated Trading** – Offer a simulated cryptocurrency exchange with real-time prices
4. **AI-Powered Automation** – Develop a voice assistant with speech recognition for hands-free financial tasks
5. **Social Transactions** – Allow chat with automatic transaction receipts
6. **User Engagement** – Implement a reward points system

---

## ✨ Features

### 🔐 Authentication
- Google Sign-In integration
- Email/Password registration and login
- Secure session management
- User profile management with unique KryptUp Tag/QR ID

### 🏦 Bank Account Linking
- ATM card details input with auto-formatting
- OTP verification with once-per-user initial deposit bonus (₹10,000)
- Masked card storage
- Secure data handling

### 💼 Crypto Vault
- Create new HD wallet with standard empty BIP-39 seed passphrase
- Import wallet via 12-word seed phrase
- Support for multiple tokens (ETH, USDT, USDC, DAI, LINK)
- Balance tracking
- Send/Receive functionality with Web3 Sepolia integration
- Private key secured in Android Keystore

### 📷 QR Code System
- Generate unique QR code for each user profile
- Scan QR codes using CameraX and ML Kit
- Recipient information lookup
- Input validation and URL-encoded route parameter handling

### 🔒 Biometric & PIN Transaction Authorization
- 4-Digit Transaction PIN setup and enforcement (`PinSecurityManager`) with SHA-256 hashing and 16-byte per-user random salt
- 5-failed-attempt threshold with 15-minute temporary lockout enforcement
- Facial & Fingerprint verification via Android `BiometricPrompt` and `BiometricPaymentSecurityManager`
- Hardware Security Enclave (TEE) backed AES-256 cipher encryption
- Invalidation on new biometric enrollment (`setInvalidatedByBiometricEnrollment(true)`)
- Camera Face Scanner visual overlay (`CameraFaceScannerView`)
- Real-time transaction receipt generation

### 📊 Trading Platform
- Real-time cryptocurrency prices from CoinGecko API
- Interactive price charts and candlestick visuals
- Order book visualization
- Market and limit orders
- Stop-loss and take-profit options
- Portfolio tracking with P&L calculation

### 🎙️ AI Voice Assistant
- Interactive microphone activation
- Voice greeting: *"Hey! What can I help you with today?"*
- Rule-based Financial NLP Engine (`FinancialNlpEngine`)
- Entity extraction (amounts, recipients, crypto tokens)
- Supported commands:
    - *"Send 0.5 ETH to Alice"*
    - *"Buy 1 ETH at 3000 with stop loss 2900"*
    - *"What's my balance?"*
    - *"How much did I spend yesterday?"*
    - *"When is my next EMI?"*
    - *"What's ETH price?"*
- Spending analysis with category breakdown
- EMI reminders & alerts
- Text-to-speech voice responses

### 💬 Communication Hub
- One-to-one real-time chat
- Automatic transaction receipts in chat threads
- Message read status and unread indicators

### 🎁 Reward System
- Earn points for transactions and trading activity
- Points balance tracking
- Benefit redemptions and activity history

---

## 🛠️ Technology Stack

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.1.10 | Primary programming language |
| Jetpack Compose | 2024.12.01 BOM | Declarative UI framework |
| Material 3 | 1.3.1 | Design system components |
| Navigation Compose | 2.8.5 | Screen navigation & argument passing |
| MPAndroidChart | 3.1.0 | Interactive financial charts |

### Architecture
| Technology | Purpose |
|------------|---------|
| MVVM + Clean Architecture | Separation of concerns |
| Hilt | Dependency injection |
| Kotlin Coroutines + Flow | Asynchronous programming & state management |
| Repository Pattern | Abstracted data handling |

### Backend & Cloud
| Technology | Purpose |
|------------|---------|
| Firebase Authentication | User authentication |
| Cloud Firestore | User profiles, balances, and transaction history (`firestore.rules`) |
| Firebase Realtime Database | Real-time chat messages (`database.rules.json`) |

### Blockchain & Web3
| Technology | Purpose |
|------------|---------|
| Web3j | Ethereum blockchain interaction |
| Ethereum Sepolia Testnet | Smart contract & transfer testing (`https://ethereum-sepolia-rpc.publicnode.com`) |
| ERC-20 | Standard token interaction |
| Android Keystore | Hardware private key storage |

### Hardware & Security Integration
| Technology | Purpose |
|------------|---------|
| CameraX | Camera preview & scanner |
| ML Kit Barcode Scanning | QR code recognition |
| BiometricPrompt | Hardware-backed facial/fingerprint authentication |
| Android KeyStore / TEE | Hardware secure enclave cryptographic operations |
| FLAG_SECURE | Window screenshot and screen capture prevention |
| SpeechRecognizer / TTS | Voice input processing and voice responses |

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

### PlantUML Use-Case Overview
The complete system use-case diagram is defined in `plantuml_use_case.puml` and includes:
- **Authentication**: Registration, Login, Profile, PIN & Biometric setup.
- **Banking**: Bank Account Linking, 2FA OTP, UPI Transfers, QR Payments.
- **Crypto Vault**: BIP-39 Seed derivation, Crypto Transfers (ETH/USDT), Wallet QR scanning.
- **Spot Trading**: Live Market Depth, Spot Buy/Sell Orders with Stop-Loss/Take-Profit.
- **AI Voice Assistant**: Hands-free voice/text commands, Chat & DM transfers, Daily APY boosters.

---

## 🔒 Security Architecture

### 1. 4-Digit Transaction PIN Enforcement (`PinSecurityManager`)
- **Storage**: PINs are hashed using SHA-256 with a cryptographically secure 16-byte per-user random salt and saved in `EncryptedSharedPreferences`.
- **Lockout Policy**: 5 consecutive invalid attempts trigger a 15-minute temporary lockout to prevent brute-force attacks.
- **Dialog Authorization**: Integrated into financial workflows via `PinSetupDialog` and `PinVerificationDialog`.

### 2. Hardware-Backed Biometric Security (`BiometricPaymentSecurityManager`)
- **Hardware Enclave**: Integrates with Android's Trusted Execution Environment (TEE) / StrongBox KeyStore via `BiometricPrompt`.
- **Privacy Compliance**: Biometric feature templates remain inside the hardware enclave; raw biometric data is never exposed to application memory or external cloud servers.
- **Key Invalidation**: Utilizes `setInvalidatedByBiometricEnrollment(true)`. Adding a new biometric face/fingerprint in device system settings invalidates existing payment keys.

### 3. Application Hardening & Cloud Rules
- **Screen Capture Protection**: `WindowManager.LayoutParams.FLAG_SECURE` enforced in `MainActivity` to prevent screenshots on sensitive screens.
- **Backup Prevention**: `android:allowBackup="false"` prevents exporting `EncryptedSharedPreferences` via ADB or cloud backups.
- **Cloud Access Control**: `firestore.rules` and `database.rules.json` enforce document-level ownership permissions for Cloud Firestore and Realtime Database.

---

## 📂 Project Structure

```
app/src/main/java/com/kryptup/kryptup/
├── data/
│   ├── model/           # User, TransactionRecord, BankAccount, CryptoWallet models
│   ├── repository/      # AuthRepository, BankingRepository, TransactionRepository
│   └── wallet/          # WalletManager (BIP-39 seed, Web3j integration)
├── security/            # PinSecurityManager, BiometricPaymentSecurityManager
├── ui/
│   ├── components/      # Reusable Compose buttons, cards, charts, input fields
│   ├── navigation/      # NavHost, NavGraph, screen routes
│   ├── screens/
│   │   ├── auth/        # LoginScreen, RegisterScreen, AuthViewModel
│   │   ├── banking/     # BankLinkingScreen, BankingViewModel
│   │   ├── home/        # HomeScreen, HomeViewModel
│   │   ├── profile/     # ProfileScreen, ProfileViewModel
│   │   ├── security/    # PinSetupDialog, PinVerificationDialog, CameraFaceScannerView
│   │   ├── splash/      # SplashScreen
│   │   ├── trading/     # TradingScreen, TradingViewModel
│   │   └── wallet/      # SendScreen, ReceiveScreen, WalletViewModel
│   └── theme/           # Color, Type, Theme tokens
└── voice/               # FinancialNlpEngine, VoiceAssistantManager
```

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- **Android Studio**: Jellyfish (2024.1.1) or higher
- **JDK**: Java 17
- **Android SDK**: API Level 34 (Minimum API Level 26 / Android 8.0)

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/jeeva-git-85/kotlin_kryptup.git
   cd kotlin_kryptup
   ```
2. Build debug APK using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run on a connected device or Android Emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## 🧪 Testing

The project includes comprehensive unit tests covering critical security algorithms and NLP voice command interpretation.

### Running Unit Tests
Execute the Gradle test task from the terminal:
```bash
./gradlew testDebugUnitTest
```

### Test Suites Included
- **`PinSecurityManagerTest`**: Validates PIN setup, salted SHA-256 verification, invalid PIN rejection, and 5-attempt lockout security.
- **`FinancialNlpEngineTest`**: Validates intent classification and entity parsing for voice commands (sending crypto, checking balances, querying exchange rates).

---

## 📄 License

This project is licensed under the MIT License - see the `LICENSE` file for details.
