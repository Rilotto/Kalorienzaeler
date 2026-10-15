# Bauplan

Status: offen · in Arbeit · Testversion · fertig

| Block | Inhalt | Version | Status |
|---|---|---|---|
| 0 Fundament | Projekt, Design, Navigation, CI, feste Signatur, Release-Weg | v0.1.0 | fertig |
| 1 Tagebuch | Room, Heute-Ansicht, Schnelleintrag, Bearbeiten, Rückgängig, Tagesziel, Tage wechseln | v0.3.0 (mit 2) | in Arbeit |
| 2 Lebensmittel | BLS 4.0 eingebaut, Suche, Portions-Chips, Zuletzt/Favoriten, Mahlzeit nach Uhrzeit | v0.3.0 | in Arbeit |
| 3 Scanner | CameraX + ML Kit, Open Food Facts + Cache, EAN von Hand, Taschenlampe, Produkt anlegen | v0.4.0 | offen |
| 4 Verlauf & Sicherung | Verlauf 7/14/30, CSV-Export, automatisches Backup + Wiederherstellen | v0.5.0 | offen |
| 5 Android | Erinnerungen, Oppo-Einrichtung, Widget (mit Getränke-Knöpfen), Shortcuts, Benachrichtigungs-Aktionen | v1.0.0 | offen |
| 6 Wiederholen | „Wie gestern“, Mahlzeit-Vorlagen, Einträge verschieben, Ziel pro Wochentag | v1.1.0 | offen |
| 7 Rezepte | Eigene Rezepte, Portionen, Gesamtgewicht gekocht, Reste | v1.2.0 | offen |
| 8 Rezept-Import | Teilen aus Chefkoch & Co., JSON-LD auslesen, Prüfliste, Stück → Gramm | v1.3.0 | offen |

Abhängigkeiten: 1 → alles · 2 → 3, 7 · 7 → 8 · 4 vor 5 empfohlen. Reihenfolge darf auf Wunsch getauscht werden.

## Block 0 – Akzeptanz
- [x] App installiert sich auf dem Oppo und startet
- [x] Heute / Verlauf / Einstellungen per Leiste unten erreichbar, hell und dunkel sehen gut aus
- [x] Einstellungen → Über zeigt die Versionsnummer
- [ ] Testversion v0.1.0-rc.1 lässt sich durch v0.1.0 ersetzen (Update ohne Deinstallieren)

Etappen (mit dem Nutzer abgestimmt): 1+2 zusammen → 3 einzeln (Kamera am Gerät testen) → 4+5 zusammen bis v1.0.

## BLS-Daten
- `app/src/main/assets/bls.tsv` wird von `tools/bls/build_bls_tsv.py` aus dem offiziellen ZIP erzeugt
  (Zweig `tools-bls-inspect`, Workflow „BLS aufbereiten“). Bei neuer BLS-Version: Skript laufen lassen,
  Datei übernehmen, `BlsImporter.ASSET_VERSION` erhöhen.
- Suchqualität: Synonyme und Gewichtung in `core/domain/SearchRanker.kt`, mit echten Namen geprüft.

## Später (Ideen, noch keinem Block zugeordnet)
- Eigene Schriften (Bricolage Grotesque / Figtree wie im Prototyp)
