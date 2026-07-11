# Mealora Plan

Mealora Plan is a native Android, fully offline, manual weekly meal planner. You plan Breakfast, Lunch, Dinner, and Snack on a seven‑day **Weekly Plate Board**, reuse your own dishes, repeat and copy meals, save weekly templates, keep a practical local shopping list, add notes, and review previous weekly menus.

> Plan your weekly meals, reuse your own dishes, and keep a practical shopping list.

Everything — dishes, ingredients, quantities, schedules, notes — is entered by you. The app performs no automatic food analysis of any kind.

---

## Main features

- Weekly plate board with a compact week navigator, a Monday–Sunday day ribbon, and one expanded day showing four plate cards.
- Four fixed meal slots: Breakfast, Lunch, Dinner, Snack (one or more entries per slot).
- Personal dishes with categories, manually entered ingredients, a preparation note, a default serving label, favorite and archive states.
- Repeat a meal across selected days, copy a day, copy a week, and apply weekly templates — all with explicit conflict handling.
- Local shopping list with generation from your dishes' ingredients, category grouping, custom items, check/uncheck, and clearing.
- Meal notes on entries, days, weeks, dishes, and shopping items.
- Menu history of previous weeks, dish usage, planning statistics, and in‑app planning prompts.
- Warm "Ceramic Weekly Table" visual identity built entirely from Compose shapes — no photos, no external assets.

---

## Disclaimers

**Manual planning disclaimer**

> Mealora Plan is a manual weekly meal organizer. Dishes, ingredients, meal slots, quantities, templates, and notes are entered by the user. The app does not calculate calories, provide diets, support weight-loss programs, or offer nutritional, medical, or professional meal advice.

**Food and ingredient disclaimer**

> Mealora Plan does not check ingredients, allergens, food safety, dietary suitability, or nutritional balance. Review all meal and ingredient information yourself.

**What Mealora Plan explicitly does NOT do**

- No calorie tracking.
- No nutrition tracking (no macronutrients or micronutrients).
- No diets.
- No weight-loss plans or targets.
- No medical advice.
- No allergy guidance or allergen identification.
- It does not classify meals or ingredients as healthy or unhealthy, and it does not interpret ingredient or note text.

---

## Privacy

> Mealora Plan stores dishes, ingredients, weekly plans, meal entries, templates, shopping items, notes, history, and settings locally on this device. The app has no account, no cloud sync, no internet access, no ads, no analytics, no payments, no calorie tracking, no diet program, no nutrition service, and no background monitoring.

Specifically, Mealora Plan has: **no push notifications, no background processing, no camera, no barcode scanning, no online recipe search, no grocery delivery, no account, no backend, no cloud sync, no Firebase, no ads, no analytics, no payments, no internet access, no external APIs, and no runtime permissions.** The Android manifest declares no permissions at all.

---

## Architecture

Simple MVVM with a single local repository.

- **UI:** Jetpack Compose + Material 3, one activity, Navigation Compose.
- **State:** A single activity‑scoped `PlannerViewModel` exposes immutable UI state via `StateFlow` (`AppData`, the selected day, the derived board, and the current in‑app prompt). Screens observe state and call ViewModel actions; they never mutate state directly.
- **Data:** `MealoraRepository` is the only data layer. It uses **DataStore Preferences** and stores each collection as a serialized JSON string via **Kotlinx Serialization**.
- **Utilities:** Focused, pure, fully unit‑tested helpers for week/date math (`WeekUtils`, `DateUtils`), plan operations (`PlanOperations`), shopping generation (`ShoppingGenerator`), statistics (`Statistics`), prompts (`Prompts`), validation (`Validation`), and IDs (`Ids`).

No dependency‑injection framework, no domain layer, no Room. Local storage only.

### Technology stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Android ViewModel, Kotlin Coroutines, Kotlin Flow, DataStore Preferences, Kotlinx Serialization, Gradle Kotlin DSL. `java.time` is used with core library desugaring so date logic works on `minSdk 24`.

### Local storage & DataStore

All application data is persisted with DataStore Preferences under these keys: `dishes_json`, `weekly_plans_json`, `meal_entries_json`, `weekly_templates_json`, `shopping_items_json`, `settings_json`, and `day_notes_json`. Reads are defensive: empty storage, missing keys, empty JSON, and corrupted JSON all resolve to safe defaults, and list decoding recovers valid items even when an individual element is malformed. Every model field has a default value for backward‑compatible deserialization.

---

## Data model

- **WeeklyPlan** `{ id, weekStartDate, title, notes, templateSourceId, createdAt, updatedAt }` — one plan per calendar week; weeks are created lazily and never duplicated. Fallback title: "Untitled Week".
- **Meal slots** — the four fixed slots Breakfast, Lunch, Dinner, Snack. Slots carry no nutritional meaning.
- **MealEntry** `{ id, weeklyPlanId, date, mealSlot, dishId?, customMealName, servingsLabel, note, repeatedFromEntryId?, createdAt, updatedAt }` — a date and slot are required; either a linked dish or a custom name is required.
- **Dish** `{ id, name, category, ingredients, preparationNote, defaultServingsLabel, favorite, archived, createdAt, updatedAt }`. Fallback label for a deleted dish: "Deleted Dish".
- **DishIngredient** `{ id, name, quantityLabel, shoppingCategory, addToShoppingByDefault, createdAt, updatedAt }` — quantity is free text.
- **WeeklyTemplate** `{ id, name, description, entries, createdAt, updatedAt }` and **TemplateMealEntry** `{ id, dayIndex, mealSlot, dishId?, customMealName, servingsLabel, note }`.
- **ShoppingItem** `{ id, title, quantityLabel, category, sourceDishId?, sourceMealEntryId?, weekStartDate, checked, note, createdAt, updatedAt }`.
- **AppSettings** / **PromptSettings** — first day of week, default meal slot, default week opening behavior, show empty slots, and the in‑app prompt toggles.

---

## Screens & workflows

- **Weekly plate board** — the home screen answers "What meals have I planned for this week?" It shows the week range, previous/next/current‑week actions, the seven‑day ribbon, the selected day's four plate cards, a compact weekly overview ("3 of 4 slots planned"), a shopping basket strip, a "Save week as template" action, and bottom navigation for Week, Dishes, Shopping, Templates, and History. Empty week: "No meals planned this week. Choose a plate to add your first meal." Empty plates are outlined; filled plates show dish labels, a note indicator, and a small loop repeat marker.
- **Day detail** — the four meal plates plus a day note, Add Meal, Copy Day, and Clear Day.
- **Add/Edit meal entry** — pick an existing dish or enter a custom meal name, plus serving label, note, and repeat options. No calorie or nutrition fields exist anywhere.
- **Personal dishes** — the Dish Library offers search, category filters, favorites, and archived views. The dish editor manages the ingredient list, preparation note, serving label, and favorite toggle, and shows the manual‑entry disclaimer. Dish detail shows neutral usage info (times planned, last planned date, meal slots used, upcoming dates) and an Add‑to‑day action; unused dishes can be deleted after confirmation, others are archived.
- **Ingredients** — plain text name plus a free‑text quantity label and a shopping category. Quantities are never parsed, converted, or combined mathematically.
- **Repeat meal** — select days in the week, choose the same or a different slot, and resolve conflicts with Keep Existing (fill empty only), Add Another Entry, or Replace Existing. Repeated entries get new IDs and are linked via `repeatedFromEntryId`; they edit independently.
- **Copy day** — choose a target day and merge (add) or replace; existing meals are never silently deleted.
- **Copy week** — copies a week day‑for‑day into a target week. Conflict behavior: keep existing (fill empty only, the safe default), merge all, or replace all.
- **Weekly templates** — create from the current week or from scratch, edit seven days × four slots, rename, duplicate, apply, and delete after confirmation. **Template application modes:** fill empty slots (default), merge all entries, or replace the entire week. No diet templates are provided.
- **Shopping list** — paper grocery‑slip styling, per‑week, with unchecked/checked sections, category grouping and filtering, custom items, check/uncheck, delete, and clear‑checked.
- **Shopping‑list generation** — the preview screen uses only your manually entered dish ingredients (meals without a linked dish are ignored — no ingredients are invented), lets you include/exclude individual ingredients, groups identical trimmed names case‑insensitively while preserving each quantity label as separate text, and saves with "Add missing" (default) or "Replace generated". **Quantity‑label limitation:** quantities are kept as your own text and are never converted between units or combined arithmetically. User‑created custom items are always preserved.
- **Meal notes / weekly notes** — plain text notes on entries (300 chars), days (500), weeks (1000), dish preparation notes (2000), and shopping items. Note contents are never interpreted.
- **Menu history** — past weeks in reverse chronological order with date range, title, meal and unique‑dish counts, and shopping counts; open, copy, save‑as‑template, rename, and delete. Search by dish name or week title, and filter by month.
- **Search & filters** — local search over dish names, custom meal names, ingredient names, and weekly titles, with favorite/archived/category filters and favorite‑first sorting. Search terms are never sent anywhere.
- **Planning statistics** — neutral counts only (planned entries, filled/empty slots, unique dishes, repeated dishes, shopping remaining, meals this month, most‑used slot, most‑planned dish, saved templates) with a filled‑slot strip and meal‑slot distribution bars drawn in Compose. No calories, nutrition, weight, diet adherence, or health scores.
- **In‑app prompts** — local, in‑app only: today has empty slots, tomorrow has no meals, the week has no shopping list, next week is empty, or a template is available. Actions are "Plan …" and "Not Now". As Settings states: "Mealora Plan prompts appear inside the app. The app does not send push notifications or run in the background."
- **Onboarding** — shown only on first launch, with a simplified plate board, feature explanations, offline‑storage and no‑calorie/no‑diet disclaimers, a "Plan This Week" primary action and an "Explore Board" secondary action.
- **Settings** — first day of week, default meal slot, default week opening behavior, show empty meal slots, in‑app prompt toggles, show onboarding again, clear checked shopping items, delete selected weekly plan, delete all menu history, delete archived dishes, reset all local data, app information, and the disclaimers and privacy note.

### Missing records & error handling

Navigation is resilient. A missing weekly plan, meal entry, dish, template, or shopping item, an invalid date or week start, a deleted record, empty DataStore, or restored process state all resolve to a friendly fallback screen with a Back action — never a crash. Fallback labels include "Deleted Dish", "Meal not found", "Week not found", and "Date unavailable".

### Data reset behavior

"Reset all local data?" — "This will permanently remove every dish, ingredient, weekly plan, meal entry, template, shopping item, note, history record, and setting stored by Mealora Plan." Reset requires explicit confirmation and clears the DataStore back to defaults.

### Manual‑entry limitations

Mealora Plan never invents data. Ingredients only exist if you type them; shopping items are generated only from ingredients you entered; quantities remain exactly the text you wrote; and no dish, meal, or ingredient is ever classified, scored, or interpreted.

---

## Visual concept & layout

**Weekly Plate Board / "Ceramic Weekly Table".** The board is genuinely plate‑centric rather than the common "mascot → title → stats card → big buttons" layout. It mixes plate shapes, meal trays, day tabs, a weekly ribbon, a shopping basket strip, template cards, and history sheets. Plate cards are drawn with Compose `Canvas` (an outer rim ring, an inner well, and four slot‑colored segments) — there is no food photography and there are no external illustration assets. The current day is emphasized, and every plate card exposes a text semantic label so meal slots are communicated by text, not color alone.

**App icon concept:** a warm‑cream adaptive icon with a simplified circular plate divided into four segments in the meal‑slot colors and a small terracotta week tab — no photo, no fork‑and‑knife logo, no calorie/scale/medical symbol, no text. Adaptive foreground/background are provided for API 26+ and density PNG launcher icons for API 24–25.

**Splash screen concept:** a static splash (via `androidx.core:core-splashscreen`) on a warm‑cream background with the centered segmented‑plate icon and no animation.

---

## Build & run

### Requirements

- Android Studio (latest stable).
- **JDK 17.**
- Android SDK **Platform 35** and **Build Tools 35.0.0**.

The project targets `compileSdk = 35`, `targetSdk = 35`, `minSdk = 24`, is portrait‑only, uses edge‑to‑edge layout with visible system bars, and — because it uses only Kotlin/Compose/DataStore with no native third‑party binaries — is compatible with Android 15+ **16 KB memory page sizes**. Verify the final release bundle on a 16 KB device/emulator image as a last check.

### Open in Android Studio

1. Open the project folder in Android Studio.
2. Let Gradle sync. On first sync Android Studio provisions the Gradle wrapper JAR automatically. If you build from the command line first and the wrapper JAR is missing, run `gradle wrapper --gradle-version 8.9` once (with a system Gradle) to generate it.
3. Select the `app` configuration and run on a device/emulator (API 24+).

### Debug build

```
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Release: signing, build, and verification

### 1. Generate a PKCS12 keystore

```
keytool -genkeypair -v -storetype PKCS12 -keystore mealora-plan-release-key.p12 -alias mealora_plan_key -keyalg RSA -keysize 2048 -validity 10000
```

Never commit the `.p12` file, passwords, a decoded keystore, or any secret signing properties (all are covered by `.gitignore`).

### 2. Local signing setup

The release `signingConfig` reads credentials from **environment variables** or from a **git‑ignored `keystore.properties`** at the project root. Create `keystore.properties` (do not commit it):

```
storeFile=/absolute/path/to/mealora-plan-release-key.p12
storePassword=YOUR_STORE_PASSWORD
keyAlias=mealora_plan_key
keyPassword=YOUR_KEY_PASSWORD
```

If no complete signing configuration is present, a release build **fails clearly** rather than silently falling back to the debug key.

### 3. Release stability (staged R8)

The release build ships with `isMinifyEnabled = false` and `isShrinkResources = false` so you can first validate a **non‑minified** signed release. Build, install, launch, watch `adb logcat`, and run the functional checklist. Only then enable R8 by setting **both** flags to `true` in `app/build.gradle.kts`:

```
isMinifyEnabled = true
isShrinkResources = true
proguardFiles(
    getDefaultProguardFile("proguard-android-optimize.txt"),
    "proguard-rules.pro",
)
```

The Kotlinx Serialization keep rules are already in `proguard-rules.pro`. Rebuild, reinstall, and re‑test Kotlinx Serialization, DataStore, Navigation Compose, weekly calculations, template application, and shopping generation.

### 4. Build signed artifacts

```
# Signed release APK (for local installation and verification)
./gradlew :app:assembleRelease

# Signed release AAB (for Google Play)
./gradlew :app:bundleRelease
```

Both the release APK and the release AAB use `signingConfigs.release`. **Only the `.aab` is uploaded to Google Play**; use the APK for local installation and verification.

### 5. Local release verification

```
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

The signing certificate must **not** contain `CN=Android Debug`. Repeat this verification after enabling R8.

---

## GitHub Actions

`.github/workflows/android-build.yml` runs on pushes to `main` and via `workflow_dispatch`. It checks out the repo, sets up JDK 17, installs Android SDK Platform 35 and Build Tools 35.0.0, restores the Gradle cache, decodes `ANDROID_KEYSTORE_BASE64` into a temporary PKCS12 file, exposes signing secrets **only** as environment variables, builds the signed release APK and AAB, locates the APK, runs `apksigner verify --print-certs`, prints the certificate, fails if verification fails or if the certificate contains `CN=Android Debug`, and uploads the signed APK (test artifact) and signed AAB (Google Play artifact). Passwords and Base64 values are never printed. CI performs no emulator smoke test — it verifies compilation, signing, certificate validity, and artifact generation, but is not proof that the app launches.

### Required GitHub secrets

- `ANDROID_KEYSTORE_BASE64` — base64 of the `.p12` keystore.
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Use the same value for the keystore and key password unless you have configured separate values reliably.

---

## Local functional test checklist

First launch with empty storage; onboarding; skip onboarding (Explore Board); open current week; navigate previous/next week; return to current week; switch Monday/Sunday week start; add Breakfast/Lunch/Dinner/Snack; add a custom meal; create/edit a dish; favorite/archive/restore a dish; delete an unused dish; add/edit an ingredient; create a meal from a dish and without a dish; edit a meal; move a meal to another day and slot; repeat a meal across several days and verify the existing‑slot conflict dialog; copy day (merge and replace); clear day; copy a previous week (merge empty slots and replace); create a template from a week; edit/apply/duplicate/delete a template; add a weekly note and a meal note; open the shopping list; generate shopping items from selected meals and from the full week; verify missing ingredients are not invented and quantity labels remain intact; add a custom shopping item; check an item and clear checked; open menu history; open/copy/save‑as‑template/delete a historical week; search dishes and ingredients; open statistics; trigger the empty‑today and empty‑tomorrow prompts and dismiss them; disable prompts; reset all local data; relaunch; launch in airplane mode and confirm all functionality still works.

Confirm: no INTERNET permission, no runtime permission dialog, and no calorie/nutrition/diet/weight‑loss fields or sections anywhere. Verify API 35 configuration, AAB generation, the release certificate, and 16 KB page‑size compatibility.

Watch `adb logcat` for `ClassNotFoundException`, `NoSuchMethodError`, serialization crashes, DataStore parse crashes, navigation‑argument crashes, `LocalDate` calculation crashes, missing weekly‑plan/meal‑entry crashes, deleted‑dish crashes, template‑mapping crashes, shopping‑generation crashes, duplicate week creation, R8‑related crashes, and signing misconfiguration.

---

## Tests

Unit tests (JUnit, JVM) cover: Monday‑ and Sunday‑based week starts, seven‑day generation, previous/next week, current‑week detection, day‑index mapping, filled‑slot counts and empty‑day/week detection, meal repeat across selected days (keep/add/replace) with new IDs and `repeatedFromEntryId` links, copy day (add/replace), copy week (merge empty and replace), template day mapping and application, template creation from a week, deleted‑dish handling, shopping‑list generation and case‑insensitive grouping with preserved quantity labels, generated‑item duplicate prevention, custom‑item preservation, statistics, invalid‑date handling, and corrupted/empty JSON fallback with item‑level recovery.

Run them with:

```
./gradlew :app:testDebugUnitTest
```
