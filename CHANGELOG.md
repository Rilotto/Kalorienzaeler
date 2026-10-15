# Änderungen

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
