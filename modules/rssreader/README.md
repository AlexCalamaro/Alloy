# RSS Reader Module

An RSS feed reader module for the Alloy application with desktop widget support.

## Features

- Subscribe to and manage RSS feeds
- View feed articles with titles, content, and timestamps
- Desktop widget for quick access to recent articles
- Offline support with local database caching

## Module Structure

```
rssreader/
├── data/          # Repository implementations
├── db/            # Room database entities and DAOs
├── di/            # Dependency injection modules
├── widget/        # Desktop widget components
└── UI components  # ViewModels and Compose screens
```

## Widget

The module includes a Jetpack Glance-based desktop widget that displays:
- Recent RSS feed articles
- Quick refresh functionality
- Configurable update intervals
