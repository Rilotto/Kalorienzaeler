# Änderungen

## [0.7.0] – Farben, schönerer Verlauf, eigene Portionen
### Neu
- **8 Farbschemas** (Einstellungen → Darstellung): Salbei, Pfirsich, Lavendel, Minze, Himmel, Rosé
  und zwei aus den 90ern: Memphis und Vaporwave – jeweils hell und dunkel
- **Verlauf** nach **Tagen, Wochen oder Monaten** (Wochen/Monate als Durchschnitt pro Tag) und als
  **Balken, Linie oder Fläche**. Die Auswahl bleibt gespeichert.
- **Eigene Portion festlegen** im Lebensmittel, z. B. „1 Stück = 16,7 g“ – oder aus der Packung ausrechnen
  („100 g, 6 Stück“). Wird danach zuerst vorgeschlagen.
### Behoben
- Handy drehen bricht das Eintragen nicht mehr ab (auch Kamera und Formulare bleiben offen)
- Portionsvorschläge: „Reiswaffeln“ bekamen „1 Portion gekocht“ wie Reis – jetzt zählt das Wortende

## [0.6.0] – Verlauf, Vorlagen, mehr Einstellungen
### Neu
- **Verlauf**: letzte 7, 14 oder 30 Tage als Balken mit Ziellinie, Durchschnitt pro Tag, Tage mit Einträgen und
  Tage über dem Ziel. Balken oder Tag antippen → diesen Tag öffnen.
- **Mahlzeit-Vorlagen**: In der Tagesansicht bei einer Mahlzeit auf ⋮ → „Als Vorlage speichern“.
  Beim Eintragen steht sie oben unter „Vorlagen“ – ein Tipp trägt alles ein (mit „Rückgängig“).
- **Ziel pro Wochentag** (z. B. am Wochenende mehr) – gilt für die Tagesansicht und den Verlauf
- **Hell/Dunkel** selbst wählen statt wie das Handy
- **Start-Reiter beim Eintragen** wählbar (z. B. direkt „Scannen“)
- **Eigene Lebensmittel verwalten**: Werte ändern oder löschen (alte Einträge bleiben erhalten)
- **Rechnen im Schnelleintrag**: „250+120+80“ oder „2*150“, und Werte in kJ umrechnen
- **Fehlerprotokoll**: Stürzt die App ab, lässt sich das Protokoll in den Einstellungen teilen
### Behoben
- „Werte pro pro 100 g“ im Korrigieren-Fenster
### Technik
- Datenbank Version 5 (Vorlagen), Migrationstest 4→5; Sicherung Format 2 (mit Vorlagen und neuen Einstellungen, alte Sicherungen bleiben lesbar)

## [0.5.0] – Genauer eintragen, Zucker, Sicherung
### Neu
- **Scheiben und Stück**: Einheit wählen (z. B. „Scheibe“) und Anzahl tippen – ½, 1, 1½, 2, 3 oder eigene Zahl.
  Der Eintrag zeigt dann z. B. „1,5 × Scheibe (38 g)“.
- **Werte korrigieren**: Im Lebensmittel auf den Stift tippen und kcal, Eiweiß, Kohlenhydrate, Fett, Zucker ändern.
  „Auf Original zurücksetzen“ holt die alten Werte zurück. Alte Tage bleiben unverändert.
- **Werte nur für einen Eintrag ändern** im Fenster „Eintrag bearbeiten“
- **Eigene Makro-Ziele** in Gramm und ein **Zucker-Limit** (Einstellungen; leer = automatisch)
- **Zucker** als vierter Balken (abschaltbar), aus BLS, Open Food Facts, Foto und eigenen Lebensmitteln
  – auch ältere Einträge bekommen ihren Zuckerwert nachträglich, wenn das Lebensmittel ihn kennt
  – früher gescannte Produkte holen sich den Zucker beim nächsten Scan, oder alle auf einmal per Knopf in den Einstellungen
- Hinweis, woher die Werte stammen (BLS = Durchschnitt, Open Food Facts = Packung)
- **Hinzufügen und weiteres**: Das Fenster bleibt offen für die nächste Zutat
- **Wie gestern**: Mahlzeit vom Vortag mit einem Tipp übernehmen
- **Nochmal**: Eintrag lange drücken → heute erneut eintragen
- **Tage wischen**: Auf der Kopfzeile oder der Tagesübersicht nach links/rechts wischen
- **Verschieben**: Im Bearbeiten-Fenster Tag und Mahlzeit ändern
- **Sicherung**: täglich automatisch in „Download/Kalorienzaehler“ (die letzten 7), „Jetzt sichern“, „Wiederherstellen“, „Als CSV teilen“
### Technik
- Datenbank Version 4 (Zucker, Einheiten, korrigierte Werte), Migrationstest 3→4
- BLS-Tabelle neu mit Zuckerwerten

## [0.4.1] – Kleinere App
### Verbessert
- Download deutlich kleiner: Die App enthält nur noch den Teil für 64-Bit-Handys (wie das Oppo Reno 2 Z) statt für vier Prozessortypen.

## [0.4.0] – Block 3: Scannen, Foto, eigene Lebensmittel
### Neu
- Reiter **Scannen**: Barcode mit der Kamera lesen (mit Licht-Knopf) oder Nummer eintippen.
  Produkte kommen aus Open Food Facts und werden gespeichert – beim nächsten Mal auch ohne Internet.
  Packungsgrößen wie „1 Dose (330 ml)“ erscheinen als Portionsknopf.
- Unbekannter Barcode → Formular, die App merkt sich das Produkt samt Barcode
- Reiter **Foto**: Kantinen-Schild, Monitor oder Nährwerttabelle fotografieren (oder Screenshot aus der Galerie) –
  Kalorien, Eiweiß, Kohlenhydrate und Fett werden auf dem Handy ausgelesen und vorausgefüllt
- **Eigenes Lebensmittel anlegen** (Suche → ganz unten): Werte pro 100 g, pro 100 ml oder pro Portion
- **Rückgängig nach dem Hinzufügen** (Leiste unten), falls man sich vertippt hat
- Enthält alles aus 0.3.1 (Wischen zum Löschen, Herz im Eintrag)
### Verbessert
- Formulare bleiben bei offener Tastatur scrollbar (Knöpfe werden nicht mehr verdeckt)
- Wischen löscht garantiert nur einmal
### Technik
- Datenbank Version 3 (Packungsgröße), Migrationstests 1→2 und 2→3
- Barcode- und Texterkennung mit Google ML Kit, Modelle in der App enthalten (offline)

## [0.3.1] – Wünsche aus dem Alltag
### Neu
- Einträge in der Tagesansicht nach links wischen zum Löschen (mit „Rückgängig“)
- Herz im Fenster „Eintrag bearbeiten“: Lebensmittel direkt als Favorit merken
- Herz bei Schnelleinträgen (z. B. „Kantine, 650 kcal“) merkt Name und Kalorien als eigenes Lebensmittel –
  danach in der Suche unter „Favoriten“, Auswahl nach Anzahl Portionen (½×, 1×, 1½×, 2×)
### Technik
- Datenbank Version 2 (automatische Migration, mit Test: bestehende Einträge und Favoriten bleiben erhalten)

## [0.3.0] – Block 1 + 2: Tagebuch und Lebensmittel
### Neu
- Tagesansicht mit echten Einträgen: Ring „kcal übrig“, Ziel, Gegessen, Makro-Balken (abschaltbar)
- Zwischen Tagen blättern (Pfeile), Tippen auf das Datum springt zu heute
- „Eintragen“-Knopf wählt die Mahlzeit nach Uhrzeit vor, das „+“ an jeder Mahlzeit direkt diese
- Suche in 7.140 Lebensmitteln des Bundeslebensmittelschlüssels (BLS 4.0), funktioniert ohne Internet
- Suche versteht Alltagswörter (Nudeln → Teigwaren, Ei → Hühnerei, Quark → Speisequark) und zusammengeschriebene Wörter
- Portions-Vorschläge (1 Scheibe, 1 Stück, 1 EL …) und freie Grammangabe, Kalorien werden live berechnet
- Favoriten (Herz) und „Zuletzt verwendet“ – erscheinen sofort beim Öffnen der Suche
- Schnelleintrag nur mit Name und Kalorien
- Einträge antippen zum Ändern (Menge, Mahlzeit) oder Löschen – mit „Rückgängig“
- Einstellungen: Tagesziel, Makros an/aus, Quellenangabe
- Einstellungen → Updates: „Nach Updates suchen“ lädt neue Versionen direkt in der App herunter und startet die Installation (auf Wunsch inkl. Testversionen)
### Behoben (nach Test rc.1)
- Portions-Vorschläge fehlten bei BLS-Namen mit Leerzeichen (z. B. „Hafer Flocken“ zeigte nur „100 g“)
- Lebensmittel ohne passende Portion bekommen jetzt 50 / 100 / 150 / 200 g zur Auswahl
- Neue Portionen für Pizza, Döner, Schnitzel/Filet, Lachs, Pommes, Würstchen


## [0.1.0] – Block 0: Fundament
### Neu
- Erste installierbare Version mit dem Design aus dem Prototyp (hell und dunkel)
- Leiste unten: Heute, Verlauf, Einstellungen (noch ohne Funktionen)
- Einstellungen → Über: Version, Datenschutz, Datenquellen
- Automatischer Build und feste Signatur: spätere Updates installieren sich über diese Version
