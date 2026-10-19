# Bauplan

Status: offen · in Arbeit · Testversion · fertig

| Block | Inhalt | Version | Status |
|---|---|---|---|
| 0 Fundament | Projekt, Design, Navigation, CI, feste Signatur, Release-Weg | v0.1.0 | fertig |
| 1 Tagebuch | Room, Heute-Ansicht, Schnelleintrag, Bearbeiten, Rückgängig, Tagesziel, Tage wechseln | v0.3.0 (mit 2) | fertig |
| 2 Lebensmittel | BLS 4.0 eingebaut, Suche, Portions-Chips, Zuletzt/Favoriten, Mahlzeit nach Uhrzeit | v0.3.0 | fertig |
| 3 Scanner | CameraX + ML Kit, Open Food Facts + Cache, EAN von Hand, Taschenlampe, Produkt anlegen, **Nährwerte per Foto (OCR)** | v0.4.0 | fertig |
| 4 Verlauf & Sicherung | Verlauf 7/14/30, CSV-Export, automatisches Backup + Wiederherstellen | v0.5.0 / v0.6.0 | fertig |
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

## To-do v0.5 / v0.6 – Vorschläge (★ = Empfehlung; [x] = gebaut)

v0.6 gebaut: Verlauf, Vorlagen, Wochentag-Ziele, Hell/Dunkel, Start-Reiter, eigene Lebensmittel, Rechnen, Fehlerprotokoll.
**Plan bis 1.0** (mit Nutzer abgestimmt): v0.7 = Nutzerwünsche unten (Verlauf-Diagramme, Farbschemas);
v1.0 = Block 5 (Erinnerungen + Oppo-Anleitung, Widget, Shortcuts, Ersteinrichtung). Danach Rezepte (Block 7/8).
Bewusst nicht gemacht: R8/Minify (Größe kommt fast nur von den ML-Kit-Modellen, Risiko für Abstürze, die CI nicht sieht).

### Weitere Wege, Essen einzutragen
- [x] (v0.5.0) ★ **Menge in Stück/Scheiben eingeben** (Wunsch des Nutzers): Statt nur Gramm eine Einheit wählen, z. B. „Scheibe (25 g)“,
      und die Anzahl eintippen oder per Knopf wählen (½, 1, 1½, 2, 3). Beispiel: 1,5 Scheiben Gouda → App rechnet 37,5 g und die kcal.
      Gilt für alle Portionen (Scheibe, Stück, EL, TL, Becher …). Der Eintrag zeigt danach „1,5 Scheiben (38 g)“, beim Bearbeiten
      bleibt die gewählte Einheit erhalten. Für die Anzeige braucht der Eintrag eine Spalte für Einheit + Anzahl (DB v4, mit Migrationstest).
- [x] (v0.5.0) ★ **Mehrere nacheinander**: „Weiter eintragen“-Knopf – das Fenster bleibt offen (Frühstück mit 4 Zutaten ohne 4× öffnen)
- [x] (v0.5.0) ★ **Mahlzeit kopieren / „Wie gestern“**: ganze Mahlzeit eines anderen Tages mit einem Tipp übernehmen
- [x] (v0.5.0) ★ **Nochmal**: Eintrag lange drücken → noch einmal eintragen (heute, gleiche Menge)
- [x] (v0.6.0) ★ **Mahlzeit-Vorlagen**: „Mein Frühstück“ = Quark + Banane + Kaffee, ein Tipp trägt alles ein
- [ ] **Packungsanteil** bei gescannten Produkten: ¼ / ½ / ganze Packung
- [ ] **Rest-Wiegen**: Packung vorher/nachher wiegen → gegessene Menge wird berechnet
- [x] (v0.6.0) **Rechnen im Schnelleintrag**: „250+120+80“ und kJ → kcal
- [ ] **Getränke-Knöpfe** in der Tagesansicht (Kaffee, Cola, Bier, Saft) – vorgezogen aus Block 5
- [ ] **Spracheingabe** („200 Gramm Quark“) über die Android-Spracherkennung
- [ ] Später: Rezepte (Block 7), Rezept-Import (Block 8), Kantinen-Wochenplan (siehe „Vielleicht“)

### Nährwerte
- [x] (v0.5.0) **Zucker als zusätzlicher Wert** (Nutzerfrage): Alle Quellen liefern ihn – BLS-Spalte `SUGAR` (Mono- + Disaccharide je 100 g),
      Open Food Facts `sugars_100g`, Foto-Erkennung „davon Zucker“. Umsetzung: BLS-Tabelle neu erzeugen (Spalte sugar),
      DB v4 (food.sugar100, entry.sugar, nullable) + Migrationstest, Anzeige als 4. Balken in der Tagesansicht (abschaltbar, wie die Makros),
      optional mit eigenem Tageslimit. Alte Einträge haben keinen Zuckerwert → als „–“ bzw. nicht mitgezählt kennzeichnen.
      Gleiches Prinzip ginge auch für Ballaststoffe und Salz (beide ebenfalls in BLS und OFF).
- [x] (v0.5.0) ★ **Nährwerte von Lebensmitteln korrigieren** (Nutzerwunsch, „die Werte sind teils komisch“):
      Im Lebensmittel (Eintragen → Lebensmittel → Stift) kcal, Eiweiß, Kohlenhydrate, Fett, Zucker je 100 g/ml bzw. Portion ändern.
      Bei BLS- und Open-Food-Facts-Produkten wird die Korrektur als eigene Änderung gespeichert, Original bleibt erhalten
      („Auf Original zurücksetzen“), damit ein späteres Datenbank-Update sie nicht überschreibt. Markierung „angepasst“.
      Gilt für neue Einträge; Frage beim Speichern: „Auch heutige Einträge damit neu berechnen?“
- [x] (v0.5.0) ★ **Werte eines einzelnen Eintrags anpassen**: im Fenster „Eintrag bearbeiten“ kcal und Makros direkt überschreiben (nur dieser Eintrag)
- [x] (v0.5.0) ★ **Eigene Tagesziele für Makros** (Nutzerwunsch): Eiweiß, Kohlenhydrate, Fett (und Zucker-Limit) in Gramm festlegen –
      wahlweise in Prozent der Kalorien. Standard bleibt die automatische Aufteilung 20/50/30.
- [x] (v0.5.0) Hinweis in der App, woher ein Wert stammt (BLS = Durchschnittswert für die Lebensmittelart, Open Food Facts = Angaben der
      Packung, von Nutzern erfasst) – erklärt Abweichungen zur eigenen Packung; bei Markenprodukten lieber scannen

### Bedienung
- [x] (v0.5.0) ★ **Zwischen Tagen wischen** in der Tagesansicht
- [x] (v0.5.0) ★ **Eintrag verschieben** (anderer Tag / andere Mahlzeit) im Bearbeiten-Fenster
- [x] (v0.6.0) **Eigene Lebensmittel verwalten** (bearbeiten, löschen) in den Einstellungen
- [ ] Optionale **Uhrzeit** an Einträgen

### Verlauf & Sicherung (Block 4)
- [x] (v0.6.0) ★ **Verlauf** 7/14/30 Tage mit Ziellinie, Durchschnitt, Tag antippen
- [x] (v0.5.0) ★ **Automatische Sicherung** + Wiederherstellen (Datei im Download-Ordner), **CSV-Export**

### Einstellungen
- [x] (v0.6.0) **Ziel pro Wochentag** (vorgezogen aus Block 6)
- [ ] **Mahlzeiten anpassen**: umbenennen, ausblenden, z. B. „Spätsnack“ ergänzen
- [x] (v0.6.0) **Hell/Dunkel** erzwingen statt Systemeinstellung
- [x] (v0.6.0) **Standard-Reiter** beim Eintragen (z. B. direkt „Scannen“)

### Technik
- [ ] Bibliotheks-Updates (offene Dependabot-PRs) einspielen und testen
- [ ] App verkleinern (R8/Minify; aktuell gut 25 MB)
- [x] (v0.6.0) Fehlerprotokoll in der App exportierbar machen (hilft bei Problemen auf dem Oppo)

## Wünsche des Nutzers – v0.7
- [ ] **Fehler: Drehen des Handys bricht das Eintragen ab** (landet in der Übersicht). Ursache: Fenster-Zustand
      (offenes Eintragen/Bearbeiten, gewählter Reiter, Eingaben) wird beim Drehen nicht gemerkt (`remember` statt
      `rememberSaveable`). Alles, was offen ist, muss das Drehen überstehen – auch Kamera und Foto-Formular.
- [ ] **Portionsgröße selbst festlegen** (Beispiel Reiswaffeln: 6 Stück = 100 g, vorgeschlagen wurden „250 g (gekocht)“).
      Im Lebensmittel eigene Portionen anlegen/ändern, z. B. „1 Stück = 16,7 g“ oder „Packung = 100 g“; die App merkt sie sich
      und schlägt sie zuerst vor. Außerdem prüfen, warum „gekocht“ auftaucht (Portionsvorschlag passt Reis gekocht statt
      Reiswaffel → PortionGuesser genauer machen).
- [ ] **Verlauf schöner und flexibler**: Diagrammart wählbar (Balken, Linie, …) und Ansicht nach Tagen / Wochen / Monaten
      (Wochen- und Monatswerte als Durchschnitt pro Tag). Baut auf dem einfachen Verlauf aus v0.6 auf.
- [ ] **Farbschemas zur Auswahl** (Einstellungen): 5 schöne Pastell-Schemas + 2 ausgefallene im 90er-Stil,
      jeweils hell und dunkel. Farben weiter nur über die Tokens in `Color.kt`.

## Vielleicht (Nutzer plant noch)
- **Kantinen-Wochenplan**: ganzen Wochenplan fotografieren oder als **PDF** einfügen → App erkennt die Gerichte mit
  Nährwerten → kleine Auswahlliste („Was gab es heute?“) zum schnellen Eintragen. Bei PDFs mit echtem Text ist keine
  Texterkennung nötig (Text direkt auslesen), bei gescannten PDFs OCR wie bei Fotos. Baut auf der OCR aus Block 3 auf.

## Später (Ideen, noch keinem Block zugeordnet)
- Eigene Schriften (Bricolage Grotesque / Figtree wie im Prototyp)
