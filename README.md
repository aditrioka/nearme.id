# NearMe

A location-based Android application that allows users to discover and interact with content based on their proximity.

## Features

- Location-based content discovery
- Create and share new posts tied to specific locations
- View nearby posts from other users
- User authentication

## Tech Stack

- Kotlin
- Jetpack Compose for UI
- Firebase (Firestore, Authentication, Analytics)
- Google Play Services Location
- Dagger Hilt for dependency injection
- Kotlin Serialization

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- Android SDK 24+
- Firebase project setup

### Setup

1. Clone the repository
   ```
   git clone https://github.com/yourusername/nearmeid.git
   ```

2. Open the project in Android Studio

3. Connect to your Firebase project or use the existing configuration

4. Build and run the application

## Project Structure

- `app/src/main/java/id/nearme/app/` - Main application code
- `presentation/` - UI components and ViewModels
- `di/` - Dependency injection modules

## License

[MIT](LICENSE)