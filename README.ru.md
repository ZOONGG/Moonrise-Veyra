# Veyra

Veyra — развиваемый Weave-клиент для Minecraft 1.8.9 на основе архивного
[Tryflle/stormy](https://github.com/Tryflle/stormy), со своим интерфейсом,
конфигурацией и изменениями поведения. Основной документ: [English](README.md).
ClickGUI открывается клавишей `Enter`.

## Возможности

- Сохранены семейства боевых, двигательных, визуальных и инвентарных модулей Stormy.
- Переработан ClickGUI: поиск с поддержкой русской раскладки, избранное, темы,
  описания модулей и режимы биндов.
- Добавлены инструменты Player, включая AutoTool, AutoFish, SpeedMine и VoidClutch.
- Конфиги хранятся отдельно в `.weave/veyra`; есть уведомления переключения модулей.

## Чем отличается от Stormy?

Сравнение с архивной веткой Stormy `main`; одинаковое поведение всех унаследованных
модулей и работа на любом сервере не гарантируются.

| Область | Stormy | Veyra |
| --- | --- | --- |
| Базовые функции | AutoClicker, AimAssist, Reach, Velocity, Backtrack, движение, ESP, инвентарь | Эти семейства модулей сохранены в реестре; клавиша ClickGUI — `Enter`. |
| Интерфейс | GUI и идентификаторы Stormy | Новый GUI, поиск, избранное, темы, описания, режимы биндов и отдельные конфиги. |
| Player | Нет SpeedMine и макроса Pearl → Fireball в реестре | SpeedMine ускоряет локальное разрушение блоков в 1.00–2.50 раза, не меняя игровые часы. VoidClutch выполняет последовательность Pearl → Fireball по бинду. |
| Timer | Диапазон 0.50–2.50, обновление по тикам; сброс в GUI | Диапазон 0.00–2.50, обновление каждый кадр; при 0.00 тики клиента остановлены, но GUI и управляющие бинды обслуживаются. Переработаны Strafe only и восстановление часов. TimerSpeedPolicy — внутренняя логика, а не отдельный модуль. |
| Дополнения | Исходный набор | Зарегистрированы JumpReset, TargetFilter, NoJumpDelay, AntiAFK, AutoTool, AutoBlockIn, AutoFish, NoUseDelay, BedESP, BedPlates, MurderFinder и Freecam. |
| Доступность | AutoPlace зарегистрирован | AutoPlace и старый Clutch не зарегистрированы. SafeWalk содержит помощь при строительстве мостов, доступен AutoBlockIn; это не точное повторение AutoPlace. |

**Pearl → Fireball:** назначьте press-бинд **VoidClutch**, положите Ender Pearl и
fire charge в хотбар. Макрос направляет камеру полностью вверх примерно за 50 мс,
выбирает жемчуг, ждёт минимум 50 мс и вызывает обычный правый клик; затем выбирает
и использует fire charge с минимальными паузами по 30 мс. Действия привязаны к
тикам, поэтому фактические задержки могут быть больше; Timer 0.00 останавливает
продвижение последовательности. При GUI, потере фокуса, смерти или смене мира
последовательность отменяется; smooth camera препятствует запуску. Fire charge
создаёт огненный шар лишь там, где это реализовано игрой/сервером. Классы
`PearlFireball` и `FireballPractice` есть в исходниках, но **недоступны в ClickGUI**;
practice-миксины выключены.

В Timer доступен выключенный по умолчанию **Experimental freeze**: попытка
остановить локальное движение при обычных тиках и кликах, с опциональным
возобновлением/отключением после серверной коррекции. Работа зависит от
инструментации и не проверена в игре для этой сборки; стабильная совместимость
и остановка сервера не гарантируются.

## Требования и установка

Целевая версия — **Minecraft 1.8.9**, **Java 17**, совместимое окружение
[WeaveLoader](https://github.com/Weave-MC/Weave-Loader). Компиляция использует
WeaveLoader/API **1.3.4** и Weave Gradle **1.3.3**, а не старую конфигурацию Stormy
с Weave 0.2.4. Для Lunar нужна соответствующая версия Minecraft и подходящий
loader. Другие сборки Lunar и версии loader не проверены; тесты и компиляция
не подтверждают работу в игре.

1. После ручной публикации владельцем скачайте JAR из
   [Releases](https://github.com/ZOONGG/Moonrise-Veyra/releases) либо соберите его.
2. Закройте игру и скопируйте JAR в каталог модов своего Weave
   (обычно `%USERPROFILE%/.weave/mods`). Для Moonrise — в `packages/weave`.
3. Запустите Minecraft 1.8.9 с loader; в игре нажмите `Enter`.

Сборка сама не устанавливает мод и не изменяет Lunar. Проверка в реальной игре
ещё требуется.

## Сборка

Используйте JDK 17 и Gradle wrapper:

```powershell
.\gradlew.bat clean build
```

JAR: `build/libs/Veyra-0.1.1-moonrise.1.jar`. Задача `build` включает JUnit-тесты.
CI собирает workflow-артефакты; Release и теги публикуются вручную.
Для явной установки в Moonrise:

```powershell
.\gradlew.bat installToMoonrise -PmoonriseHome=D:\path\to\Moonrise
```

Вместо параметра можно задать переменную `MOONRISE_HOME`.

## Структура проекта

- `dev.veyra.client` — модули, конфиги и интерфейс.
- `dev.veyra.weave` — entrypoint, события, hooks и mixins.
- Конфигурация: `.weave/veyra`, отдельно от Stormy.

## Авторство и лицензии

Основа: [Stormy, автор Tryflle](https://github.com/Tryflle/stormy). Исходный README
указывает раннюю сборку [syz66/illusion](https://github.com/syz66/illusion)
(удалена из списка), основанную на Raven. Это историческое указание сохранено.
Самостоятельные изменения интерфейса, конфигурации и модулей — работа
[проекта Veyra](https://github.com/ZOONGG/Moonrise-Veyra).
Loader: [Weave-MC / WeaveLoader](https://github.com/Weave-MC/Weave-Loader).

