# Bauplan

Status: offen · in Arbeit · Testversion · fertig

| Block | Inhalt | Version | Status |
|---|---|---|---|
| 0 Fundament | Projekt, Design, Navigation, CI, feste Signatur, Release-Weg | v0.1.0 | fertig |
| 1 Tagebuch | Room, Heute-Ansicht, Schnelleintrag, Bearbeiten, Rückgängig, Tagesziel, Tage wechseln | v0.3.0 (mit 2) | fertig |
| 2 Lebensmittel | BLS 4.0 eingebaut, Suche, Portions-Chips, Zuletzt/Favoriten, Mahlzeit nach Uhrzeit | v0.3.0 | fertig |
| 3 Scanner | CameraX + ML Kit, Open Food Facts + Cache, EAN von Hand, Taschenlampe, Produkt anlegen, **Nährwerte per Foto (OCR)** | v0.4.0 | fertig |
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

## Wünsche des Nutzers – v0.3.1
- [x] **Wischen zum Löschen** in der Tagesansicht: Eintrag nach links wischen → gelöscht, Snackbar „Rückgängig“ wie beim Löschen-Knopf
- [x] **Favorit direkt aus dem Eintrag**: Im „Eintrag bearbeiten“-Fenster ein Herz/Stern, das das Lebensmittel zu den Favoriten hinzufügt/entfernt.
      Bei Schnelleinträgen (ohne Lebensmittel) legt der Stern ein eigenes Lebensmittel mit Name + kcal an und markiert es als Favorit,
      damit man es nicht jedes Mal neu eintippen muss.

## Block 3 – Ergänzung: Nährwerte per Foto (OCR, lokal) – gebaut in v0.4.0
- ML Kit Texterkennung (gebündelt, offline). Reiter „Foto“ neben „Scannen“.
- Foto von Kantinen-Schild oder Nährwerttabelle → Zahlen hinter kcal / Eiweiß / Fett / Kohlenhydrate finden →
  vorausgefülltes Formular (Name, Werte, pro Portion oder pro 100 g) → prüfen → speichern, optional als Favorit.
- Auch für „Barcode unbekannt → Produkt anlegen“.
- Nutzer fotografiert die Kantinenwerte meist vom **Monitor** (Moiré/Spiegelung beachten). Beispielfotos vom Nutzer
  abwarten und den Parser daran testen.

## Zusätzlich gebaut
- Update-Knopf in den Einstellungen (v0.3.0): GitHub-Releases prüfen, APK laden, Android-Installer starten. Repo ist dafür öffentlich.

## To-do v0.5 – Vorschläge (★ = Empfehlung, bitte auswählen)

### Weitere Wege, Essen einzutragen
- [ ] ★ **Mehrere nacheinander**: „Weiter eintragen“-Knopf – das Fenster bleibt offen (Frühstück mit 4 Zutaten ohne 4× öffnen)
- [ ] ★ **Mahlzeit kopieren / „Wie gestern“**: ganze Mahlzeit eines anderen Tages mit einem Tipp übernehmen
- [ ] ★ **Nochmal**: Eintrag lange drücken → noch einmal eintragen (heute, gleiche Menge)
- [ ] ★ **Mahlzeit-Vorlagen**: „Mein Frühstück“ = Quark + Banane + Kaffee, ein Tipp trägt alles ein
- [ ] **Packungsanteil** bei gescannten Produkten: ¼ / ½ / ganze Packung
- [ ] **Rest-Wiegen**: Packung vorher/nachher wiegen → gegessene Menge wird berechnet
- [ ] **Rechnen im Schnelleintrag**: „250+120+80“ und kJ → kcal
- [ ] **Getränke-Knöpfe** in der Tagesansicht (Kaffee, Cola, Bier, Saft) – vorgezogen aus Block 5
- [ ] **Spracheingabe** („200 Gramm Quark“) über die Android-Spracherkennung
- [ ] Später: Rezepte (Block 7), Rezept-Import (Block 8), Kantinen-Wochenplan (siehe „Vielleicht“)

### Bedienung
- [ ] ★ **Zwischen Tagen wischen** in der Tagesansicht
- [ ] ★ **Eintrag verschieben** (anderer Tag / andere Mahlzeit) im Bearbeiten-Fenster
- [ ] **Eigene Lebensmittel verwalten** (bearbeiten, löschen) in den Einstellungen
- [ ] Optionale **Uhrzeit** an Einträgen

### Verlauf & Sicherung (Block 4)
- [ ] ★ **Verlauf** 7/14/30 Tage mit Ziellinie, Durchschnitt, Tag antippen
- [ ] ★ **Automatische Sicherung** + Wiederherstellen (Datei im Download-Ordner), **CSV-Export**

### Einstellungen
- [ ] **Makro-Ziele** selbst festlegen (Gramm oder Prozent)
- [ ] **Ziel pro Wochentag** (vorgezogen aus Block 6)
- [ ] **Mahlzeiten anpassen**: umbenennen, ausblenden, z. B. „Spätsnack“ ergänzen
- [ ] **Hell/Dunkel** erzwingen statt Systemeinstellung
- [ ] **Standard-Reiter** beim Eintragen (z. B. direkt „Scannen“)

### Technik
- [ ] Bibliotheks-Updates (offene Dependabot-PRs) einspielen und testen
- [ ] App verkleinern (R8/Minify; aktuell gut 25 MB)
- [ ] Fehlerprotokoll in der App exportierbar machen (hilft bei Problemen auf dem Oppo)

## Vielleicht (Nutzer plant noch)
- **Kantinen-Wochenplan**: ganzen Wochenplan fotografieren oder als **PDF** einfügen → App erkennt die Gerichte mit
  Nährwerten → kleine Auswahlliste („Was gab es heute?“) zum schnellen Eintragen. Bei PDFs mit echtem Text ist keine
  Texterkennung nötig (Text direkt auslesen), bei gescannten PDFs OCR wie bei Fotos. Baut auf der OCR aus Block 3 auf.

## Später (Ideen, noch keinem Block zugeordnet)
- Eigene Schriften (Bricolage Grotesque / Figtree wie im Prototyp)
