// ===== Einstellungen =====
function renderSettings(root) {
  const s = S.settings; const t = macroTargets(s.dailyGoal, { });
  const ownCount = Object.keys(S.own).length + Object.values(S.blsOverlay).filter(o => o.isAdjusted || o.servingGrams).length;
  const sw = (id, on, label, sub) => `<label class="switch"><span><b>${label}</b>${sub ? `<small>${sub}</small>` : ''}</span><input type="checkbox" id="${id}" ${on ? 'checked' : ''}><i></i></label>`;
  root.innerHTML = `<h1 class="pagetitle">Einstellungen</h1>
  <section class="card"><h2>Tagesziel</h2>
    <div class="inline"><label class="field grow"><span>Kalorien pro Tag</span><input id="goal" inputmode="numeric" value="${s.dailyGoal}"></label><button type="button" class="btn" id="goal-save">Speichern</button></div>
    ${sw('macros', s.showMacros, 'Makros anzeigen', 'Eiweiß, Kohlenhydrate und Fett als Balken')}</section>
  <section class="card"><h2>Ziel pro Wochentag</h2><p class="hint small">Leer lassen = dein normales Ziel (${fmtInt(s.dailyGoal)} kcal). Zum Beispiel am Wochenende etwas mehr.</p>
    <div class="grid2">${WEEKDAYS.map((k, i) => `<label class="field"><span>${WD_LABEL[i]}</span><input data-wd="${k}" inputmode="numeric" value="${s.weekdayGoals[k] ?? ''}"></label>`).join('')}</div>
    <div class="row-btns"><button type="button" class="btn ghost" id="wd-clear">Alle zurücksetzen</button><button type="button" class="btn" id="wd-save">Speichern</button></div></section>
  <section class="card"><h2>Eigene Makro-Ziele</h2><p class="hint small">Leer lassen = automatisch aus deinem Kalorienziel (${t.protein} g Eiweiß, ${t.carbs} g Kohlenhydrate, ${t.fat} g Fett, max. ${t.sugar} g Zucker).</p>
    <div class="grid2"><label class="field"><span>Eiweiß g (${t.protein})</span><input id="mg-p" inputmode="numeric" value="${s.proteinGoal ?? ''}"></label>
    <label class="field"><span>Kohlenh. g (${t.carbs})</span><input id="mg-c" inputmode="numeric" value="${s.carbsGoal ?? ''}"></label>
    <label class="field"><span>Fett g (${t.fat})</span><input id="mg-f" inputmode="numeric" value="${s.fatGoal ?? ''}"></label>
    <label class="field"><span>Zucker max. g (${t.sugar})</span><input id="mg-s" inputmode="numeric" value="${s.sugarLimit ?? ''}"></label></div>
    <div class="row-btns"><button type="button" class="btn ghost" id="mg-auto">Automatisch</button><button type="button" class="btn" id="mg-save">Speichern</button></div>
    ${sw('sugar', s.showSugar, 'Zucker anzeigen', 'Vierter Balken auf der Heute-Seite')}</section>
  <section class="card"><h2>Darstellung</h2><span class="label">Farbschema</span>
    <div class="swatches">${Object.entries(PALETTES).map(([k, p]) => { const c = isDark() ? p.dark : p.light;
      return `<button type="button" class="swatch${s.palette === k ? ' on' : ''}" data-pal="${k}" style="background:${c.bg};color:${c.fg};border-color:${s.palette === k ? 'var(--accent)' : c.line}">
        <span class="dots">${[c.accent, c.accentSoft, c.over, c.protein].map(x => `<i style="background:${x};border-color:${c.line}"></i>`).join('')}</span>${esc(p.name)}</button>`; }).join('')}</div>
    <span class="label">Hell oder dunkel</span>${chips([['SYSTEM', 'Wie das Gerät'], ['LIGHT', 'Hell'], ['DARK', 'Dunkel']], s.themeMode, 'theme')}
    <span class="label">Beim Eintragen zuerst öffnen</span>${chips([[0, 'Suchen'], [3, 'Schnell']], s.startTab === 3 ? 3 : 0, 'starttab')}</section>
  <section class="card"><h2>Vorlagen</h2>${S.templates.length ? S.templates.map(tp => `<div class="lrow"><span class="fname"><b>${esc(tp.name)}</b>
      <small>${tp.items.length} Teile · ${fmtInt(tp.items.reduce((a, i) => a + i.kcal, 0))} kcal · ${esc(tp.items.map(i => i.name).join(', '))}</small></span>
      <button type="button" class="icon-btn" data-deltpl="${tp.id}" aria-label="Vorlage löschen">${ICON.trash}</button></div>`).join('')
    : '<p class="hint small">Noch keine Vorlagen. In der Tagesansicht bei einer Mahlzeit auf ⋮ → „Als Vorlage speichern“.</p>'}</section>
  <section class="card"><h2>Eigene Lebensmittel</h2><p class="hint small">Selbst angelegte, gescannte und korrigierte Lebensmittel. Antippen zum Ändern oder Löschen.</p>
    <button type="button" class="btn ghost wide" id="own">Anzeigen (${ownCount})</button></section>
  <section class="card"><h2>Sicherung</h2><p class="hint small">Gleiches Format wie die Handy-App: Eine Sicherung aus „Download/Kalorienzaehler“ vom Handy lässt sich hier einspielen – und umgekehrt.</p>
    <div class="row-btns"><button type="button" class="btn ghost" id="bk-out">Sicherung erstellen</button><button type="button" class="btn ghost" id="bk-in">Wiederherstellen</button></div>
    <button type="button" class="btn ghost wide" id="csv">Als CSV kopieren</button></section>
  <section class="card about"><h2>Über</h2><dl>
    <div><dt>Version</dt><dd>Web-Ausgabe, Stand App 0.7</dd></div>
    <div><dt>Speicherort</dt><dd>${storageOk ? 'Nur in diesem Browser auf diesem Gerät. Nichts wird hochgeladen. Browserdaten löschen löscht auch die Einträge – deshalb ab und zu eine Sicherung erstellen.'
      : 'Dieser Browser erlaubt gerade kein Speichern (z. B. privates Fenster). Einträge gehen beim Schließen verloren – bitte Sicherung erstellen.'}</dd></div>
    <div><dt>Datenquellen</dt><dd>Lebensmittel: Bundeslebensmittelschlüssel (BLS) 4.0, Max Rubner-Institut, Lizenz CC BY 4.0 (blsdb.de). Nährwerte je 100 g essbarem Anteil, vereinfacht. Barcodes in der App: Open Food Facts (ODbL).</dd></div></dl></section>`;

  const intOrNull = v => { const n = parseInt(String(v).trim(), 10); return Number.isFinite(n) && n >= 0 ? n : null; };
  $('#goal-save').onclick = () => { const g = intOrNull($('#goal').value);
    if (g == null || g < 800 || g > 6000) { snack('Bitte ein Ziel zwischen 800 und 6.000 kcal eingeben'); return; }
    s.dailyGoal = g; save(); render(); snack(`Tagesziel: ${fmtInt(g)} kcal`); };
  $('#macros').onchange = ev => { s.showMacros = ev.target.checked; save(); };
  $('#sugar').onchange = ev => { s.showSugar = ev.target.checked; save(); };
  $('#wd-save').onclick = () => { const o = {}; let bad = false;
    $$('[data-wd]').forEach(i => { if (!i.value.trim()) return; const n = intOrNull(i.value); if (n == null || n < 800 || n > 6000) bad = true; else o[i.dataset.wd] = n; });
    if (bad) { snack('Bitte Ziele zwischen 800 und 6.000 kcal eingeben'); return; } s.weekdayGoals = o; save(); render(); snack('Wochentag-Ziele gespeichert'); };
  $('#wd-clear').onclick = () => { s.weekdayGoals = {}; save(); render(); };
  $('#mg-save').onclick = () => { s.proteinGoal = intOrNull($('#mg-p').value); s.carbsGoal = intOrNull($('#mg-c').value); s.fatGoal = intOrNull($('#mg-f').value);
    s.sugarLimit = intOrNull($('#mg-s').value); save(); render(); snack('Makro-Ziele gespeichert'); };
  $('#mg-auto').onclick = () => { s.proteinGoal = s.carbsGoal = s.fatGoal = s.sugarLimit = null; save(); render(); };
  $$('[data-pal]').forEach(b => b.onclick = () => { s.palette = b.dataset.pal; save(); applyTheme(); render(); });
  $$('[data-theme]', root).forEach(b => b.onclick = () => { s.themeMode = b.dataset.theme; save(); applyTheme(); render(); });
  $$('[data-starttab]').forEach(b => b.onclick = () => { s.startTab = +b.dataset.starttab; save(); render(); });
  $$('[data-deltpl]').forEach(b => b.onclick = () => { const i = S.templates.findIndex(x => x.id === b.dataset.deltpl); const [tp] = S.templates.splice(i, 1); save(); render();
    snack(`Vorlage „${tp.name}“ gelöscht`, () => { S.templates.splice(i, 0, tp); save(); }); });
  $('#own').onclick = openOwnFoods;
  $('#bk-out').onclick = () => textSheet('Sicherung', 'Die Ansicht im Browser kann keine Dateien speichern. Kopiere den Text und speichere ihn z. B. als „kalorienzaehler-backup.json“ – die Handy-App kann ihn wiederherstellen.', exportBackup());
  $('#csv').onclick = () => textSheet('CSV', 'Kopieren und in eine Tabelle (z. B. Excel) einfügen. Trennzeichen ist das Semikolon.', exportCsv());
  $('#bk-in').onclick = openRestore;
}
function copyText(ta, btn) {
  const done = () => { btn.textContent = 'Kopiert'; setTimeout(() => btn.textContent = 'Kopieren', 1500); };
  navigator.clipboard?.writeText(ta.value).then(done, () => { ta.focus(); ta.select(); btn.textContent = 'Markiert – jetzt kopieren'; });
  if (!navigator.clipboard) { ta.focus(); ta.select(); }
}
function textSheet(title, hint, text) {
  sheetStack = []; openSheet(b => { b.innerHTML = `<h2 class="sheet-title">${title}</h2><p class="hint small">${hint}</p>
    <textarea id="tx" readonly rows="10">${esc(text)}</textarea>
    <div class="row-btns"><button type="button" class="btn ghost" id="tx-x">Schließen</button><button type="button" class="btn" id="tx-copy">Kopieren</button></div>`;
    $('#tx-x', b).onclick = closeSheet; $('#tx-copy', b).onclick = ev => copyText($('#tx', b), ev.currentTarget); });
}
function openRestore() {
  let text = '';
  sheetStack = []; openSheet(function draw(b) {
    b.innerHTML = `<h2 class="sheet-title">Wiederherstellen</h2><p class="hint small">Sicherungsdatei wählen (z. B. vom Handy: Download/Kalorienzaehler) oder den Text einfügen.</p>
      <label class="btn ghost wide file">Datei wählen<input type="file" id="rs-file" accept=".json,application/json,text/plain"></label>
      <textarea id="rs-text" rows="6" placeholder="… oder Sicherungstext hier einfügen">${esc(text)}</textarea>
      <p class="note warn">Achtung: Alle jetzigen Einträge und eigenen Lebensmittel werden durch die Sicherung ersetzt.</p>
      <p class="hint small bad" id="rs-err" hidden></p>
      <div class="row-btns"><button type="button" class="btn ghost" id="rs-x">Abbrechen</button><button type="button" class="btn danger" id="rs-ok">Ja, ersetzen</button></div>`;
    $('#rs-x', b).onclick = closeSheet;
    $('#rs-file', b).onchange = ev => { const f = ev.target.files[0]; if (!f) return; const r = new FileReader();
      r.onload = () => { text = String(r.result); $('#rs-text', b).value = text; }; r.readAsText(f); };
    $('#rs-ok', b).onclick = () => { text = $('#rs-text', b).value; const err = $('#rs-err', b);
      try { const n = importBackup(text); applyTheme(); closeAllSheets(); curDate = D.today(); snack(`Wiederhergestellt: ${n} Einträge`); }
      catch (e) { err.hidden = false; err.textContent = e instanceof SyntaxError ? 'Das ist kein gültiger Sicherungstext.' : e.message; } };
  });
}
function openOwnFoods() {
  let filter = '';
  sheetStack = []; openSheet(function draw(b) {
    const list = [...Object.values(S.own), ...Object.entries(S.blsOverlay).filter(([, o]) => o.isAdjusted || o.servingGrams).map(([c]) => food('b:' + c))]
      .filter(Boolean).sort((a, c) => a.name.localeCompare(c.name, 'de'));
    b.innerHTML = `<div class="sheet-top"><h2 class="sheet-title">Eigene Lebensmittel</h2><button type="button" class="link" id="of-x">Fertig</button></div>
      <label class="search"><span aria-hidden="true">${ICON.search}</span><input id="of-q" type="search" placeholder="Filtern" value="${esc(filter)}"></label><div id="of-list"></div>`;
    const drawList = () => { const k = normalize(filter).replace(/ /g, '');
      const shown = list.filter(f => !k || normalize(f.name + ' ' + (f.brand || '')).replace(/ /g, '').includes(k));
      $('#of-list', b).innerHTML = shown.length ? shown.map(f => `<button type="button" class="foodrow" data-of="${f.id}"><span class="fname"><b>${esc(f.name)}</b>
        <small>${esc([f.brand, sourceLabel(f), f.isAdjusted ? 'angepasst' : null, f.servingGrams ? `${f.servingLabel || '1 Portion'} = ${fmtNum(f.servingGrams)} ${f.isLiquid ? 'ml' : 'g'}` : null].filter(Boolean).join(' · '))}</small></span>
        <span class="muted small">${per100Label(f)}</span></button>`).join('') : '<p class="hint">Keine Lebensmittel gefunden.</p>';
      $$('[data-of]', b).forEach(r => r.onclick = () => openSheet(ownFoodSheet(food(r.dataset.of)))); };
    $('#of-q', b).oninput = ev => { filter = ev.target.value; drawList(); };
    $('#of-x', b).onclick = closeAllSheets; drawList();
  });
}
function ownFoodSheet(f) {
  let confirmDel = false;
  return function draw(b) {
    const g = food(f.id) || f;
    b.innerHTML = `<h2 class="sheet-title">${esc(g.name)}</h2><p class="muted small">${esc([g.brand, sourceLabel(g), per100Label(g)].filter(Boolean).join(' · '))}</p>
      <div class="menu"><button type="button" id="of-values">Nährwerte ändern</button>
      <button type="button" id="of-serving">${g.servingGrams ? 'Eigene Portion ändern' : 'Eigene Portion festlegen'}</button>
      ${g.source !== 'BLS' ? (confirmDel ? `<p class="note warn">Deine bisherigen Einträge bleiben mit ihren Werten erhalten.</p><button type="button" id="of-del2" class="danger">Ja, löschen</button>`
        : '<button type="button" id="of-del" class="danger">Lebensmittel löschen</button>') : ''}
      <button type="button" id="of-back">Zurück</button></div>`;
    $('#of-values', b).onclick = () => openSheet(valuesSheet(g, () => {}));
    $('#of-serving', b).onclick = () => openSheet(servingSheet(g, () => {}));
    $('#of-del', b)?.addEventListener('click', () => { confirmDel = true; redrawSheet(); });
    $('#of-del2', b)?.addEventListener('click', () => { const copy = { ...S.own[g.id] }; deleteOwn(g); closeSheet();
      snack(`${g.name} gelöscht`, () => { S.own[copy.id] = copy; save(); }); });
    $('#of-back', b).onclick = closeSheet;
  };
}

// ===== Navigation (#heute, #verlauf, #einstellungen) =====
const VIEWS = { heute: renderToday, verlauf: renderHistory, einstellungen: renderSettings };
function view() { const h = location.hash.slice(1); return VIEWS[h] ? h : 'heute'; }
function go(v) { if (location.hash.slice(1) !== v) location.hash = v; else render(); window.scrollTo(0, 0); }
function render() {
  const v = view(); const main = $('#main'); VIEWS[v](main);
  $$('.nav a').forEach(a => a.classList.toggle('on', a.dataset.view === v));
}
window.addEventListener('hashchange', () => { render(); window.scrollTo(0, 0); });
let lastW = innerWidth; window.addEventListener('resize', () => { if (Math.abs(innerWidth - lastW) > 40 && view() === 'verlauf') { lastW = innerWidth; render(); } });
$('#sheet-wrap').addEventListener('click', ev => { if (ev.target.id === 'sheet-wrap') closeAllSheets(); });
document.addEventListener('keydown', ev => { if (ev.key === 'Escape' && sheetStack.length) closeSheet(); });
document.addEventListener('visibilitychange', () => { if (!document.hidden && curDate !== D.today() && sheetStack.length === 0) { /* nach Mitternacht auf heute springen */ } });
applyTheme(); render();
