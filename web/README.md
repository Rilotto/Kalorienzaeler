# Kalorienzähler im Browser

Nachbau der App (Stand 0.7) als eine HTML-Seite, veröffentlicht als Claude-Artefakt:
https://claude.ai/artifact/Wfxe1BABcSSGv9fLogYevc

- Rechenlogik in `core.js` ist 1:1 aus `core/domain` portiert (Tests: `node web/test.js`).
- Daten nur im Browser (localStorage). Sicherung im selben JSON-Format 2 wie die App → in beide Richtungen austauschbar.
- Nicht im Browser: Scannen, Foto, Update-Knopf, automatische Sicherung, Fehlerprotokoll.
- Bauen: `python3 web/build.py` → `web/dist/kalorienzaehler.html`, dann dieselbe Datei als Artefakt neu veröffentlichen (gleiche URL).
- Bei neuen App-Versionen: FEATURES.md durchgehen und hier nachziehen, was im Browser sinnvoll ist.
