# Settings Module

User settings management

## Structure

```
settings/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/squidink/alloy/modules/settings/
│   │       ├── di/
│   │       │   └── SettingsModule.kt
│   │       ├── data/
│   │       │   ├── ISettingsRepository.kt
│   │       │   └── SettingsRepositoryImpl.kt
│   │       ├── SettingsViewModel.kt
│   │       └── ui/
│   │           └── SettingsScreen.kt
│   └── test/
│       └── java/com/squidink/alloy/modules/settings/
│           └── SettingsViewModelTest.kt
└── README.md
```

## Usage

### Navigation

```kotlin
navController.navigate("settings")
```

## Development

Run tests:

```bash
./gradlew :modules:settings:test
```
