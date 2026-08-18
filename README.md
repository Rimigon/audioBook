# AudioBook — аудиокниги для Android

Личное Android-приложение для прослушивания аудиокниг, скачанных на устройство. Всё хранилище локальное, без облака.

Ключевая фишка: **удаление аудиофайла с устройства не удаляет книгу из каталога.** Карточка с метаданными, прогрессом, закладками, оценкой и историей переживает удаление файлов и «оживает», когда файл снова появляется.

## Возможности

- **Универсальный плеер**: mp3-папки, m4b с главами, одиночные файлы (mp3/m4a/opus/flac/ogg) — авто-определение.
- **Автоскан** выбранной папки (SAF, `ACTION_OPEN_DOCUMENT_TREE` + persistable permission).
- Плеер: скорость 0.5–4×, таймер сна, закладки с заметками, настраиваемая перемотка, MediaSession (экран блокировки, наушники), автопауза, эквалайзер, volume boost, фоновое воспроизведение, авто-резьюм.
- **Каталог**: метаданные + обложка + прогресс + закладки + оценка + теги + полки/коллекции + история.
- **Метаданные**: из тегов файла + ручное редактирование + онлайн-обогащение (OpenLibrary / Google Books).
- Локальное хранение: Room + кэш обложек.

## Стек

- Kotlin + Jetpack Compose
- Media3 (ExoPlayer + `MediaSessionService`)
- Room, Hilt, WorkManager, SAF, Coroutines/Flow, Compose Navigation

## Структура

```
app/
├── app/            # Application, Hilt, навигация
├── domain/         # model + usecase
├── data/           # Room, SAF, репозитории, кэш обложек
├── metadata/       # теги, главы m4b, онлайн-обогащение
├── player/         # Media3 service, фоновое воспроизведение
└── ui/             # библиотека, плеер, детали книги, полки
```

Полный дизайн — в [`docs/superpowers/specs/`](docs/superpowers/specs/).

## Сборка

```bash
./gradlew assembleDebug   # APK в app/build/outputs/apk/debug/
```

Требуется Android SDK 34+ (см. `gradle.properties` / `build.gradle.kts`).
