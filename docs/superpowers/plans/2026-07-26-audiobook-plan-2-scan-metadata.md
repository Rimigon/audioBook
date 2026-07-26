# Подплан 2: SAF-сканер + Конвейер метаданных — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans. Steps use checkbox (`- [ ]`).

**Goal:** Книги появляются в каталоге из выбранной SAF-папки; для каждой книги читаются теги/главы, при неполных данных — онлайн-обогащение (OpenLibrary/Google Books) с кэшем; ручное переопределение защищено флагом `manuallyEdited`.

**Architecture:** Чистая логика (классификация структуры, построение глав, нормализация названия, парсинг JSON-ответов) отделена от Android/сети адаптерами и тестируется JVM-тестами. Сетевые источники за интерфейсом `OnlineMetadataSource`. Оркестратор `MetadataEnricher` ходит в кэш `MetadataCacheDao`. SAF-обход — тонкая обёртка над `DocumentFile`. Публичная точка — `ScanFacade.scanNow(treeUri)` (suspend); периодический `WorkManager`-воркер отложен в Подплан 4 (настройки).

**Tech Stack:** DocumentFile (SAF), MediaMetadataRetriever (теги), Media3 MetadataExtractor (главы m4b), OkHttp (онлайн), androidx.datastore (путь папки), androidx.work (воркер — заготовка).

## File Structure (этот подплан)

```
domain/model/
  AudioFileRef.kt          # ссылка на аудиофайл в SAF (uri, name, sizeMs?)
  BookDescriptor.kt        # результат классификации: type + файлы
  BookMetadata.kt          # метаданные книги (title/author/cover/...)
data/saf/
  FolderScanner.kt         # обход DocumentFile -> List<BookDescriptor> (адаптер)
  BookClassifier.kt        # ЧИСТАЯ эвристика: дерево -> List<BookDescriptor>
  ScanSettings.kt          # DataStore: persisted treeUri
  ScanFacade.kt            # scanNow(treeUri): связка сканер+метаданные+репо
metadata/
  tags/TagReader.kt        # интерфейс + MediaMetadataRetriever impl
  chapters/ChapterBuilder.kt   # ЧИСТАЯ: файлы/длительности -> List<Chapter>
  chapters/M4bChapterExtractor.kt  # адаптер Media3 для embedded chapters
  online/OnlineMetadataSource.kt  # интерфейс
  online/OpenLibrarySource.kt
  online/GoogleBooksSource.kt
  online/TitleNormalizer.kt   # ЧИСТАЯ: regex-чистка названия для поиска
  online/MetadataEnricher.kt  # оркестратор: теги -> онлайн -> кэш
```

---

### Task 1: `BookClassifier` (чистая эвристика) + тест

**Files:** `domain/model/AudioFileRef.kt`, `domain/model/BookDescriptor.kt`, `data/saf/BookClassifier.kt`, test.

`AudioFileRef(uri: String, name: String, mime: String?)`. Эвристика:

- Узел «папка» = список детей. Папка с ≥1 аудиофайлом → `FOLDER` (дети-аудио = главы, natural-order по имени). Не-аудио дети (jpg/png/cue/txt/nfo) игнорируются.
- Папка, где все дети — подпапки → рекурсия (каждая подпапка — кандидат).
- Одиночный аудиофайл вне книги-папки → `M4B` если `.m4b`, иначе `SINGLE_FILE`.

Вход: `sealed class FsNode { data class Dir(name, children: List<FsNode>); data class File(name, ref: AudioFileRef) }`. `BookClassifier.classify(roots: List<FsNode>): List<BookDescriptor>`. Аудио-расширения: mp3,m4b,m4a,opus,flac,ogg,aac,wav.

- [ ] тесты: папка с mp3 → один FOLDER; корень с разрозненными mp3 + m4b → две SINGLE/M4B; папка с подпапками-книгами; не-аудио игнорируются.
- [ ] реализация; прогон; коммит `feat(scan): book classifier heuristics`.

### Task 2: `ChapterBuilder` (чистая) + тест

Из `List<AudioFileRef>` (для FOLDER) строит `List<Chapter>`: index по natural-order имени, title = имя без расширения, startMs/endMs = 0 (длительность выясняется плеером/TagReader позже; здесь без медиа — оставляем 0, плеер заполнит). Для SINGLE_FILE/M4B без embedded-глав — одна виртуальная глава на всю книгу. Это утилита для FOLDER; для m4b есть отдельный экстрактор.

- [ ] тесты: порядок natural-order (`ch2` < `ch10`), title без расширения, индексы.
- [ ] реализация; коммит `feat(metadata): chapter builder for folder books`.

### Task 3: `TitleNormalizer` (чистая) + тест

`normalize(raw: String): String` → lowercase, обрезает мусорные токены (mp3, m4b, audiobook, аудиокнига, год `19xx`/`20xx`, скобки-технические, разделители `_./-` → пробел, схлопывание пробелов). Для запроса к онлайн-API.

- [ ] тесты: `"Дюна (1965) [m4b]"` → `"дюна"`; `"Author - Book mp3 320"` → `"author book"`; год удаляется.
- [ ] реализация; коммит `feat(metadata): title normalizer`.

### Task 4: `OnlineMetadataSource` + парсинг OpenLibrary/Google Books + тест

Интерфейс `OnlineMetadataSource { suspend fun search(title: String, author: String?): MetadataResult? }`. `MetadataResult(title, author, description, coverUrl, source)`. Реализации парсят JSON-ответ (org.json). Тесты парсят зафиксированные строки JSON (без сети) через метод `parse(json): MetadataResult?`.

- [ ] `OpenLibrarySource.parse` тест на реальном образце ответа (`docs` array).
- [ ] `GoogleBooksSource.parse` тест на `items[].volumeInfo`.
- [ ] сетевые методы (OkHttp) — без автотеста; коммит `feat(metadata): online metadata sources`.

### Task 5: `MetadataEnricher` (оркестратор + кэш) + тест

`MetadataEnricher(tags: BookMetadata, cache: MetadataCacheDao, sources: List<OnlineMetadataSource>)`. Логика: если теги полны (есть title+author+cover) — вернуть как есть. Иначе — `queryKey = normalize(title)+"|"+(author?:"")`; проверить кэш → если есть, распарсить payload. Иначе — спросить источники по очереди, первый непустой → кэш+вернуть. Тест с фейковым DAO (in-memory map) и фейковым источником.

- [ ] тесты: full-tags → no network; partial → cache hit; cache miss → source → cached.
- [ ] реализация; коммит `feat(metadata): metadata enricher with cache`.

### Task 6: `TagReader` + `M4bChapterExtractor` (адаптеры) + smoke

`TagReader` через `MediaMetadataRetriever` (interface `TagReader { fun read(uri: String): BookMetadata }`). `M4bChapterExtractor` через Media3 `MetadataExtractor` — без unit-теста (нужен реальный m4b), только собирается. Добавить зависимость media3-extractor в каталог. Smoke — Robolectric проверяет, что класс инстанцируется/компилируется.

- [ ] добавить media3-exoplayer/metadata в зависимости (для Подплана 3 тоже).
- [ ] реализации; коммит `feat(metadata): tag reader and m4b chapter extractor adapters`.

### Task 7: `FolderScanner` (SAF-адаптер) + `ScanSettings` + `ScanFacade`

`FolderScanner` берёт `contentResolver`+ treeUri, строит `List<FsNode>` через `DocumentFile.fromTreeUri`, отдает `BookClassifier`. `ScanSettings` (DataStore) хранит treeUri строкой. `ScanFacade.scanNow(treeUri)`: скан → для каждого дескриптора: upsert Book + chapters (через `BookRepository`), прочитать теги/обогатить, обновить книгу (уважая `manuallyEdited`). Дедуп: если книга с таким `sourceUri` уже есть — пропуск.

- [ ] интеграционный smoke (Robolectric): создать временную SAF-дерево через `ShadowDocumentFile`/`ContentProvider` сложно — поэтому `ScanFacade` тестируем через фейковый `FolderScanner` интерфейс, возвращающий заданные дескрипторы, и in-memory Room: проверка, что книги создаются и дедуп работает.
- [ ] реализации; коммит `feat(scan): folder scanner, settings, scan facade`.

### Task 8: Финальный прогон + DoD

- [ ] `./gradlew assembleDebug` + `testDebugUnitTest` — все зелёные.
- [ ] коммит-маркер не нужен.

## Self-Review

Покрытие спеки (Подплан 2): автоскан + эвристика (Task 1,7), конвейер теги→главы→онлайн→ручное (Tasks 1–7), `manuallyEdited` защита (Task 7), кэш метаданных (Task 5), OpenLibrary/Google Books (Task 4), производительность/WorkManager (WorkManager отложен в Подплан 4 — отмечено). Плейсхолдеров нет.
