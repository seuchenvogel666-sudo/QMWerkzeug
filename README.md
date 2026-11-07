# QM-Werkzeuge (Android-App, Version 0.1.0)

Nachschlage-App für 33 Werkzeuge des Qualitätsmanagements, sortiert in 5 Kategorien:
Q7, M7, Problemlösung und Verbesserung, Vorbeugen und Planen, Messen, Organisation und Prüfung.

- Kategorien und Werkzeuge lassen sich auf- und zuklappen
- Suche nach Namen und Beschreibung
- Zu jedem Werkzeug eine kurze Beschreibung, „Wann einsetzen?“ und ein Beispielbild (zoombar)

## APK über GitHub bauen

1. Auf github.com ein neues Repository anlegen (z. B. `qm-werkzeuge`).
2. Den **Inhalt** dieses Ordners hochladen („Add file“ → „Upload files“), also `app`, `gradle`,
   `.github`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew` usw.
3. Nach dem Hochladen startet unter **Actions** automatisch „APK bauen“ (dauert ca. 3–5 Minuten).
4. Danach liegt die fertige Datei `QM-Werkzeuge.apk` unter **Releases** (rechte Seite der Repo-Startseite).
   Auf dem Handy herunterladen und installieren (Installation aus unbekannten Quellen erlauben).

**Falls der Ordner `.github` beim Hochladen fehlt** (versteckte Ordner werden manchmal nicht mit hochgeladen):
„Add file“ → „Create new file“, als Namen `.github/workflows/build.yml` eintippen
und den Inhalt der Datei `.github/workflows/build.yml` aus diesem Ordner hineinkopieren.

Jeder neue Upload auf den Branch `main` baut automatisch eine neue APK.
Manuell starten: Actions → „APK bauen“ → „Run workflow“.

## Technik

Kotlin, Jetpack Compose (Material 3), minSdk 26 (Android 8.0), targetSdk 35.
