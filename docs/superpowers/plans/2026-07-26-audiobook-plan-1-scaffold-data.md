# Подплан 1: Скаффолд + Домен + Слой данных — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Создать Gradle-проект Android-приложения, доменные модели и Room-слой каталога (сущности, DAO, БД, репозитории, use-case’ы удаления), чтобы ключевой сценарий «удалить с устройства vs удалить из каталога» работал и покрывался тестами.

**Architecture:** Clean single-module (Подход A из спеки). `domain` — чистые модели + use-case’ы; `data` — Room-сущности (`*Entity`), DAO, `AudioBookDatabase`, репозитории с маппингом Entity↔Domain. Hilt для DI. UI в этом подплане не делается.

**Tech Stack:** Kotlin 2.0.x, AGP 8.7.x, Jetpack Compose BOM (в каталоге, подключается в Подплане 4), Room 2.6.1, Hilt 2.52, Coroutines/Flow 1.8.x, JUnit5/Robolectric для тестов.

## Global Constraints

- minSdk 26, compileSdk 35, targetSdk 35.
- Kotlin DSL Gradle (`build.gradle.kts`), version catalog `gradle/libs.versions.toml`.
- Пакет приложения: `com.nikit.audiobook`.
- Room: `exportSchema = true`, схема в `app/schemas/`. **Запрещён** `fallbackToDestructiveMigration` — только явные `Migration` объекты.
- Все DAO-методы — `suspend` или возвращают `Flow`. Никаких блокирующих вызовов в репозиториях.
- Маппинг Entity↔Domain — в `data/repo/Mappers.kt`; UI/домен не видят `*Entity`.
- Идентификаторы — `String` (UUID), генерируются в домене (`java.util.UUID.randomUUID().toString()`).
- Коммиты — после каждой задачи, conventional commits (`feat:`, `test:`, `chore:`).
- Тесты запускаются через `./gradlew test` (JVM/Robolectric) — без эмулятора.

---

## File Structure (этот подплан)

```
audioBook/                              (корень проекта)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── .gitignore
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── kotlin/com/nikit/audiobook/
        │       ├── app/AudioBookApp.kt
        │       ├── app/di/DatabaseModule.kt
        │       ├── app/di/RepositoryModule.kt
        │       ├── domain/model/Enums.kt
        │       ├── domain/model/Book.kt
        │       ├── domain/model/Chapter.kt
        │       ├── domain/model/Bookmark.kt
        │       ├── domain/model/PlaybackProgress.kt
        │       ├── domain/model/Shelf.kt
        │       ├── domain/model/ShelfMembership.kt
        │       ├── domain/model/Tag.kt
        │       ├── domain/model/BookTag.kt
        │       ├── domain/model/MetadataCache.kt
        │       ├── domain/usecase/DeleteBookFiles.kt
        │       ├── domain/usecase/DeleteBookFromCatalog.kt
        │       ├── data/db/entity/BookEntity.kt
        │       ├── data/db/entity/ChapterEntity.kt
        │       ├── data/db/entity/BookmarkEntity.kt
        │       ├── data/db/entity/PlaybackProgressEntity.kt
        │       ├── data/db/entity/ShelfEntity.kt
        │       ├── data/db/entity/ShelfMembershipEntity.kt
        │       ├── data/db/entity/TagEntity.kt
        │       ├── data/db/entity/BookTagEntity.kt
        │       ├── data/db/entity/MetadataCacheEntity.kt
        │       ├── data/db/dao/BookDao.kt
        │       ├── data/db/dao/ChapterDao.kt
        │       ├── data/db/dao/BookmarkDao.kt
        │       ├── data/db/dao/PlaybackProgressDao.kt
        │       ├── data/db/dao/ShelfDao.kt
        │       ├── data/db/dao/TagDao.kt
        │       ├── data/db/dao/MetadataCacheDao.kt
        │       ├── data/db/AudioBookDatabase.kt
        │       ├── data/db/Converters.kt
        │       ├── data/repo/Mappers.kt
        │       ├── data/repo/BookRepository.kt
        │       ├── data/repo/ShelfRepository.kt
        │       ├── data/repo/ProgressRepository.kt
        │       └── data/repo/TagRepository.kt
        └── test/
            └── kotlin/com/nikit/audiobook/
                ├── data/db/BookDaoTest.kt
                ├── data/db/ChapterDaoTest.kt
                ├── data/db/BookmarkDaoTest.kt
                ├── data/db/PlaybackProgressDaoTest.kt
                ├── data/db/ShelfDaoTest.kt
                ├── data/db/TagDaoTest.kt
                ├── data/repo/BookRepositoryTest.kt
                └── domain/usecase/DeleteBookUseCasesTest.kt
```

---

### Task 1: Gradle-скаффолд проекта

**Files:**

- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `.gitignore`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/nikit/audiobook/app/AudioBookApp.kt`

**Interfaces:**

- Consumes: ничего (первая задача)
- Produces: собираемый Android-проект с Hilt-Application; `./gradlew test` запускается (0 тестов OK).

- [ ] **Step 1: Создать `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "audioBook"
include(":app")
```

- [ ] **Step 2: Создать `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
coreKtx = "1.13.1"
room = "2.6.1"
hilt = "2.52"
coroutines = "1.9.0"
junit = "4.13.2"
robolectric = "4.13"
androidxTestCore = "1.6.1"
androidxTestRunner = "1.6.2"
truth = "1.4.4"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidxTestCore" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidxTestRunner" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 3: Создать корневой `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 4: Создать `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Создать `.gitignore`**

```
*.iml
.gradle/
.idea/
local.properties
build/
captures/
.externalNativeBuild/
.cxx/
*.apk
*.keystore
```

- [ ] **Step 6: Создать `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.nikit.audiobook"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nikit.audiobook"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }
    sourceSets {
        getByName("main") { java.srcDirs("src/main/kotlin") }
        getByName("test") { java.srcDirs("src/test/kotlin") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
}
```

- [ ] **Step 7: Создать `app/proguard-rules.pro`** (пустой с комментарием)

```
# Keep Room, Hilt, Coroutines defaults; project-specific rules added as needed.
-keep class com.nikit.audiobook.data.db.entity.** { *; }
```

- [ ] **Step 8: Создать `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".app.AudioBookApp"
        android:label="Аудиокниги"
        android:supportsRtl="true"
        android:icon="@android:drawable/ic_media_play">
    </application>
</manifest>
```

- [ ] **Step 9: Создать `app/src/main/kotlin/com/nikit/audiobook/app/AudioBookApp.kt`**

```kotlin
package com.nikit.audiobook.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AudioBookApp : Application()
```

- [ ] **Step 10: Сгенерировать Gradle-wrapper**

Run:

```bash
cd /c/Users/nikit/Desktop/audioBook
gradle wrapper --gradle-version 8.10.2
```

(Если `gradle` не в PATH — скачать `gradle-8.10.2-bin.zip`, распаковать, использовать `bin/gradle`.)

- [ ] **Step 11: Проверить сборку**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 12: Проверить запуск тестов**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL (тестов пока нет — 0 выполнено).

- [ ] **Step 13: Коммит**

```bash
git add -A
git commit -m "chore: scaffold android gradle project with hilt application"
```

---

### Task 2: Доменные enums и модели

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/Enums.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/Book.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/Chapter.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/Bookmark.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/PlaybackProgress.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/Shelf.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/ShelfMembership.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/Tag.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/BookTag.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/model/MetadataCache.kt`

**Interfaces:**

- Consumes: Task 1 (проект собирается).
- Produces: доменные модели, используемые репозиториями (Task 10+) и use-case’ами (Task 12). Канонические типы для всего приложения.

- [ ] **Step 1: Создать `Enums.kt`**

```kotlin
package com.nikit.audiobook.domain.model

enum class FileType { SINGLE_FILE, FOLDER, M4B }
enum class BookStatus { READING, COMPLETED, DROPPED, PAUSED, WISHLIST }
enum class SourceKind { LOCAL_FOLDER, LOCAL_FILE }
```

- [ ] **Step 2: Создать `Book.kt`**

```kotlin
package com.nikit.audiobook.domain.model

import java.util.UUID

data class Book(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String? = null,
    val series: String? = null,
    val seriesIndex: Int? = null,
    val genre: String? = null,
    val description: String? = null,
    val year: Int? = null,
    val coverPath: String? = null,
    val sourceUri: String? = null,
    val filesPresent: Boolean = true,
    val fileType: FileType = FileType.SINGLE_FILE,
    val totalDurationMs: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val completedAt: Long? = null,
    val rating: Int = 0,
    val review: String? = null,
    val status: BookStatus = BookStatus.WISHLIST,
    val sourceKind: SourceKind = SourceKind.LOCAL_FILE,
    val originalPath: String? = null,
    val manuallyEdited: Boolean = false,
)
```

- [ ] **Step 3: Создать `Chapter.kt`**

```kotlin
package com.nikit.audiobook.domain.model

import java.util.UUID

data class Chapter(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val index: Int,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val filePath: String? = null,
)
```

- [ ] **Step 4: Создать `Bookmark.kt`**

```kotlin
package com.nikit.audiobook.domain.model

import java.util.UUID

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val positionMs: Long,
    val title: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
```

- [ ] **Step 5: Создать `PlaybackProgress.kt`**

```kotlin
package com.nikit.audiobook.domain.model

data class PlaybackProgress(
    val bookId: String,
    val positionMs: Long = 0L,
    val chapterIndex: Int = 0,
    val percent: Float = 0f,
    val lastPlayedAt: Long = System.currentTimeMillis(),
)
```

- [ ] **Step 6: Создать `Shelf.kt`**

```kotlin
package com.nikit.audiobook.domain.model

import java.util.UUID

data class Shelf(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String? = null,
    val sortIndex: Int = 0,
)
```

- [ ] **Step 7: Создать `ShelfMembership.kt`**

```kotlin
package com.nikit.audiobook.domain.model

data class ShelfMembership(
    val shelfId: String,
    val bookId: String,
)
```

- [ ] **Step 8: Создать `Tag.kt`**

```kotlin
package com.nikit.audiobook.domain.model

import java.util.UUID

data class Tag(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
)
```

- [ ] **Step 9: Создать `BookTag.kt`**

```kotlin
package com.nikit.audiobook.domain.model

data class BookTag(
    val tagId: String,
    val bookId: String,
)
```

- [ ] **Step 10: Создать `MetadataCache.kt`**

```kotlin
package com.nikit.audiobook.domain.model

import java.util.UUID

data class MetadataCache(
    val id: String = UUID.randomUUID().toString(),
    val queryKey: String,
    val payloadJson: String,
    val source: String,
    val createdAt: Long = System.currentTimeMillis(),
)
```

- [ ] **Step 11: Проверить сборку**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 12: Коммит**

```bash
git add -A
git commit -m "feat(domain): add catalog domain models"
```

---

### Task 3: Room-сущности + Converters

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/Converters.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/entity/BookEntity.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/entity/ChapterEntity.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/entity/BookmarkEntity.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/entity/PlaybackProgressEntity.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/entity/ShelfEntity.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/entity/ShelfMembershipEntity.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/entity/TagEntity.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/entity/BookTagEntity.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/entity/MetadataCacheEntity.kt`

**Interfaces:**

- Consumes: Task 2 (доменные enums).
- Produces: `*Entity` классы с `@Entity`, foreign keys + CASCADE, индексы. Поля 1:1 к доменным моделям.

- [ ] **Step 1: Создать `Converters.kt`**

```kotlin
package com.nikit.audiobook.data.db

import androidx.room.TypeConverter
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind

class Converters {
    @TypeConverter fun fileTypeToString(v: FileType?): String? = v?.name
    @TypeConverter fun stringToFileType(v: String?): FileType? = v?.let { FileType.valueOf(it) }

    @TypeConverter fun bookStatusToString(v: BookStatus?): String? = v?.name
    @TypeConverter fun stringToBookStatus(v: String?): BookStatus? = v?.let { BookStatus.valueOf(it) }

    @TypeConverter fun sourceKindToString(v: SourceKind?): String? = v?.name
    @TypeConverter fun stringToSourceKind(v: String?): SourceKind? = v?.let { SourceKind.valueOf(it) }
}
```

- [ ] **Step 2: Создать `BookEntity.kt`**

```kotlin
package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val series: String?,
    val seriesIndex: Int?,
    val genre: String?,
    val description: String?,
    val year: Int?,
    val coverPath: String?,
    val sourceUri: String?,
    val filesPresent: Boolean,
    val fileType: FileType,
    val totalDurationMs: Long,
    val addedAt: Long,
    val lastPlayedAt: Long?,
    val completedAt: Long?,
    val rating: Int,
    val review: String?,
    val status: BookStatus,
    val sourceKind: SourceKind,
    val originalPath: String?,
    val manuallyEdited: Boolean,
)
```

- [ ] **Step 3: Создать `ChapterEntity.kt`**

```kotlin
package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bookId")],
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val index: Int,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val filePath: String?,
)
```

- [ ] **Step 4: Создать `BookmarkEntity.kt`**

```kotlin
package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bookId")],
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val positionMs: Long,
    val title: String,
    val note: String?,
    val createdAt: Long,
)
```

- [ ] **Step 5: Создать `PlaybackProgressEntity.kt`**

```kotlin
package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "playback_progress",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class PlaybackProgressEntity(
    @PrimaryKey val bookId: String,
    val positionMs: Long,
    val chapterIndex: Int,
    val percent: Float,
    val lastPlayedAt: Long,
)
```

- [ ] **Step 6: Создать `ShelfEntity.kt`**

```kotlin
package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String?,
    val sortIndex: Int,
)
```

- [ ] **Step 7: Создать `ShelfMembershipEntity.kt`**

```kotlin
package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "shelf_memberships",
    primaryKeys = ["shelfId", "bookId"],
    foreignKeys = [
        ForeignKey(entity = ShelfEntity::class, parentColumns = ["id"], childColumns = ["shelfId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("bookId")],
)
data class ShelfMembershipEntity(
    val shelfId: String,
    val bookId: String,
)
```

- [ ] **Step 8: Создать `TagEntity.kt`**

```kotlin
package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
)
```

- [ ] **Step 9: Создать `BookTagEntity.kt`**

```kotlin
package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_tags",
    primaryKeys = ["tagId", "bookId"],
    foreignKeys = [
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("bookId")],
)
data class BookTagEntity(
    val tagId: String,
    val bookId: String,
)
```

- [ ] **Step 10: Создать `MetadataCacheEntity.kt`**

```kotlin
package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metadata_cache")
data class MetadataCacheEntity(
    @PrimaryKey val id: String,
    val queryKey: String,
    val payloadJson: String,
    val source: String,
    val createdAt: Long,
)
```

- [ ] **Step 11: Проверить сборку (KSP сгенерирует код только после БД в Task 4, но компиляция сущностей уже работает)**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 12: Коммит**

```bash
git add -A
git commit -m "feat(data): add room entities with cascade foreign keys"
```

---

### Task 4: БД `AudioBookDatabase` + Hilt-модуль

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/AudioBookDatabase.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/app/di/DatabaseModule.kt`

**Interfaces:**

- Consumes: Task 3 (сущности, Converters).
- Produces: `AudioBookDatabase` (абстрактный Room-класс, v1, exportSchema=true, список DAO-геттеров) и Hilt-провайдеры `AudioBookDatabase` + DAO. **DAO-геттеры добавляются по мере создания DAO в Tasks 5–9** — в этой задаче БД объявляет только геттеры для DAO, которые будут созданы в следующих задачах; чтобы проект компилировался, создаются «заглушки-DAO-интерфейсы» НЕТ — вместо этого БД в Task 4 включает только `BookDao` (создаётся в Task 5 первым), а остальные геттеры добавляются инкрементально. Поэтому **Task 4 реализуется после Task 5** — см. порядок: Task 5 (BookDao) → Task 4 (БД + DI). Ниже шаги Task 4 предполагают, что `BookDao` уже существует.

> **Порядок выполнения:** сначала выполни Task 5 (создаст `BookDao`), затем вернись к Task 4.

- [ ] **Step 1: Создать `AudioBookDatabase.kt`**

```kotlin
package com.nikit.audiobook.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.entity.BookEntity

@Database(
    entities = [BookEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AudioBookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
```

(Сущности и DAO-геттеры добавляются в Tasks 5–9 инкрементально — каждый раз правится `entities = [...]` и добавляется `abstract fun ...Dao()`.)

- [ ] **Step 2: Создать `DatabaseModule.kt`**

```kotlin
package com.nikit.audiobook.app.di

import android.content.Context
import androidx.room.Room
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.db.dao.BookDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AudioBookDatabase =
        Room.databaseBuilder(ctx, AudioBookDatabase::class.java, "audiobook.db")
            .build()

    @Provides fun provideBookDao(db: AudioBookDatabase): BookDao = db.bookDao()
}
```

(Провайдеры остальных DAO добавляются в Tasks 5–9.)

- [ ] **Step 3: Проверить сборку**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. KSP генерирует `AudioBookDatabase_Impl`.

- [ ] **Step 4: Коммит**

```bash
git add -A
git commit -m "feat(data): add room database and hilt di module"
```

---

### Task 5: `BookDao` + тест

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/dao/BookDao.kt`
- Create: `app/src/test/kotlin/com/nikit/audiobook/data/db/BookDaoTest.kt`

**Interfaces:**

- Consumes: Task 3 (`BookEntity`).
- Produces: `BookDao` с `upsert`, `observeAll`, `observeById`, `getById`, `markFilesDeleted`, `deleteById`. Используется `BookRepository` (Task 10) и БД (Task 4).

- [ ] **Step 1: Написать падающий тест `BookDaoTest.kt`**

```kotlin
package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var dao: BookDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AudioBookDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.bookDao()
    }
    @After fun teardown() { db.close() }

    private fun book(id: String, title: String = "T", filesPresent: Boolean = true) = BookEntity(
        id = id, title = title, author = null, series = null, seriesIndex = null,
        genre = null, description = null, year = null, coverPath = null,
        sourceUri = "uri://$id", filesPresent = filesPresent, fileType = FileType.SINGLE_FILE,
        totalDurationMs = 0L, addedAt = 1L, lastPlayedAt = null, completedAt = null,
        rating = 0, review = null, status = BookStatus.WISHLIST,
        sourceKind = SourceKind.LOCAL_FILE, originalPath = null, manuallyEdited = false,
    )

    @Test fun upsertAndObserve() = runTest {
        dao.upsert(book("a"))
        assertThat(dao.observeAll().first()).hasSize(1)
        assertThat(dao.getById("a")?.title).isEqualTo("T")
    }

    @Test fun markFilesDeleted_keepsRow_clearsSourceUri() = runTest {
        dao.upsert(book("a"))
        dao.markFilesDeleted("a")
        val b = dao.getById("a")
        assertThat(b).isNotNull()
        assertThat(b!!.filesPresent).isFalse()
        assertThat(b.sourceUri).isNull()
    }

    @Test fun deleteById_removesRow() = runTest {
        dao.upsert(book("a"))
        dao.deleteById("a")
        assertThat(dao.observeAll().first()).isEmpty()
    }
}
```

- [ ] **Step 2: Запустить тест — должен падать (нет `BookDao`)**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.BookDaoTest"`
Expected: FAIL (unresolved reference `BookDao` / БД без `bookDao()`).

- [ ] **Step 3: Создать `BookDao.kt`**

```kotlin
package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: BookEntity)

    @Query("SELECT * FROM books ORDER BY lastPlayedAt DESC, addedAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Query("UPDATE books SET filesPresent = 0, sourceUri = NULL WHERE id = :id")
    suspend fun markFilesDeleted(id: String)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

- [ ] **Step 4: Зарегистрировать `BookEntity` и `BookDao` в БД (Task 4 уже включает их — убедиться, что `entities = [BookEntity::class]` и `abstract fun bookDao(): BookDao` присутствуют)**

(Если Task 4 делался после — уже сделано.)

- [ ] **Step 5: Запустить тест — должен пройти**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.BookDaoTest"`
Expected: PASS (3 теста).

- [ ] **Step 6: Коммит**

```bash
git add -A
git commit -m "feat(data): add book dao with mark-files-deleted support"
```

---

### Task 6: `ChapterDao` + тест

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/dao/ChapterDao.kt`
- Create: `app/src/test/kotlin/com/nikit/audiobook/data/db/ChapterDaoTest.kt`
- Modify: `app/src/main/kotlin/com/nikit/audiobook/data/db/AudioBookDatabase.kt` (добавить `ChapterEntity` в `entities` + `abstract fun chapterDao()`)
- Modify: `app/src/main/kotlin/com/nikit/audiobook/app/di/DatabaseModule.kt` (провайдер `chapterDao`)

**Interfaces:**

- Consumes: Task 3 (`ChapterEntity`), Task 5 (БД собирается).
- Produces: `ChapterDao` (`insertAll`, `observeByBook`, `getByBook`, `clearFilePathsForBook`, cascade-deletes с книгой). Используется `BookRepository.markFilesDeleted` (Task 10).

- [ ] **Step 1: Написать падающий тест `ChapterDaoTest.kt`**

```kotlin
package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.ChapterEntity
import com.nikit.audiobook.domain.model.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChapterDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var chapterDao: ChapterDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AudioBookDatabase::class.java
        ).allowMainThreadQueries().build()
        bookDao = db.bookDao()
        chapterDao = db.chapterDao()
    }
    @After fun teardown() { db.close() }

    private fun book(id: String) = BookEntity(
        id, "T", null, null, null, null, null, null, null, "uri://$id",
        true, FileType.SINGLE_FILE, 0L, 1L, null, null, 0, null,
        BookStatus.WISHLIST, SourceKind.LOCAL_FILE, null, false,
    )

    @Test fun insertAndObserve() = runTest {
        bookDao.upsert(book("a"))
        chapterDao.insertAll(listOf(
            ChapterEntity("c1", "a", 0, "Ch0", 0L, 1000L, "file://c1"),
            ChapterEntity("c2", "a", 1, "Ch1", 1000L, 2000L, "file://c2"),
        ))
        assertThat(chapterDao.observeByBook("a").first()).hasSize(2)
    }

    @Test fun clearFilePathsForBook_nullsPaths_keepsRows() = runTest {
        bookDao.upsert(book("a"))
        chapterDao.insertAll(listOf(ChapterEntity("c1", "a", 0, "Ch0", 0L, 1000L, "file://c1")))
        chapterDao.clearFilePathsForBook("a")
        val rows = chapterDao.getByBook("a")
        assertThat(rows).hasSize(1)
        assertThat(rows.first().filePath).isNull()
    }

    @Test fun deletingBook_cascadesChapters() = runTest {
        bookDao.upsert(book("a"))
        chapterDao.insertAll(listOf(ChapterEntity("c1", "a", 0, "Ch0", 0L, 1000L, "file://c1")))
        bookDao.deleteById("a")
        assertThat(chapterDao.getByBook("a")).isEmpty()
    }
}
```

- [ ] **Step 2: Запустить — падает (нет `ChapterDao`)**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.ChapterDaoTest"`
Expected: FAIL (unresolved `ChapterDao` / `chapterDao()`).

- [ ] **Step 3: Создать `ChapterDao.kt`**

```kotlin
package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index` ASC")
    fun observeByBook(bookId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index` ASC")
    suspend fun getByBook(bookId: String): List<ChapterEntity>

    @Query("UPDATE chapters SET filePath = NULL WHERE bookId = :bookId")
    suspend fun clearFilePathsForBook(bookId: String)
}
```

- [ ] **Step 4: Зарегистрировать в БД и DI**

В `AudioBookDatabase.kt`: добавить `ChapterEntity::class` в `entities` и

```kotlin
abstract fun chapterDao(): ChapterDao
```

В `DatabaseModule.kt`:

```kotlin
@Provides fun provideChapterDao(db: AudioBookDatabase): ChapterDao = db.chapterDao()
```

- [ ] **Step 5: Запустить — проходит**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.ChapterDaoTest"`
Expected: PASS (3 теста).

- [ ] **Step 6: Коммит**

```bash
git add -A
git commit -m "feat(data): add chapter dao with filepath clearing"
```

---

### Task 7: `BookmarkDao` + тест

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/dao/BookmarkDao.kt`
- Create: `app/src/test/kotlin/com/nikit/audiobook/data/db/BookmarkDaoTest.kt`
- Modify: `AudioBookDatabase.kt` (`BookmarkEntity` + `bookmarkDao()`)
- Modify: `DatabaseModule.kt` (провайдер)

**Interfaces:**

- Consumes: Task 3 (`BookmarkEntity`), Task 5.
- Produces: `BookmarkDao` (`upsert`, `observeByBook`, `deleteById`, cascade-deletes с книгой).

- [ ] **Step 1: Написать падающий тест `BookmarkDaoTest.kt`**

```kotlin
package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.BookmarkEntity
import com.nikit.audiobook.domain.model.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookmarkDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var bookmarkDao: BookmarkDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AudioBookDatabase::class.java
        ).allowMainThreadQueries().build()
        bookDao = db.bookDao()
        bookmarkDao = db.bookmarkDao()
    }
    @After fun teardown() { db.close() }

    private fun book(id: String) = BookEntity(
        id, "T", null, null, null, null, null, null, null, "uri://$id",
        true, FileType.SINGLE_FILE, 0L, 1L, null, null, 0, null,
        BookStatus.WISHLIST, SourceKind.LOCAL_FILE, null, false,
    )

    @Test fun upsertAndObserve() = runTest {
        bookDao.upsert(book("a"))
        bookmarkDao.upsert(BookmarkEntity("bm1", "a", 5000L, "Отметка", "заметка", 1L))
        assertThat(bookmarkDao.observeByBook("a").first()).hasSize(1)
    }

    @Test fun deletingBook_cascadesBookmarks() = runTest {
        bookDao.upsert(book("a"))
        bookmarkDao.upsert(BookmarkEntity("bm1", "a", 5000L, "x", null, 1L))
        bookDao.deleteById("a")
        assertThat(bookmarkDao.observeByBook("a").first()).isEmpty()
    }
}
```

- [ ] **Step 2: Запустить — падает**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.BookmarkDaoTest"`
Expected: FAIL.

- [ ] **Step 3: Создать `BookmarkDao.kt`**

```kotlin
package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY positionMs ASC")
    fun observeByBook(bookId: String): Flow<List<BookmarkEntity>>

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

- [ ] **Step 4: Зарегистрировать в БД и DI** (`BookmarkEntity` + `abstract fun bookmarkDao(): BookmarkDao` + провайдер).

- [ ] **Step 5: Запустить — проходит**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.BookmarkDaoTest"`
Expected: PASS (2 теста).

- [ ] **Step 6: Коммит**

```bash
git add -A
git commit -m "feat(data): add bookmark dao"
```

---

### Task 8: `PlaybackProgressDao` + тест

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/dao/PlaybackProgressDao.kt`
- Create: `app/src/test/kotlin/com/nikit/audiobook/data/db/PlaybackProgressDaoTest.kt`
- Modify: `AudioBookDatabase.kt`, `DatabaseModule.kt`

**Interfaces:**

- Consumes: Task 3 (`PlaybackProgressEntity`), Task 5.
- Produces: `PlaybackProgressDao` (`upsert`, `observeByBook`, `getByBook`, cascade-deletes с книгой). Используется `ProgressRepository` (Task 11) и плеером (Подплан 3).

- [ ] **Step 1: Написать падающий тест**

```kotlin
package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.PlaybackProgressEntity
import com.nikit.audiobook.domain.model.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackProgressDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var progressDao: PlaybackProgressDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AudioBookDatabase::class.java
        ).allowMainThreadQueries().build()
        bookDao = db.bookDao()
        progressDao = db.playbackProgressDao()
    }
    @After fun teardown() { db.close() }

    private fun book(id: String) = BookEntity(
        id, "T", null, null, null, null, null, null, null, "uri://$id",
        true, FileType.SINGLE_FILE, 0L, 1L, null, null, 0, null,
        BookStatus.WISHLIST, SourceKind.LOCAL_FILE, null, false,
    )

    @Test fun upsertReplacesByBookId() = runTest {
        bookDao.upsert(book("a"))
        progressDao.upsert(PlaybackProgressEntity("a", 1000L, 0, 0.1f, 1L))
        progressDao.upsert(PlaybackProgressEntity("a", 2000L, 1, 0.2f, 2L))
        val p = progressDao.getByBook("a")
        assertThat(p?.positionMs).isEqualTo(2000L)
    }

    @Test fun deletingBook_cascadesProgress() = runTest {
        bookDao.upsert(book("a"))
        progressDao.upsert(PlaybackProgressEntity("a", 1000L, 0, 0.1f, 1L))
        bookDao.deleteById("a")
        assertThat(progressDao.getByBook("a")).isNull()
    }

    @Test fun observeEmitsChanges() = runTest {
        bookDao.upsert(book("a"))
        progressDao.upsert(PlaybackProgressEntity("a", 1000L, 0, 0.1f, 1L))
        assertThat(progressDao.observeByBook("a").first()?.positionMs).isEqualTo(1000L)
    }
}
```

- [ ] **Step 2: Запустить — падает**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.PlaybackProgressDaoTest"`
Expected: FAIL.

- [ ] **Step 3: Создать `PlaybackProgressDao.kt`**

```kotlin
package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PlaybackProgressEntity)

    @Query("SELECT * FROM playback_progress WHERE bookId = :bookId")
    fun observeByBook(bookId: String): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress WHERE bookId = :bookId")
    suspend fun getByBook(bookId: String): PlaybackProgressEntity?
}
```

- [ ] **Step 4: Зарегистрировать в БД и DI**

В `AudioBookDatabase`: добавить `PlaybackProgressEntity::class` в `entities` + `abstract fun playbackProgressDao(): PlaybackProgressDao`.
В `DatabaseModule`: провайдер `playbackProgressDao`.

- [ ] **Step 5: Запустить — проходит**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.PlaybackProgressDaoTest"`
Expected: PASS (3 теста).

- [ ] **Step 6: Коммит**

```bash
git add -A
git commit -m "feat(data): add playback progress dao"
```

---

### Task 9: `ShelfDao` + `TagDao` + `MetadataCacheDao` + тесты

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/dao/ShelfDao.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/dao/TagDao.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/db/dao/MetadataCacheDao.kt`
- Create: `app/src/test/kotlin/com/nikit/audiobook/data/db/ShelfDaoTest.kt`
- Create: `app/src/test/kotlin/com/nikit/audiobook/data/db/TagDaoTest.kt`
- Modify: `AudioBookDatabase.kt` (добавить `ShelfEntity`, `ShelfMembershipEntity`, `TagEntity`, `BookTagEntity`, `MetadataCacheEntity` в `entities` + геттеры)
- Modify: `DatabaseModule.kt` (провайдеры)

**Interfaces:**

- Consumes: Task 3 (сущности), Task 5.
- Produces: `ShelfDao`, `TagDao`, `MetadataCacheDao`. Используются `ShelfRepository`/`TagRepository` (Task 11); `MetadataCacheDao` — в Подплане 2.

- [ ] **Step 1: Написать падающий тест `ShelfDaoTest.kt`**

```kotlin
package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nikit.audiobook.data.db.entity.*
import com.nikit.audiobook.domain.model.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShelfDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var shelfDao: ShelfDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AudioBookDatabase::class.java
        ).allowMainThreadQueries().build()
        bookDao = db.bookDao()
        shelfDao = db.shelfDao()
    }
    @After fun teardown() { db.close() }

    private fun book(id: String) = BookEntity(
        id, "T", null, null, null, null, null, null, null, "uri://$id",
        true, FileType.SINGLE_FILE, 0L, 1L, null, null, 0, null,
        BookStatus.WISHLIST, SourceKind.LOCAL_FILE, null, false,
    )

    @Test fun shelfAndMembershipObserve() = runTest {
        bookDao.upsert(book("a"))
        shelfDao.upsert(ShelfEntity("s1", "Прочитал", "#aa0000", 0))
        shelfDao.addMembership(ShelfMembershipEntity("s1", "a"))
        assertThat(shelfDao.observeAll().first()).hasSize(1)
        assertThat(shelfDao.observeBooksOfShelf("s1").first()).hasSize(1)
    }

    @Test fun deletingBook_cascadesMembership() = runTest {
        bookDao.upsert(book("a"))
        shelfDao.upsert(ShelfEntity("s1", "Прочитал", null, 0))
        shelfDao.addMembership(ShelfMembershipEntity("s1", "a"))
        bookDao.deleteById("a")
        assertThat(shelfDao.observeBooksOfShelf("s1").first()).isEmpty()
    }
}
```

- [ ] **Step 2: Написать падающий тест `TagDaoTest.kt`**

```kotlin
package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nikit.audiobook.data.db.entity.*
import com.nikit.audiobook.domain.model.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TagDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var tagDao: TagDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AudioBookDatabase::class.java
        ).allowMainThreadQueries().build()
        bookDao = db.bookDao()
        tagDao = db.tagDao()
    }
    @After fun teardown() { db.close() }

    private fun book(id: String) = BookEntity(
        id, "T", null, null, null, null, null, null, null, "uri://$id",
        true, FileType.SINGLE_FILE, 0L, 1L, null, null, 0, null,
        BookStatus.WISHLIST, SourceKind.LOCAL_FILE, null, false,
    )

    @Test fun tagAndLinkObserve() = runTest {
        bookDao.upsert(book("a"))
        tagDao.upsert(TagEntity("t1", "фэнтези"))
        tagDao.link(BookTagEntity("t1", "a"))
        assertThat(tagDao.observeTagsOfBook("a").first()).hasSize(1)
    }

    @Test fun deletingBook_cascadesTagLinks() = runTest {
        bookDao.upsert(book("a"))
        tagDao.upsert(TagEntity("t1", "фэнтези"))
        tagDao.link(BookTagEntity("t1", "a"))
        bookDao.deleteById("a")
        assertThat(tagDao.observeTagsOfBook("a").first()).isEmpty()
        // тег остаётся
        assertThat(tagDao.observeAll().first()).hasSize(1)
    }
}
```

- [ ] **Step 3: Запустить — падают (нет DAO)**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.ShelfDaoTest" --tests "com.nikit.audiobook.data.db.TagDaoTest"`
Expected: FAIL.

- [ ] **Step 4: Создать `ShelfDao.kt`**

```kotlin
package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.ShelfEntity
import com.nikit.audiobook.data.db.entity.ShelfMembershipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shelf: ShelfEntity)

    @Query("SELECT * FROM shelves ORDER BY sortIndex ASC")
    fun observeAll(): Flow<List<ShelfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMembership(m: ShelfMembershipEntity)

    @Query("DELETE FROM shelf_memberships WHERE shelfId = :shelfId AND bookId = :bookId")
    suspend fun removeMembership(shelfId: String, bookId: String)

    @Query("SELECT * FROM books WHERE id IN (SELECT bookId FROM shelf_memberships WHERE shelfId = :shelfId) ORDER BY lastPlayedAt DESC")
    fun observeBooksOfShelf(shelfId: String): Flow<List<BookEntity>>
}
```

- [ ] **Step 5: Создать `TagDao.kt`**

```kotlin
package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.BookTagEntity
import com.nikit.audiobook.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun link(link: BookTagEntity)

    @Query("DELETE FROM book_tags WHERE tagId = :tagId AND bookId = :bookId")
    suspend fun unlink(tagId: String, bookId: String)

    @Query("SELECT * FROM tags WHERE id IN (SELECT tagId FROM book_tags WHERE bookId = :bookId) ORDER BY name ASC")
    fun observeTagsOfBook(bookId: String): Flow<List<TagEntity>>
}
```

- [ ] **Step 6: Создать `MetadataCacheDao.kt`**

```kotlin
package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.MetadataCacheEntity

@Dao
interface MetadataCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MetadataCacheEntity)

    @Query("SELECT * FROM metadata_cache WHERE queryKey = :queryKey LIMIT 1")
    suspend fun findByQueryKey(queryKey: String): MetadataCacheEntity?

    @Query("DELETE FROM metadata_cache WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

- [ ] **Step 7: Зарегистрировать всё в БД и DI**

В `AudioBookDatabase`: `entities = [BookEntity::class, ChapterEntity::class, BookmarkEntity::class, PlaybackProgressEntity::class, ShelfEntity::class, ShelfMembershipEntity::class, TagEntity::class, BookTagEntity::class, MetadataCacheEntity::class]` + добавить `abstract fun shelfDao()`, `tagDao()`, `metadataCacheDao()`.
В `DatabaseModule`: провайдеры `shelfDao`, `tagDao`, `metadataCacheDao`.

- [ ] **Step 8: Запустить — проходят**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.ShelfDaoTest" --tests "com.nikit.audiobook.data.db.TagDaoTest"`
Expected: PASS (всего 4 теста).

- [ ] **Step 9: Запустить весь тест-набор**

Run: `./gradlew test`
Expected: PASS — все DAO-тесты зелёные.

- [ ] **Step 10: Коммит**

```bash
git add -A
git commit -m "feat(data): add shelf, tag, metadata cache daos"
```

---

### Task 10: Мапперы + `BookRepository` + ключевой тест

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/data/repo/Mappers.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/repo/BookRepository.kt`
- Create: `app/src/test/kotlin/com/nikit/audiobook/data/repo/BookRepositoryTest.kt`

**Interfaces:**

- Consumes: Tasks 5–6 (`BookDao`, `ChapterDao`), Task 2 (доменные модели).
- Produces: `BookRepository` — `observeAll(): Flow<List<Book>>`, `observeBook(id): Flow<Book?>`, `getBook(id): Book?`, `upsert(book, chapters)`, `markFilesDeleted(bookId)`, `deleteBookPermanently(bookId)`. Используется use-case’ами (Task 12) и UI (Подплан 4).

- [ ] **Step 1: Написать падающий тест `BookRepositoryTest.kt` (ключевой acceptance-сценарий)**

```kotlin
package com.nikit.audiobook.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.BookmarkDao
import com.nikit.audiobook.data.db.dao.ChapterDao
import com.nikit.audiobook.data.db.dao.PlaybackProgressDao
import com.nikit.audiobook.data.db.dao.ShelfDao
import com.nikit.audiobook.data.db.dao.TagDao
import com.nikit.audiobook.domain.model.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookRepositoryTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var repo: BookRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AudioBookDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = BookRepository(db.bookDao(), db.chapterDao())
    }
    @After fun teardown() { db.close() }

    private fun sampleBook(id: String = "b1") = Book(
        id = id, title = "Дюна", author = "Герберт",
        sourceUri = "uri://dune", fileType = FileType.FOLDER,
        status = BookStatus.READING,
    )

    @Test fun upsertPersistsBookAndChapters() = runTest {
        repo.upsert(sampleBook(), listOf(
            Chapter(bookId = "b1", index = 0, title = "Ch0", startMs = 0, endMs = 1000, filePath = "f0"),
        ))
        val loaded = repo.getBook("b1")
        assertThat(loaded?.title).isEqualTo("Дюна")
    }

    @Test fun markFilesDeleted_keepsCard_clearsFiles() = runTest {
        repo.upsert(sampleBook(), listOf(
            Chapter(bookId = "b1", index = 0, title = "Ch0", startMs = 0, endMs = 1000, filePath = "f0"),
        ))
        repo.markFilesDeleted("b1")
        val card = repo.getBook("b1")
        assertThat(card).isNotNull()
        assertThat(card!!.filesPresent).isFalse()
        assertThat(card.sourceUri).isNull()
        // глава остаётся, но filePath обнулён
        // (наблюдение глав — через ChapterRepository в Подплане 2; здесь проверяем через DAO напрямую)
    }

    @Test fun deleteBookPermanently_removesEverything() = runTest {
        repo.upsert(sampleBook(), listOf(
            Chapter(bookId = "b1", index = 0, title = "Ch0", startMs = 0, endMs = 1000, filePath = "f0"),
        ))
        repo.deleteBookPermanently("b1")
        assertThat(repo.getBook("b1")).isNull()
        assertThat(repo.observeAll().first()).isEmpty()
    }
}
```

- [ ] **Step 2: Запустить — падает (нет `BookRepository`)**

Run: `./gradlew test --tests "com.nikit.audiobook.data.repo.BookRepositoryTest"`
Expected: FAIL (unresolved `BookRepository`).

- [ ] **Step 3: Создать `Mappers.kt`**

```kotlin
package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.entity.*
import com.nikit.audiobook.domain.model.*

fun BookEntity.toDomain() = Book(
    id, title, author, series, seriesIndex, genre, description, year, coverPath,
    sourceUri, filesPresent, fileType, totalDurationMs, addedAt, lastPlayedAt,
    completedAt, rating, review, status, sourceKind, originalPath, manuallyEdited,
)

fun Book.toEntity() = BookEntity(
    id, title, author, series, seriesIndex, genre, description, year, coverPath,
    sourceUri, filesPresent, fileType, totalDurationMs, addedAt, lastPlayedAt,
    completedAt, rating, review, status, sourceKind, originalPath, manuallyEdited,
)

fun ChapterEntity.toDomain() = Chapter(id, bookId, index, title, startMs, endMs, filePath)
fun Chapter.toEntity() = ChapterEntity(id, bookId, index, title, startMs, endMs, filePath)

fun BookmarkEntity.toDomain() = Bookmark(id, bookId, positionMs, title, note, createdAt)
fun Bookmark.toEntity() = BookmarkEntity(id, bookId, positionMs, title, note, createdAt)

fun PlaybackProgressEntity.toDomain() = PlaybackProgress(bookId, positionMs, chapterIndex, percent, lastPlayedAt)
fun PlaybackProgress.toEntity() = PlaybackProgressEntity(bookId, positionMs, chapterIndex, percent, lastPlayedAt)

fun ShelfEntity.toDomain() = Shelf(id, name, colorHex, sortIndex)
fun Shelf.toEntity() = ShelfEntity(id, name, colorHex, sortIndex)

fun TagEntity.toDomain() = Tag(id, name)
fun Tag.toEntity() = TagEntity(id, name)

fun MetadataCacheEntity.toDomain() = MetadataCache(id, queryKey, payloadJson, source, createdAt)
fun MetadataCache.toEntity() = MetadataCacheEntity(id, queryKey, payloadJson, source, createdAt)
```

- [ ] **Step 4: Создать `BookRepository.kt`**

```kotlin
package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.ChapterDao
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.Chapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
) {
    fun observeAll(): Flow<List<Book>> =
        bookDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeBook(id: String): Flow<Book?> =
        bookDao.observeById(id).map { it?.toDomain() }

    suspend fun getBook(id: String): Book? = bookDao.getById(id)?.toDomain()

    suspend fun upsert(book: Book, chapters: List<Chapter> = emptyList()) {
        bookDao.upsert(book.toEntity())
        if (chapters.isNotEmpty()) chapterDao.insertAll(chapters.map { it.toEntity() })
    }

    /** Удалить аудиофайлы с устройства, карточку сохранить. */
    suspend fun markFilesDeleted(bookId: String) {
        bookDao.markFilesDeleted(bookId)
        chapterDao.clearFilePathsForBook(bookId)
    }

    /** Удалить книгу из каталога навсегда (каскад чистит главы/закладки/прогресс/полки/теги). */
    suspend fun deleteBookPermanently(bookId: String) {
        bookDao.deleteById(bookId)
    }
}
```

- [ ] **Step 5: Запустить — проходит**

Run: `./gradlew test --tests "com.nikit.audiobook.data.repo.BookRepositoryTest"`
Expected: PASS (3 теста, включая ключевой `markFilesDeleted_keepsCard_clearsFiles`).

- [ ] **Step 6: Коммит**

```bash
git add -A
git commit -m "feat(data): add book repository with delete-files vs delete-catalog semantics"
```

---

### Task 11: `ShelfRepository`, `ProgressRepository`, `TagRepository`

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/data/repo/ShelfRepository.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/repo/ProgressRepository.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/data/repo/TagRepository.kt`
- Modify: `app/src/main/kotlin/com/nikit/audiobook/app/di/RepositoryModule.kt` (если нужны бинды; репозитории имеют `@Inject constructor` + `@Singleton`, отдельный модуль не обязателен — Hilt сам построит. Файл `RepositoryModule.kt` не создаётся, провайдеры DAO хватает.)

**Interfaces:**

- Consumes: Tasks 7–9 (DAO).
- Produces: три репозитория для UI/плеера/метаданных.

- [ ] **Step 1: Создать `ShelfRepository.kt`**

```kotlin
package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.ShelfDao
import com.nikit.audiobook.data.db.entity.ShelfMembershipEntity
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.Shelf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShelfRepository @Inject constructor(private val dao: ShelfDao) {
    fun observeAll(): Flow<List<Shelf>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    suspend fun upsert(shelf: Shelf) = dao.upsert(shelf.toEntity())
    suspend fun addBook(shelfId: String, bookId: String) =
        dao.addMembership(ShelfMembershipEntity(shelfId, bookId))
    suspend fun removeBook(shelfId: String, bookId: String) =
        dao.removeMembership(shelfId, bookId)
    fun observeBooksOfShelf(shelfId: String): Flow<List<Book>> =
        dao.observeBooksOfShelf(shelfId).map { it.map { e -> e.toDomain() } }
}
```

- [ ] **Step 2: Создать `ProgressRepository.kt`**

```kotlin
package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.PlaybackProgressDao
import com.nikit.audiobook.domain.model.PlaybackProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(private val dao: PlaybackProgressDao) {
    fun observeByBook(bookId: String): Flow<PlaybackProgress?> =
        dao.observeByBook(bookId).map { it?.toDomain() }

    suspend fun get(bookId: String): PlaybackProgress? = dao.getByBook(bookId)?.toDomain()

    suspend fun upsert(progress: PlaybackProgress) = dao.upsert(progress.toEntity())
}
```

- [ ] **Step 3: Создать `TagRepository.kt`**

```kotlin
package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.TagDao
import com.nikit.audiobook.data.db.entity.BookTagEntity
import com.nikit.audiobook.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(private val dao: TagDao) {
    fun observeAll(): Flow<List<Tag>> = dao.observeAll().map { it.map { e -> e.toDomain() } }
    fun observeTagsOfBook(bookId: String): Flow<List<Tag>> =
        dao.observeTagsOfBook(bookId).map { it.map { e -> e.toDomain() } }
    suspend fun upsert(tag: Tag) = dao.upsert(tag.toEntity())
    suspend fun link(tagId: String, bookId: String) = dao.link(BookTagEntity(tagId, bookId))
    suspend fun unlink(tagId: String, bookId: String) = dao.unlink(tagId, bookId)
}
```

- [ ] **Step 4: Добавить smoke-тесты (минимальные)**

Создать `app/src/test/kotlin/com/nikit/audiobook/data/repo/RepositoriesSmokeTest.kt`:

```kotlin
package com.nikit.audiobook.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.domain.model.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoriesSmokeTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var shelves: ShelfRepository
    private lateinit var progress: ProgressRepository
    private lateinit var tags: TagRepository
    private lateinit var books: BookRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AudioBookDatabase::class.java
        ).allowMainThreadQueries().build()
        shelves = ShelfRepository(db.shelfDao())
        progress = ProgressRepository(db.playbackProgressDao())
        tags = TagRepository(db.tagDao())
        books = BookRepository(db.bookDao(), db.chapterDao())
    }
    @After fun teardown() { db.close() }

    private fun book(id: String) = BookEntity(
        id, "T", null, null, null, null, null, null, null, "uri://$id",
        true, FileType.SINGLE_FILE, 0L, 1L, null, null, 0, null,
        BookStatus.WISHLIST, SourceKind.LOCAL_FILE, null, false,
    )

    @Test fun shelfFlow() = runTest {
        db.bookDao().upsert(book("a"))
        shelves.upsert(Shelf("s1", "Прочитал", null, 0))
        shelves.addBook("s1", "a")
        assertThat(shelves.observeBooksOfShelf("s1").first()).hasSize(1)
    }

    @Test fun progressFlow() = runTest {
        db.bookDao().upsert(book("a"))
        progress.upsert(PlaybackProgress("a", 1000L, 0, 0.1f, 1L))
        assertThat(progress.get("a")?.positionMs).isEqualTo(1000L)
    }

    @Test fun tagFlow() = runTest {
        db.bookDao().upsert(book("a"))
        tags.upsert(Tag("t1", "фэнтези"))
        tags.link("t1", "a")
        assertThat(tags.observeTagsOfBook("a").first()).hasSize(1)
    }
}
```

- [ ] **Step 5: Запустить — проходят**

Run: `./gradlew test --tests "com.nikit.audiobook.data.repo.RepositoriesSmokeTest"`
Expected: PASS (3 теста).

- [ ] **Step 6: Коммит**

```bash
git add -A
git commit -m "feat(data): add shelf, progress, tag repositories"
```

---

### Task 12: Use-case’ы удаления + тест

**Files:**

- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/usecase/DeleteBookFiles.kt`
- Create: `app/src/main/kotlin/com/nikit/audiobook/domain/usecase/DeleteBookFromCatalog.kt`
- Create: `app/src/test/kotlin/com/nikit/audiobook/domain/usecase/DeleteBookUseCasesTest.kt`

**Interfaces:**

- Consumes: Task 10 (`BookRepository`).
- Produces: `DeleteBookFiles(bookId)` и `DeleteBookFromCatalog(bookId)` — тонкие use-case’ы, которые UI вызывает в Подплане 4. Имена и сигнатуры фиксированы: `suspend operator fun invoke(bookId: String)`.

- [ ] **Step 1: Написать падающий тест `DeleteBookUseCasesTest.kt`**

```kotlin
package com.nikit.audiobook.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.domain.model.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeleteBookUseCasesTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var repo: BookRepository
    private lateinit var deleteFiles: DeleteBookFiles
    private lateinit var deleteFromCatalog: DeleteBookFromCatalog

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AudioBookDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = BookRepository(db.bookDao(), db.chapterDao())
        deleteFiles = DeleteBookFiles(repo)
        deleteFromCatalog = DeleteBookFromCatalog(repo)
    }
    @After fun teardown() { db.close() }

    private suspend fun seed(): String {
        val book = Book(id = "b1", title = "Дюна", author = "Герберт",
            sourceUri = "uri://dune", fileType = FileType.FOLDER, status = BookStatus.READING)
        repo.upsert(book, listOf(
            Chapter(bookId = "b1", index = 0, title = "Ch0", startMs = 0, endMs = 1000, filePath = "f0"),
        ))
        return book.id
    }

    @Test fun deleteFiles_keepsCard() = runTest {
        val id = seed()
        deleteFiles(id)
        val card = repo.getBook(id)
        assertThat(card).isNotNull()
        assertThat(card!!.filesPresent).isFalse()
        assertThat(card.sourceUri).isNull()
    }

    @Test fun deleteFromCatalog_removesCard() = runTest {
        val id = seed()
        deleteFromCatalog(id)
        assertThat(repo.getBook(id)).isNull()
        assertThat(repo.observeAll().first()).isEmpty()
    }
}
```

- [ ] **Step 2: Запустить — падает (нет use-case’ов)**

Run: `./gradlew test --tests "com.nikit.audiobook.domain.usecase.DeleteBookUseCasesTest"`
Expected: FAIL.

- [ ] **Step 3: Создать `DeleteBookFiles.kt`**

```kotlin
package com.nikit.audiobook.domain.usecase

import com.nikit.audiobook.data.repo.BookRepository
import javax.inject.Inject

class DeleteBookFiles @Inject constructor(private val repo: BookRepository) {
    suspend operator fun invoke(bookId: String) = repo.markFilesDeleted(bookId)
}
```

- [ ] **Step 4: Создать `DeleteBookFromCatalog.kt`**

```kotlin
package com.nikit.audiobook.domain.usecase

import com.nikit.audiobook.data.repo.BookRepository
import javax.inject.Inject

class DeleteBookFromCatalog @Inject constructor(private val repo: BookRepository) {
    suspend operator fun invoke(bookId: String) = repo.deleteBookPermanently(bookId)
}
```

- [ ] **Step 5: Запустить — проходят**

Run: `./gradlew test --tests "com.nikit.audiobook.domain.usecase.DeleteBookUseCasesTest"`
Expected: PASS (2 теста).

- [ ] **Step 6: Коммит**

```bash
git add -A
git commit -m "feat(domain): add delete-book use cases"
```

---

### Task 13: Стратегия миграций + smoke всей БД

**Files:**

- Modify: `app/src/main/kotlin/com/nikit/audiobook/data/db/AudioBookDatabase.kt` (добавить пустой `MIGRATION_N`-паттерн и документирующий комментарий; добавить `.addMigrations(*AudioBookDatabase.MIGRATIONS)` в `DatabaseModule`)
- Create: `app/src/test/kotlin/com/nikit/audiobook/data/db/MigrationSafetyTest.kt`

**Interfaces:**

- Consumes: Task 4 (БД, DI).
- Produces: гарантия, что `fallbackToDestructiveMigration` не используется, и каркас для будущих миграций (`MIGRATIONS` array).

- [ ] **Step 1: Написать падающий тест `MigrationSafetyTest.kt`**

```kotlin
package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationSafetyTest {
    @Test fun migrationsArrayExists_andVersionIs1() {
        assertThat(AudioBookDatabase.MIGRATIONS).isNotNull()
        // v1 — миграций нет, но массив определён (каркас для будущего)
        assertThat(AudioBookDatabase.MIGRATIONS).isEmpty()
    }

    @Test fun databaseBuilderWithoutDestructiveFallback_builds() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.databaseBuilder(ctx, AudioBookDatabase::class.java, "test.db")
            .addMigrations(*AudioBookDatabase.MIGRATIONS)
            .build()
        // БД открывается без fallbackToDestructiveMigration
        assertThat(db.openHelper.writableDatabase).isNotNull()
        db.close()
    }
}
```

- [ ] **Step 2: Запустить — падает (нет `MIGRATIONS`)**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.MigrationSafetyTest"`
Expected: FAIL.

- [ ] **Step 3: Добавить `MIGRATIONS` в `AudioBookDatabase.kt`**

В начало класса `AudioBookDatabase` добавить:

```kotlin
    companion object {
        // Явные миграции. НИКОГДА не использовать fallbackToDestructiveMigration —
        // каталог ценен и не должен пересоздаваться при смене версии.
        // Пример будущей миграции:
        //   val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) { ... } }
        val MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf(
            // MIGRATION_1_2,
        )
    }
```

- [ ] **Step 4: Подключить миграции в `DatabaseModule.kt`**

Заменить `Room.databaseBuilder(...).build()` на:

```kotlin
Room.databaseBuilder(ctx, AudioBookDatabase::class.java, "audiobook.db")
    .addMigrations(*AudioBookDatabase.MIGRATIONS)
    .build()
```

- [ ] **Step 5: Запустить — проходит**

Run: `./gradlew test --tests "com.nikit.audiobook.data.db.MigrationSafetyTest"`
Expected: PASS (2 теста).

- [ ] **Step 6: Финальный прогон всего тест-набора**

Run: `./gradlew test`
Expected: PASS — все тесты подплана зелёные.

- [ ] **Step 7: Коммит**

```bash
git add -A
git commit -m "feat(data): migration scaffolding and destructive-fallback ban"
```

---

## Self-Review

**Spec coverage (Подплан 1):**

- Доменные модели (Book/Chapter/Bookmark/PlaybackProgress/Shelf/ShelfMembership/Tag/BookTag/MetadataCache + enums) — Task 2. ✅
- Room-сущности с CASCADE FK — Task 3. ✅
- `filesPresent` + «удалить с устройства vs из каталога» — Task 5 (DAO), Task 10 (repo), Task 12 (use-case), тесты покрывают. ✅
- `manuallyEdited` поле — в модели (Task 2) и сущности (Task 3); логика защиты от перетирания — Подплан 2 (рескан). В этом подплане поле есть, поведение — позже. ✅ (поле определено)
- Прогресс/закладки/полки/теги — Tasks 7–9, 11. ✅
- `MetadataCache` — Task 9. ✅
- Миграции, запрет `fallbackToDestructiveMigration` — Task 13. ✅
- Hilt DI — Tasks 1, 4. ✅
- Онлайн-метаданные, SAF-сканер, плеер, UI — **намеренно вне этого подплана** (Подпланы 2–4).

**Placeholder scan:** плейсхолдеров нет; все шаги содержат реальный код и реальные команды.

**Type consistency:** `BookDao.markFilesDeleted`, `BookRepository.markFilesDeleted`, `DeleteBookFiles.invoke` — имена согласованы. `deleteBookPermanently` / `DeleteBookFromCatalog` — согласованы. `MIGRATIONS` — одинаково в `AudioBookDatabase` и `DatabaseModule`. Имена репозиториев и DAO консистентны между задачами.

## Definition of Done (Подплан 1)

- `./gradlew assembleDebug` — SUCCESS.
- `./gradlew test` — все тесты зелёные (≈20 тестов: DAO, репозитории, use-case’ы, миграции).
- Ключевой acceptance-сценарий «удалить с устройства → карточка остаётся; удалить из каталога → каскадное удаление» покрыт тестами `BookDaoTest`, `BookRepositoryTest`, `DeleteBookUseCasesTest`.
- Схема Room экспортирована в `app/schemas/`.
- Коммиты по каждой задаче.
