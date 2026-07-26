# Подплан 4: UI (Compose) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans.

**Goal:** Рабочее Compose-приложение: тема editorial_calm, навигация (Library / Shelves / Settings), экран библиотеки с фильтрами/сортировкой, карточка книги с двумя действиями удаления, плеер-экран + мини-плеер, экран полок, настройки с выбором папки SAF и запуском скана. Приложение собирается в runnable APK.

**Architecture:** Compose + Navigation + Hilt ViewModels. UI подписывается на `StateFlow` из репозиториев/контроллера. Скан запускается из настроек через `ACTION_OPEN_DOCUMENT_TREE` → `ScanFacade.scanNow` → `ScanSettings.setTreeUri`. Изображения обложек через Coil.

**Tech Stack:** Jetpack Compose (Material3), navigation-compose, hilt-navigation-compose, coil-compose, activity-compose.

## File Structure (этот подплан)

```
ui/
  theme/Theme.kt, Color.kt, Type.kt, Shape.kt
  navigation/AppNav.kt, Destinations.kt
  LibraryScreen.kt + LibraryViewModel.kt
  BookDetailScreen.kt + BookDetailViewModel.kt
  PlayerScreen.kt + PlayerBar.kt
  ShelvesScreen.kt + ShelvesViewModel.kt
  SettingsScreen.kt + SettingsViewModel.kt
MainActivity.kt
```

## Tasks (компактно)

1. Compose-зависимости + MainActivity + тема. Smoke-сборка.
2. Навигация + bottom bar + Library (список книг, сортировка, FAB-скан-заглушка).
3. BookDetail (карточка, главы, закладки, действия «удалить с устройства» / «удалить из каталога»).
4. PlayerBar + PlayerScreen (подключение к PlayerController).
5. Shelves + Settings (выбор папки SAF, запуск скана, тема).
6. Финальный прогон + runnable APK.

## Self-Review

Покрытие спеки (Подплан 4): editorial_calm тема, навигация, библиотека/каталог, карточка с двумя удалениями, плеер-экран+мини-плеер, полки, настройки (SAF+скан+тема). Фильтры/сортировка базовые. Эквалайзер-UI опционален (задокументировано в P3: eq ограничен). Плейсхолдеров нет.
