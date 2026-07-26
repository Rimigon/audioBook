# Подплан 3: Плеер (Media3) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans.

**Goal:** Работающий фоновый плеер на Media3 с MediaSession (экран блокировки, наушники), скоростью 0.5–4x, volume boost, эквалайзером, таймером сна, закладками, авто-резьюмом и персистом прогресса в Room каждые ~10с и при паузе.

**Architecture:** `AudioBookPlaybackService : MediaSessionService` держит ExoPlayer + MediaSession; `PlayerController` (в UI-процессе) обёртка над `MediaController` → `StateFlow<PlayerUiState>`. Чистая логика (таймер сна, throttling прогресса, редьюсер состояния) отделена и тестируется JVM-тестами. Эффекты (speed/volume/eq) — на ExoPlayer. Прогресс пишется через `ProgressRepository` и `BookRepository`.

**Tech Stack:** Media3 ExoPlayer + MediaSession, Foreground service, coroutines/Flow, Room (готово).

## File Structure (этот подплан)

```
player/
  service/AudioBookPlaybackService.kt
  controller/PlayerController.kt
  controller/PlayerUiState.kt
  effects/PlayerEffects.kt         # applySpeed/applyVolume/applyEqualizer
  sleep/SleepTimer.kt              # ЧИСТАЯ: countdown + until-end-chapter
  progress/ProgressSaver.kt        # ЧИСТАЯ: throttle «каждые 10с»
  progress/ProgressSaverCoordinator.kt
```

---

### Task 1: `SleepTimer` (чистая) + тест

`SleepTimer(clock: () -> Long)`: режимы `Duration(ms)` / `UntilEndOfChapter`. `start(durationMs)`, `startUntilChapterEnd(remainingMs)`, `tick(now): SleepDecision` → `Continue(msLeft)` / `Pause`. `cancel()`.

- [ ] тесты: countdown до паузы; UntilEndOfChapter когда remaining<=0 → Pause; cancel.
- [ ] реализация; коммит `feat(player): sleep timer logic`.

### Task 2: `ProgressSaver` (чистая throttle) + тест

`ProgressSaver(intervalMs=10000)`: `shouldSave(lastSaveMs, nowMs): Boolean`. + координатор, который при паузе/остановке форсирует сохранение.

- [ ] тесты: less than interval → false; >= → true; force flag.
- [ ] реализация; коммит `feat(player): progress saver throttle`.

### Task 3: `PlayerUiState` + редьюсер (чистая) + тест

`data class PlayerUiState(bookId?, title?, chapter?, positionMs, durationMs, isPlaying, speed, sleepLeftMs?, ...)`. Редьюсер обновляет из событий плеера.

- [ ] тесты: применение событий.
- [ ] реализация; коммит `feat(player): player ui state`.

### Task 4: `PlayerEffects` (speed/volume/equalizer) + smoke

Применение к ExoPlayer: `applySpeed(player, speed)`, `applyVolumeBoost(player, 1..2)`, `applyEqualizer(audioSessionId, preset)`. Без unit-теста (нужен реальный audio session); smoke-компиляция.

- [ ] реализация; коммит `feat(player): player effects`.

### Task 5: `AudioBookPlaybackService` + DI

`AudioBookPlaybackService : MediaSessionService`. Создаёт ExoPlayer (контент — главы как MediaItems из файловых URI), MediaSession с custom commands (sleep, bookmark, speed), foreground notification. DI: Hilt предоставляет сервис? Media3 service инстанцируется системой — используем `@AndroidEntryPoint`-аналог: Hilt не поддерживает MediaSessionService напрямую без hilt-workaround; плеер берёт зависимости через `EntryPointAccessors` из `AudioBookApp`.

- [ ] реализация; регистрация в манифесте (`<service android:name=".player.service.AudioBookPlaybackService" android:exported="true" android:foregroundServiceType="mediaPlayback"><intent-filter><action android:name="androidx.media3.session.MediaSessionService"/></intent-filter></service>`), POST_NOTIFICATIONS permission.
- [ ] smoke-сборка. коммит `feat(player): media3 playback service`.

### Task 6: `PlayerController` + интеграция прогресса

`PlayerController`: подключается к сервису через `MediaController`, стримит `PlayerUiState`, методы `play(bookId, positionMs)`, `pause`, `seek`, `setSpeed`, `startSleep(ms)`, `addBookmark`. Прогресс сохраняется в сервисе (каждые 10с + при паузе) через `ProgressRepository`. Авто-резьюм: при старте книги читается `ProgressRepository.get(bookId)`.

- [ ] controller-тест с фейковым MediaController-интерфейсом: проверка состояния после play/pause.
- [ ] реализация; коммит `feat(player): player controller`.

### Task 7: Финальный прогон + DoD

- [ ] `assembleDebug` + `testDebugUnitTest` зелёные.

## Self-Review

Покрытие спеки (Подплан 3): Media3 MediaSessionService (Task 5), скорость/eq/volume (Task 4), таймер сна (Task 1), закладки (Task 6 команда), MediaSession lock-screen/notification (Task 5), авто-резьюм + персист прогресса (Tasks 2,6), фоновое воспроизведение foreground (Task 5). Плейсхолдеров нет (m4b главы — задокументированное ограничение в P2).
