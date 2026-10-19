# CLAUDE.md – Regeln für die Arbeit an diesem Repo

Schlichter Kalorienzähler für Android 11+ (Zielgerät: Oppo Reno 2 Z, ColorOS 11). Nutzer spricht Deutsch,
ist kein Android-Entwickler: Antworten kurz, einfache Worte, keine Fachbegriffe ohne Erklärung.

## Nicht verhandelbar
- Kein Gewichtsverlauf, keine Motivation/Tipps/Streaks, keine Foto-Erkennung, kein Konto, keine Cloud, keine Analytics.
- Daten bleiben auf dem Gerät. Netzwerk nur für: Update-Prüfung per Knopf (GitHub-Releases, Repo ist öffentlich),
  Open Food Facts (ab Block 3) und Rezept-Import (Block 8). Nie automatisch im Hintergrund.
- Datenbank: niemals `fallbackToDestructiveMigration`. Jede Schemaänderung = Version +1 + Migration + Test.
  Schemas liegen in `app/schemas/` und werden eingecheckt.
- Signatur: `signing/release.p12` nie ersetzen oder neu erzeugen – sonst lassen sich Updates nicht mehr installieren.
  Passwort steht nur im GitHub-Secret `KEYSTORE_PASSWORD` (und beim Nutzer).
- Nur Features des aktuellen Blocks bauen (siehe `BLOCKS.md`). Ideen unter „Später“ in BLOCKS.md notieren.

## Bauen und Testen (wichtig: nicht lokal!)
- Im Claude-Container sind Google Maven, Maven Central und services.gradle.org gesperrt → kein lokaler Gradle-Build.
  Es gibt deshalb auch keinen Gradle-Wrapper; CI installiert Gradle 8.11.1 über `gradle/actions/setup-gradle`.
- Jeder Push baut über `.github/workflows/build.yml` (Tests + assembleRelease, APK als Artefakt).
- Die Actions-API ist über den Proxy gesperrt. Ergebnis lesen über:
  - `gh api repos/Rilotto/Kalorienzaeler/commits/<sha>/check-runs --jq '.check_runs[] | {name,status,conclusion}'`
  - bei Fehlern hängt der Workflow einen Log-Auszug als Commit-Kommentar an:
    `gh api repos/Rilotto/Kalorienzaeler/commits/<sha>/comments --jq '.[].body'`
- Build dauert ca. 5–10 min. Warten mit ScheduleWakeup/send_later, nicht in Sekunden-Schleifen pollen.
- Release: Tag-Pushes und die Git-Refs-API sind über den Proxy gesperrt. Stattdessen einen Zweig pushen:
  `git push origin HEAD:refs/heads/release/vX.Y.Z` (Testversion: `release/vX.Y.Z-rc.N`). `release.yml` baut signiert,
  legt Tag + GitHub-Release mit `kalorienzaehler-vX.Y.Z.apk` an. Release-Text kommt aus dem CHANGELOG-Abschnitt.
  Den Release-Zweig danach stehen lassen (Löschen ist ebenfalls nicht nötig).

## Ablauf pro Block
1. `BLOCKS.md`, `CHANGELOG.md`, offene PRs/Kommentare lesen. Branch `block-<n>-<name>` von `main`.
2. Kleine Commits. Rechenlogik als reine Kotlin-Funktionen in `core/domain` mit Unit-Tests.
3. Push → CI prüfen (siehe oben) → Fehler beheben, bis grün.
4. Testversion: Zweig `release/vX.Y.0-rc.N` auf den Branch-Stand pushen → Nutzer installiert sie vom Releases-Tab.
5. PR öffnen mit deutscher Zusammenfassung + Testliste fürs Handy (3–8 konkrete Punkte).
6. Nach Freigabe durch den Nutzer: Merge, `release/vX.Y.0` von main pushen, BLOCKS.md + CHANGELOG.md aktualisieren.
7. Nutzer in 2–4 Sätzen informieren: was neu ist, wie updaten, was als Nächstes kommt.

Versionen: Block n → `v0.(n+1).0` (Block 0 = v0.1.0). Nach Block 5 → `v1.0.0`.

## Aufbau
```
app/src/main/java/de/kalorien/app/
  KalorienApp.kt          Application + AppContainer (manuelle DI, bewusst ohne Hilt)
  MainActivity.kt         einzige Activity
  navigation/             Bottom-Navigation, Routen (TopLevel)
  core/ui/theme/          Design-Tokens (Color.kt) – Farben NUR dort
  core/domain/            reine Logik, testbar (Nutrition, später Rezepte, Einheiten-Parser)
  core/data/              ab Block 1: Room, DAOs, Migrationen, Backup
  core/food/              ab Block 2: FoodSource-Interface + BLS/OFF/Custom
  feature/<bereich>/      today, history, settings, später add, scan, recipes, recipeimport
  platform/               ab Block 5: reminders, widget, shortcuts, onboarding
```
- UI-Texte nur in `res/values/strings.xml` (Deutsch). Code-Bezeichner Englisch.
- Versionen nur in `gradle/libs.versions.toml`. Dependabot öffnet monatlich Update-PRs; nur mergen, wenn CI grün.
- UI-Vorlage: Prototyp https://claude.ai/artifact/BREmjixHtk3Yx5x7T8Z1kX
- Konzept (für den Nutzer): https://claude.ai/code/artifact/b8b77347-bff6-4460-b58c-801a697fbbea

## Technik-Entscheidungen
- Kotlin, Jetpack Compose, Material 3, minSdk 30, target/compile 35, JVM 17.
- Ab Block 1: Room + KSP, DataStore für Einstellungen. Block 3: CameraX + ML Kit (gebündelt), Ktor/Retrofit.
  Block 5: Glance-Widget, AlarmManager.setExactAndAllowWhileIdle + BOOT_COMPLETED.
- Einträge speichern einen Nährwert-Snapshot (kcal, P, K, F), damit spätere Änderungen an Lebensmitteln alte Tage nicht verändern.
- Datenbank aktuell Version 5. Migrationstests in `app/src/test/.../core/data/MigrationTest.kt` (Robolectric, JVM).
  Neue Version: Test ergänzen; die neue Schema-JSON entsteht erst im ersten CI-Lauf (bot committet sie) →
  danach einen weiteren Commit pushen, damit der Test sie findet.
- Portions-Lebensmittel (`isPortionFood`): Werte in den *100-Feldern gelten für 1 Portion, Auswahl nach Anzahl.
- Scanner: CameraX + ML Kit Barcode (gebündelt), Code nur nach 2× gleicher Lesung + Prüfziffer.
  Fotos: System-Kamera (TakePicture) oder Galerie → verkleinert → ML Kit Text → `NutritionTextParser`.
- APK nur für `arm64-v8a` (abiFilters), sonst ~90 MB wegen ML-Kit-Modellen.
- Update-Knopf: GitHub-Releases-API, Repo ist öffentlich. FileProvider-Authority `${applicationId}.updates`
  (Pfade `updates/` und `photos/` im Cache).
- Sicherung (ab 0.5): `core/data/Backup.kt`, JSON-Format 2 (seit 0.6, mit Vorlagen) über MediaStore nach `Download/Kalorienzaehler`, die letzten 7 bleiben.
  Formatänderung = `FORMAT` +1 und alte Dateien weiter lesen können.
- Fehlerprotokoll (ab 0.6): `core/CrashLog.kt` schreibt Abstürze nach `filesDir/crash.log`, Teilen nur per Knopf.
- Offene Ideen, Wünsche für v0.7 und der Plan bis 1.0 stehen in `BLOCKS.md`.
