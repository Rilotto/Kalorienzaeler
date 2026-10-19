# Funktionsliste (Feature-Register)

Alle Funktionen, die sich der Nutzer gewünscht hat oder die im Konzept zugesagt sind – mit Status.
**Vor jeder neuen Version durchgehen** (siehe CLAUDE.md → „Vor jeder Version“). Nichts, was hier als
✅ steht, darf in einer neuen Version fehlen.

Der automatische Bau prüft das mit: `python3 tools/check_features.py`.
Bei jeder ✅-Zeile steht in der letzten Spalte ein **Prüfanker** (Text/Regex in `app/src/main` oder `app/build.gradle.kts`).
Fehlt ein Anker, schlägt der Bau fehl. Wird eine Funktion bewusst umgebaut, den Anker anpassen – nie einfach löschen.

Quellen: K = Konzept (Konzept-Dokument, 01.10.), N = Wunsch des Nutzers im Chat (mit Datum), V = eigener Vorschlag, vom Nutzer angenommen.
Status: ✅ fertig · 🧪 in Testversion · ⏳ geplant (Version) · 💭 vielleicht · ❌ bewusst nicht

## Grundsätze (gelten immer)
| ID | Regel | Quelle | Status | Prüfanker |
|---|---|---|---|---|
| G1 | Schlichte Oberfläche, kein Gewichtsverlauf, keine Motivation/Tipps/Streaks, kein Konto, keine Cloud, keine Werbung | K, N | ✅ | – |
| G2 | Keine Foto-Erkennung von Gerichten (Nährwert-Schilder per Texterkennung sind erlaubt) | N | ✅ | – |
| G3 | Daten bleiben auf dem Handy; Internet nur per Knopf/Scan (Update, Open Food Facts, später Rezept-Import) | K | ✅ | – |
| G4 | Updates ohne Datenverlust (feste Signatur, Datenbank-Migrationen mit Test) | K | ✅ | `AutoMigration\(from = 4, to = 5\)` |
| G5 | Optimiert fürs Oppo Reno 2 Z (Android 11), App nur für 64-Bit (kleiner Download) | N, 0.4.1 | ✅ | `abiFilters` |

## Tagesansicht (Heute)
| ID | Funktion | Quelle | Seit | Status | Prüfanker |
|---|---|---|---|---|---|
| T1 | Ring „kcal übrig“, Ziel, Gegessen, Anzahl Einträge | K | 0.3.0 | ✅ | `R.string.kcal_left` |
| T2 | Makro-Balken Eiweiß/Kohlenhydrate/Fett, abschaltbar | K | 0.3.0 | ✅ | `setShowMacros` |
| T3 | Zucker als 4. Balken mit Limit, abschaltbar; Hinweis „Einträge ohne Zucker“ | N 15.10. | 0.5.0 | ✅ | `R.string.sugar_missing` |
| T4 | Vier Mahlzeiten (Frühstück, Mittag, Abend, Snacks) | K | 0.3.0 | ✅ | `meal_snacks` |
| T5 | Tage wechseln mit Pfeilen, „Heute“-Sprung, nach Mitternacht automatisch auf heute | K | 0.3.0 | ✅ | `fun refreshToday` |
| T6 | Tage wechseln durch Wischen auf Kopf/Übersicht | K, V | 0.5.0 | ✅ | `fun Modifier.daySwipe` |
| T7 | Eintrag nach links wischen = löschen, mit „Rückgängig“ | N | 0.3.1 | ✅ | `fun SwipeToDelete` |
| T8 | Eintrag lange drücken → „Heute nochmal“ / Bearbeiten / Löschen | V | 0.5.0 | ✅ | `R.string.again_today` |
| T9 | „Wie gestern“ pro Mahlzeit (mit Rückgängig) | K | 0.5.0 | ✅ | `fun copyFromYesterday` |
| T10 | Mahlzeit als Vorlage speichern (⋮ an der Mahlzeit) | K | 0.6.0 | ✅ | `R.string.template_save` |
| T11 | Ziel pro Wochentag wird für den Tag verwendet | K | 0.6.0 | ✅ | `fun goalFor` |
| T12 | Aus dem Verlauf einen Tag öffnen | K | 0.6.0 | ✅ | `openDay.filterNotNull` |

## Eintragen
| ID | Funktion | Quelle | Seit | Status | Prüfanker |
|---|---|---|---|---|---|
| E1 | Reiter Suchen / Scannen / Foto / Schnell | K | 0.3.0/0.4.0 | ✅ | `R.string.tab_photo` |
| E2 | Suche in BLS 4.0 (eingebaut, offline) mit Synonymen und guter Sortierung | K | 0.3.0 | ✅ | `object SearchRanker` |
| E3 | „Zuletzt verwendet“ und Favoriten oben in der Suche | K | 0.3.0 | ✅ | `R.string.recently_used` |
| E4 | Mahlzeit wird nach Uhrzeit vorgewählt | K | 0.3.0 | ✅ | `fun forTime` |
| E5 | Portions-Chips (1 Scheibe, 1 Becher …) und Grammfeld, sinnvolle Vorschläge (Hauptwort, Wortende) | K, N | 0.3.0 | ✅ | `object PortionGuesser` |
| E6 | Menge in Stück/Scheiben mit Anzahl (½, 1, 1½, 2, 3 oder frei), Eintrag zeigt „1,5 × Scheibe (38 g)“ | N 15.10. | 0.5.0 | ✅ | `object UnitAmount` |
| E7 | Eigene Portion im Lebensmittel festlegen / aus Packung ausrechnen (Reiswaffeln) | N 19.10. | 0.7.0 | 🧪 | `private fun ServingDialog` |
| E8 | Favorit per Herz (im Lebensmittel und im Eintrag) | N | 0.3.0/0.3.1 | ✅ | `R.string.unfavorite` |
| E9 | Schnelleintrag nur mit kcal (+ Name), Herz merkt ihn als eigenes Lebensmittel | K, N | 0.3.0/0.3.1 | ✅ | `fun favoriteFromQuickEntry` |
| E10 | Schnelleintrag rechnet („250+120“, „2*150“) und wandelt kJ um | V | 0.6.0 | ✅ | `object QuickCalc` |
| E11 | „Hinzufügen und weiteres“ (Fenster bleibt offen) mit Rückgängig | V | 0.5.0 | ✅ | `R.string.add_and_more` |
| E12 | Rückgängig nach dem Hinzufügen | K | 0.4.0 | ✅ | `fun undo\(entry` |
| E13 | Mahlzeit-Vorlagen eintragen (oben in der Suche) mit Rückgängig | K | 0.6.0 | ✅ | `private fun TemplateRow` |
| E14 | Start-Reiter beim Eintragen wählbar | V | 0.6.0 | ✅ | `suspend fun startTab` |
| E15 | Nährwerte eines Lebensmittels korrigieren (Stift), „Auf Original zurücksetzen“, heutige Einträge neu berechnen | N 15.10. | 0.5.0 | ✅ | `private fun FoodValuesForm` |
| E16 | Hinweis zur Herkunft der Werte (BLS = Durchschnitt, OFF = Packung) | V | 0.5.0 | ✅ | `private fun sourceHint` |
| E17 | Drehen des Handys bricht nichts ab | N 19.10. | 0.7.0 | 🧪 | `android:configChanges` |

## Scannen und Foto
| ID | Funktion | Quelle | Seit | Status | Prüfanker |
|---|---|---|---|---|---|
| S1 | Barcode-Scan (CameraX + ML Kit, offline), doppelte Lesung + Prüfziffer | K | 0.4.0 | ✅ | `fun BarcodeCamera` |
| S2 | Taschenlampe, Barcode von Hand eintippen | K | 0.4.0 | ✅ | `scan_torch_on` |
| S3 | Open Food Facts mit lokalem Speicher (später offline) | K | 0.4.0 | ✅ | `object OpenFoodFacts` |
| S4 | Unbekannter Barcode → Produkt selbst anlegen | K | 0.4.0 | ✅ | `BarcodeUnknown` |
| S5 | Nährwerte per Foto (Kantinen-Monitor, Tabelle) auslesen – Texterkennung, kein Gericht-Erkennen | N | 0.4.0 | ✅ | `object NutritionTextParser` |
| S6 | Eigenes Lebensmittel anlegen (pro 100 g / 100 ml / Portion, mit Zucker) | K | 0.4.0 | ✅ | `private fun CustomFoodForm` |
| S7 | Zucker für früher gescannte Produkte nachladen (beim Scan + Knopf) | N 16.10. | 0.5.0 | ✅ | `fun refreshOffSugar` |

## Eintrag bearbeiten
| ID | Funktion | Quelle | Seit | Status | Prüfanker |
|---|---|---|---|---|---|
| B1 | Menge ändern (Gramm, Einheit+Anzahl, Portionen), Schnelleintrag Name/kcal | K | 0.3.0 | ✅ | `fun EditEntrySheet` |
| B2 | Mahlzeit und Tag ändern (verschieben) | K | 0.5.0 | ✅ | `fun DateRow` |
| B3 | Werte nur für diesen Eintrag überschreiben | N 15.10. | 0.5.0 | ✅ | `R.string.override_show` |
| B4 | Löschen (mit Rückgängig statt Nachfrage) | K | 0.3.0 | ✅ | `R.string.entry_deleted` |
| B5 | Zuckerwerte älterer Einträge automatisch nachtragen | N 16.10. | 0.5.0 | ✅ | `fun fillMissingSugar` |

## Verlauf
| ID | Funktion | Quelle | Seit | Status | Prüfanker |
|---|---|---|---|---|---|
| H1 | Verlauf 7/14/30 Tage, Ziellinie, Durchschnitt, Tage mit Einträgen / über dem Ziel, Tagesliste | K | 0.6.0 | ✅ | `fun HistoryScreen` |
| H2 | Ansicht nach Tagen / Wochen / Monaten (Ø pro Tag) | N 16.10. | 0.7.0 | 🧪 | `enum class HistoryGrouping` |
| H3 | Diagrammart wählbar: Balken, Linie, Fläche; Auswahl wird gemerkt | N 16.10. | 0.7.0 | 🧪 | `enum class ChartType` |

## Einstellungen, Sicherung, Technik
| ID | Funktion | Quelle | Seit | Status | Prüfanker |
|---|---|---|---|---|---|
| X1 | Tagesziel | K | 0.3.0 | ✅ | `setDailyGoal` |
| X2 | Eigene Makro-Ziele in Gramm + Zucker-Limit (leer = automatisch) | N 15.10. | 0.5.0 | ✅ | `fun MacroGoalsCard` |
| X3 | Ziel pro Wochentag | K | 0.6.0 | ✅ | `fun WeekGoalsCard` |
| X4 | Hell/Dunkel: wie Handy, hell, dunkel | V | 0.6.0 | ✅ | `enum class ThemeMode` |
| X5 | 8 Farbschemas (5 Pastell + 2× 90er + Standard), hell und dunkel | N 16.10. | 0.7.0 | 🧪 | `enum class Palette` |
| X6 | Automatische tägliche Sicherung (Download/Kalorienzaehler, 7 Stück), Jetzt sichern, Wiederherstellen | K | 0.5.0 | ✅ | `fun autoBackupIfDue` |
| X7 | CSV-Export zum Teilen | K | 0.5.0 | ✅ | `fun exportCsv` |
| X8 | Vorlagen verwalten (löschen mit Rückgängig) | K | 0.6.0 | ✅ | `fun TemplatesCard` |
| X9 | Eigene Lebensmittel verwalten (ändern, löschen) | V | 0.6.0 | ✅ | `fun OwnFoodsScreen` |
| X10 | Update-Knopf (GitHub), Testversionen anbieten | N | 0.3.0 | ✅ | `fun UpdateCard` |
| X11 | Fehlerprotokoll teilen | V | 0.6.0 | ✅ | `class CrashLog` |
| X12 | Quellenangabe BLS / Open Food Facts | K | 0.3.0 | ✅ | `settings_sources_text` |
| X13 | Daten löschen (alles zurücksetzen) | K | – | ⏳ 1.0 | – |

## Geplant
| ID | Funktion | Quelle | Status |
|---|---|---|---|
| P1 | Erinnerungen pro Mahlzeit (nur wenn noch leer), optionaler Tagesabschluss, Aktionen „Scannen“/„Wie gestern“ | K | ⏳ 1.0 |
| P2 | Oppo-Einrichtung (Autostart, Hintergrundaktivität) + Ersteinrichtung (Ziel, Erinnerungen, Widget) | K | ⏳ 1.0 |
| P3 | Widget 4×2 (kcal übrig, Scannen/Suchen/+kcal, Getränke-Knöpfe) und 2×1 | K | ⏳ 1.0 |
| P4 | App-Shortcuts (Scannen, Suchen, Schnell, Gestriges Frühstück) | K | ⏳ 1.0 |
| P5 | Getränke-Schnellknöpfe (Kaffee, Cola, Bier, Saft) | K | ⏳ 1.0 (Widget) |
| P6 | Schnelleinstellungs-Kachel „Scanner“ (optional) | K | ⏳ 1.0 |
| P7 | Eigene Rezepte (Zutaten, Portionen, Gesamtgewicht gekocht), Reste | K, N | ⏳ 1.1 (Block 7) |
| P8 | Rezept-Import per Teilen aus Chefkoch & Co. | K, N | ⏳ 1.2 (Block 8) |
| P9 | Mahlzeiten anpassen (umbenennen, ausblenden, Spätsnack) | V | ⏳ offen |
| P10 | Packungsanteil (¼ / ½ / ganze Packung), Rest-Wiegen, Spracheingabe | V | 💭 |
| P11 | Kantinen-Wochenplan (Foto/PDF) → Auswahlliste | N | 💭 |
| P12 | Ballaststoffe / Salz als weitere Werte | V | 💭 |
| P13 | Eigene Schriften wie im Prototyp | V | 💭 |
| P14 | Optionale Uhrzeit an Einträgen | V | 💭 |

## Offene Fehlermeldungen
| ID | Meldung | Datum | Status |
|---|---|---|---|
| F1 | „Lebensmittel lassen sich nicht mehr bearbeiten, sobald ausgewählt“ – genaue Stelle beim Nutzer erfragt | 19.10. | 🔍 offen |
