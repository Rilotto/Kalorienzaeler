// ===== Eintragen =====
function openAdd(meal) {
  const st = { meal, date: curDate, tab: S.settings.startTab === 3 ? 'quick' : 'search', q: '', lastAdded: null };
  const added = (entry, keepOpen) => {
    if (keepOpen) { st.lastAdded = entry; st.q = ''; replaceSheet(main); return; }
    closeAllSheets(); snack(`${entry.name} zu ${MEAL_LABEL[entry.meal]} hinzugefügt`, () => removeEntries([entry.id]));
  };
  function main(b) {
    b.innerHTML = `<div class="sheet-top"><h2 class="sheet-title">Zu ${MEAL_LABEL[st.meal]} hinzufügen</h2><button type="button" class="link" id="a-close">Fertig</button></div>
      ${st.lastAdded ? `<div class="banner"><span>${esc(st.lastAdded.name)} eingetragen (${st.lastAdded.kcal} kcal)</span><button type="button" class="link" id="a-undo">Rückgängig</button></div>` : ''}
      <div class="tabs" role="tablist"><button type="button" role="tab" data-tab="search" class="${st.tab === 'search' ? 'on' : ''}">Suchen</button>
        <button type="button" role="tab" data-tab="quick" class="${st.tab === 'quick' ? 'on' : ''}">Schnell</button></div>
      <div id="tab-body"></div>`;
    $('#a-close', b).onclick = closeAllSheets;
    $('#a-undo', b)?.addEventListener('click', () => { removeEntries([st.lastAdded.id]); st.lastAdded = null; replaceSheet(main); });
    $$('[data-tab]', b).forEach(t => t.onclick = () => { st.tab = t.dataset.tab; replaceSheet(main); });
    const tb = $('#tab-body', b);
    if (st.tab === 'search') searchTab(tb); else quickTab(tb);
  }
  function searchTab(tb) {
    tb.innerHTML = `<label class="search"><span aria-hidden="true">${ICON.search}</span><input id="q" type="search" placeholder="Lebensmittel suchen, z. B. Haferflocken" value="${esc(st.q)}" autocomplete="off"></label>
      <p class="hint small">Scannen und Foto gibt es in der Handy-App.</p><div id="results"></div>`;
    const q = $('#q', tb); const res = $('#results', tb);
    const draw = () => {
      st.q = q.value; let h = '';
      const row = f => `<button type="button" class="foodrow" data-food="${f.id}"><span class="fname"><b>${esc(f.name)}</b><small>${esc([f.brand, sourceLabel(f)].filter(Boolean).join(' · '))}</small></span>
        ${f.favorite ? `<span class="fav">${ICON.heart}</span>` : ''}<span class="muted small">${per100Label(f)}</span></button>`;
      if (!st.q.trim()) {
        const tpls = [...S.templates].sort((a, b) => (b.useCount || 0) - (a.useCount || 0) || a.name.localeCompare(b.name, 'de'));
        if (tpls.length) h += `<h3 class="label">Vorlagen</h3>` + tpls.map(t => `<button type="button" class="foodrow" data-tpl="${t.id}"><span class="plus">${ICON.plus}</span>
          <span class="fname"><b>${esc(t.name)}</b><small>${esc(t.items.map(i => i.name).join(', '))}</small></span><span class="muted small">${fmtInt(t.items.reduce((a, i) => a + i.kcal, 0))} kcal</span></button>`).join('');
        const fav = favorites(); if (fav.length) h += `<h3 class="label">Favoriten</h3>` + fav.map(row).join('');
        const rec = recent(); if (rec.length) h += `<h3 class="label">Zuletzt verwendet</h3>` + rec.map(row).join('');
        if (!tpls.length && !fav.length && !rec.length) h += `<p class="hint">Tippe oben, was du gegessen hast. Favoriten und zuletzt Verwendetes erscheinen dann hier.</p>`;
      } else {
        const r = searchFoods(st.q);
        h += r.length ? r.map(row).join('') : `<p class="hint">Nichts gefunden für „${esc(st.q)}“. Leg es unten selbst an.</p>`;
      }
      h += `<button type="button" class="foodrow create" id="create"><span class="plus">${ICON.plus}</span><b>Eigenes Lebensmittel anlegen</b></button>`;
      res.innerHTML = h;
      $$('[data-food]', res).forEach(x => x.onclick = () => openSheet(detailSheet(food(x.dataset.food))));
      $$('[data-tpl]', res).forEach(x => x.onclick = () => {
        const t = S.templates.find(y => y.id === x.dataset.tpl); let now = Date.now();
        const list = t.items.map(i => ({ ...i, id: newId('e'), date: st.date, meal: st.meal, createdAt: now++ }));
        restoreEntries(list); list.forEach(e => e.foodId && markUsed(e.foodId)); t.useCount = (t.useCount || 0) + 1; save();
        closeAllSheets(); snack(`${t.name} eingetragen (${fmtInt(list.reduce((a, e) => a + e.kcal, 0))} kcal)`, () => removeEntries(list.map(e => e.id)));
      });
      $('#create', res).onclick = () => openSheet(customSheet({ name: st.q.trim() }));
    };
    let timer; q.oninput = () => { clearTimeout(timer); timer = setTimeout(draw, 120); };
    draw(); setTimeout(() => q.focus(), 50);
  }
  function quickTab(tb) {
    tb.innerHTML = `<p class="hint">Nur Kalorien, z. B. für die Kantine oder unterwegs.</p>
      <label class="field"><span>Bezeichnung</span><input id="qn" maxlength="60" placeholder="Schnelleintrag"></label>
      <label class="field"><span id="qk-label">Kalorien (kcal)</span><input id="qk" inputmode="decimal" placeholder="z. B. 650 oder 250+120"></label>
      <p class="hint small" id="qk-hint">Auch Rechnen geht, z. B. 250+120</p>
      <div class="chips"><button type="button" class="chip" id="qkj">Wert ist in kJ</button></div>
      <span class="label">Mahlzeit</span>${mealChips(st.meal)}
      <button type="button" class="btn wide" id="q-add" disabled>Hinzufügen</button>`;
    let kj = false; const k = $('#qk', tb);
    const upd = () => {
      const input = kj ? `${k.value} kJ` : k.value; const v = quickCalc(input) ?? 0; const hint = $('#qk-hint', tb);
      $('#qk-label', tb).textContent = kj ? 'Kilojoule (kJ)' : 'Kalorien (kcal)';
      if (k.value.trim() && quickCalc(input) == null) { hint.textContent = 'Das kann ich nicht ausrechnen'; hint.className = 'hint small bad'; }
      else if (isCalculation(input) && v > 0) { hint.textContent = `= ${fmtInt(v)} kcal`; hint.className = 'hint small good'; }
      else { hint.textContent = 'Auch Rechnen geht, z. B. 250+120'; hint.className = 'hint small'; }
      $('#q-add', tb).disabled = !(v > 0); return v;
    };
    k.oninput = upd;
    $('#qkj', tb).onclick = ev => { kj = !kj; ev.currentTarget.classList.toggle('on', kj); upd(); };
    $$('[data-meal]', tb).forEach(c => c.onclick = () => { st.meal = c.dataset.meal; $$('[data-meal]', tb).forEach(x => x.classList.toggle('on', x === c)); });
    $('#q-add', tb).onclick = () => { const v = upd(); const name = $('#qn', tb).value.trim() || 'Schnelleintrag';
      const e = addEntry({ date: st.date, meal: st.meal, foodId: null, name, brand: null, grams: null, isLiquid: false, kcal: v, protein: 0, carbs: 0, fat: 0, sugar: null, unitLabel: null, unitCount: null });
      closeAllSheets(); snack(`${fmtInt(v)} kcal eingetragen`, () => removeEntries([e.id])); };
  }
  // --- Lebensmittel: Menge wählen
  function detailSheet(f0) {
    let f = f0; const ds = { unit: undefined, count: '1', grams: null, servingOpen: false };
    return function draw(b) {
      f = food(f.id) || f;
      const serving = f.servingGrams > 0 ? { label: f.servingLabel || '1 Portion', grams: f.servingGrams } : null;
      const portions = portionsFor(f.name, f.isLiquid, serving);
      const units = portions.filter(p => !isPlainAmount(p.label)).map(p => ({ label: unitName(p.label), grams: p.grams }));
      if (ds.unit === undefined) ds.unit = units[0] || null;
      if (ds.grams == null) ds.grams = fmtNum(portions[0].grams);
      const unit = f.isLiquid ? 'ml' : 'g';
      const byPortion = f.isPortionFood;
      const count = parseNum(ds.count);
      const value = byPortion ? count : ds.unit ? (count == null ? null : count * ds.unit.grams) : parseNum(ds.grams);
      const a = value == null ? null : amountOf(f, byPortion ? value * 100 : value);
      const hint = f.isAdjusted ? 'Von dir korrigierte Werte.' : f.source === 'BLS' ? 'Durchschnittswert (BLS) – Packungsangaben können abweichen.'
        : f.source === 'OFF' ? 'Werte von der Packung (Open Food Facts).' : 'Eigenes Lebensmittel.';
      b.innerHTML = `<div class="sheet-top"><div><h2 class="sheet-title">${esc(f.name)}</h2><p class="muted small">${esc([f.brand, per100Label(f), sourceLabel(f)].filter(Boolean).join(' · '))}</p></div>
        <div class="top-icons"><button type="button" class="icon-btn" id="d-edit" aria-label="Werte korrigieren">${ICON.pen}</button>
        <button type="button" class="icon-btn${f.favorite ? ' fav' : ''}" id="d-fav" aria-label="${f.favorite ? 'Aus Favoriten entfernen' : 'Als Favorit merken'}">${ICON.heart}</button></div></div>
        <p class="hint small">${hint}</p>
        <div class="bigkcal"><b>${a ? fmtInt(a.kcal) : '–'}</b> kcal ${!byPortion && ds.unit && value != null ? `<span class="muted"> · ${fmtNum(value)} ${unit}</span>` : ''}</div>
        ${a && a.protein + a.carbs + a.fat > 0 ? `<p class="muted small">Eiweiß ${Math.round(a.protein)} g · Kohlenhydrate ${Math.round(a.carbs)} g · Fett ${Math.round(a.fat)} g${a.sugar != null ? ` · davon Zucker ${Math.round(a.sugar)} g` : ''}</p>` : ''}
        ${byPortion ? `${chips(COUNTS, count, 'count')}<label class="field"><span>Anzahl Portionen</span><input id="d-count" inputmode="decimal" value="${esc(ds.count)}"></label>`
        : `<span class="label">Einheit</span><div class="chips"><button type="button" class="chip${!ds.unit ? ' on' : ''}" data-unit="">${unit}</button>
          ${units.map((u, i) => `<button type="button" class="chip${ds.unit && ds.unit.label === u.label && ds.unit.grams === u.grams ? ' on' : ''}" data-unit="${i}">${esc(u.label)} (${fmtNum(u.grams)} ${unit})</button>`).join('')}</div>
          <button type="button" class="link" id="d-serving">${f.servingGrams ? 'Eigene Portion ändern' : '+ Eigene Portion festlegen'}</button>
          ${ds.unit ? `${chips(COUNTS, count, 'count')}<label class="field"><span>Wie viele × ${esc(ds.unit.label)}?</span><input id="d-count" inputmode="decimal" value="${esc(ds.count)}"></label>`
          : `${chips([50, 100, 150, 200, 250].map(g => [g, `${g} ${unit}`]), parseNum(ds.grams), 'grams')}<label class="field"><span>Menge in ${unit}</span><input id="d-grams" inputmode="decimal" value="${esc(ds.grams)}"></label>`}`}
        <span class="label">Mahlzeit</span>${mealChips(st.meal)}
        <div class="row-btns"><button type="button" class="btn ghost" id="d-back">Zurück</button><button type="button" class="btn" id="d-add" ${value > 0 ? '' : 'disabled'}>Hinzufügen</button></div>
        <button type="button" class="link center" id="d-more" ${value > 0 ? '' : 'disabled'}>Hinzufügen und weiteres</button>`;
      const re = () => redrawSheet();
      $('#d-back', b).onclick = closeSheet;
      $('#d-fav', b).onclick = () => { patchFood(f, { favorite: !f.favorite }); re(); };
      $('#d-edit', b).onclick = () => openSheet(valuesSheet(f, () => {}));
      $('#d-serving', b)?.addEventListener('click', () => openSheet(servingSheet(f, () => { ds.unit = undefined; ds.count = '1'; })));
      $$('[data-unit]', b).forEach(c => c.onclick = () => { if (c.dataset.unit === '') { if (value != null) ds.grams = fmtNum(value); ds.unit = null; }
        else { ds.unit = units[+c.dataset.unit]; ds.count = '1'; } re(); });
      $$('[data-count]', b).forEach(c => c.onclick = () => { ds.count = fmtNum(+c.dataset.count); re(); });
      $$('[data-grams]', b).forEach(c => c.onclick = () => { ds.grams = c.dataset.grams; re(); });
      const live = (sel, key) => { const el = $(sel, b); if (!el) return; el.oninput = () => { ds[key] = el.value; };
        el.onchange = re; el.onkeydown = ev => { if (ev.key === 'Enter') re(); }; };
      live('#d-count', 'count'); live('#d-grams', 'grams');
      $$('[data-meal]', b).forEach(c => c.onclick = () => { st.meal = c.dataset.meal; re(); });
      const add = keep => { const cnt = parseNum(ds.count); const v = byPortion ? cnt : ds.unit ? (cnt == null ? null : cnt * ds.unit.grams) : parseNum(ds.grams);
        if (!(v > 0)) { re(); return; }
        const e = addEntry(byPortion ? entryPortions(f, v, st.meal, st.date) : entryFromFood(f, v, st.meal, st.date, ds.unit?.label, ds.unit ? cnt : null));
        if (!keep) { added(e, false); return; } sheetStack.pop(); added(e, true); };
      $('#d-add', b).onclick = () => add(false); $('#d-more', b).onclick = () => add(true);
    };
  }
  function customSheet(pre) {
    const v = { name: pre.name || '', brand: '', basis: 'G', kcal: '', protein: '', carbs: '', fat: '', sugar: '', serving: '', fav: false };
    return b => {
      b.innerHTML = `<h2 class="sheet-title">Eigenes Lebensmittel</h2>
        <label class="field"><span>Name</span><input id="c-name" maxlength="80" value="${esc(v.name)}"></label>
        <label class="field"><span>Marke (optional)</span><input id="c-brand" maxlength="60" value="${esc(v.brand)}"></label>
        <span class="label">Werte gelten</span>${chips([['G', 'pro 100 g'], ['ML', 'pro 100 ml'], ['PORTION', 'pro Portion']], v.basis, 'basis')}
        <div class="grid2"><label class="field"><span>kcal</span><input id="c-kcal" inputmode="decimal" value="${esc(v.kcal)}"></label>
        <label class="field"><span>Eiweiß g</span><input id="c-protein" inputmode="decimal" value="${esc(v.protein)}"></label>
        <label class="field"><span>Kohlenh. g</span><input id="c-carbs" inputmode="decimal" value="${esc(v.carbs)}"></label>
        <label class="field"><span>Fett g</span><input id="c-fat" inputmode="decimal" value="${esc(v.fat)}"></label>
        <label class="field"><span>Zucker g (optional)</span><input id="c-sugar" inputmode="decimal" value="${esc(v.sugar)}"></label>
        ${v.basis !== 'PORTION' ? `<label class="field"><span>Portion in ${v.basis === 'ML' ? 'ml' : 'g'} (optional)</span><input id="c-serving" inputmode="decimal" value="${esc(v.serving)}"></label>` : ''}</div>
        <label class="check"><input type="checkbox" id="c-fav" ${v.fav ? 'checked' : ''}> Als Favorit merken</label>
        <p class="hint small bad" id="c-err" hidden>Bitte Name und Kalorien angeben.</p>
        <div class="row-btns"><button type="button" class="btn ghost" id="c-back">Zurück</button><button type="button" class="btn" id="c-save">Speichern</button></div>`;
      const read = () => { for (const k of ['name', 'brand', 'kcal', 'protein', 'carbs', 'fat', 'sugar', 'serving']) { const el = $('#c-' + k, b); if (el) v[k] = el.value; } v.fav = $('#c-fav', b).checked; };
      $$('[data-basis]', b).forEach(c => c.onclick = () => { read(); v.basis = c.dataset.basis; redrawSheet(); });
      $('#c-back', b).onclick = closeSheet;
      $('#c-save', b).onclick = () => { read(); const k = parseNum(v.kcal);
        if (!v.name.trim() || k == null) { $('#c-err', b).hidden = false; return; }
        const f = createCustom({ name: v.name, brand: v.brand, basis: v.basis, favorite: v.fav, servingGrams: parseNum(v.serving),
          per: { kcal: k, protein: parseNum(v.protein) ?? 0, carbs: parseNum(v.carbs) ?? 0, fat: parseNum(v.fat) ?? 0, sugar: parseNum(v.sugar) } });
        replaceSheet(detailSheet(f)); };
    };
  }
  sheetStack = []; openSheet(main);
}

// --- Nährwerte korrigieren (Stift)
function valuesSheet(f0, after) {
  const f = food(f0.id) || f0; const custom = f.source === 'CUSTOM';
  const fm = v => v == null ? '' : fmtNum(v);
  const basis = f.isPortionFood ? 'pro Portion' : f.isLiquid ? 'pro 100 ml' : 'pro 100 g';
  return b => {
    b.innerHTML = `<h2 class="sheet-title">Nährwerte korrigieren</h2><p class="muted small">${esc(f.name)}${custom ? '' : ' · ' + sourceLabel(f)}</p>
      <p class="note">${custom ? 'Ändert dein eigenes Lebensmittel für alle neuen Einträge.' : 'Gilt ab jetzt für alle neuen Einträge. Alte Tage bleiben, wie sie sind.'}</p>
      ${custom ? `<label class="field"><span>Name</span><input id="v-name" value="${esc(f.name)}"></label>` : ''}
      <span class="label">Werte ${basis}</span>
      <div class="grid2"><label class="field"><span>kcal</span><input id="v-kcal" inputmode="decimal" value="${Math.round(f.kcal100)}"></label>
      <label class="field"><span>Eiweiß g</span><input id="v-protein" inputmode="decimal" value="${fm(f.protein100)}"></label>
      <label class="field"><span>Kohlenh. g</span><input id="v-carbs" inputmode="decimal" value="${fm(f.carbs100)}"></label>
      <label class="field"><span>Fett g</span><input id="v-fat" inputmode="decimal" value="${fm(f.fat100)}"></label>
      <label class="field"><span>Zucker g</span><input id="v-sugar" inputmode="decimal" value="${fm(f.sugar100)}"></label></div>
      ${f.isAdjusted && f.origKcal100 != null ? `<p class="muted small">Original: ${Math.round(f.origKcal100)} kcal</p>` : ''}
      <label class="check"><input type="checkbox" id="v-recalc" checked> Einträge des angezeigten Tages damit neu berechnen</label>
      ${f.isAdjusted ? '<button type="button" class="link" id="v-reset">Auf Original zurücksetzen</button>' : ''}
      <div class="row-btns"><button type="button" class="btn ghost" id="v-back">Zurück</button><button type="button" class="btn" id="v-save">Speichern</button></div>`;
    const recalc = g => { if ($('#v-recalc', b).checked) recalcForFood(g, curDate); };
    $('#v-back', b).onclick = closeSheet;
    $('#v-reset', b)?.addEventListener('click', () => { const g = resetFood(f); recalc(g); closeSheet(); after(); redrawSheet(); });
    $('#v-save', b).onclick = () => { const k = parseNum($('#v-kcal', b).value); if (k == null) return;
      const g = correctFood(f, { kcal: k, protein: parseNum($('#v-protein', b).value) ?? 0, carbs: parseNum($('#v-carbs', b).value) ?? 0,
        fat: parseNum($('#v-fat', b).value) ?? 0, sugar: parseNum($('#v-sugar', b).value) }, $('#v-name', b)?.value ?? f.name);
      recalc(g); closeSheet(); after(); redrawSheet(); };
  };
}
// --- eigene Portion (z. B. Reiswaffeln: Packung 100 g, 6 Stück → 16,7 g)
function servingSheet(f, after) {
  const unit = f.isLiquid ? 'ml' : 'g';
  return b => {
    b.innerHTML = `<h2 class="sheet-title">Eigene Portion</h2><p class="hint">Die App merkt sich die Portion bei diesem Lebensmittel und schlägt sie zuerst vor.</p>
      <label class="field"><span>Bezeichnung</span><input id="s-label" maxlength="30" value="${esc(f.servingLabel || '1 Stück')}"></label>
      <label class="field"><span>Menge in ${unit}</span><input id="s-grams" inputmode="decimal" value="${f.servingGrams ? fmtNum(f.servingGrams) : ''}"></label>
      <span class="label">Oder aus der Packung ausrechnen:</span>
      <div class="grid2"><label class="field"><span>Packung (${unit})</span><input id="s-pack" inputmode="decimal"></label>
      <label class="field"><span>Anzahl Stück</span><input id="s-pieces" inputmode="decimal"></label></div>
      ${f.servingGrams ? '<button type="button" class="link danger" id="s-remove">Eigene Portion entfernen</button>' : ''}
      <div class="row-btns"><button type="button" class="btn ghost" id="s-x">Abbrechen</button><button type="button" class="btn" id="s-ok">Speichern</button></div>`;
    const calc = () => { const p = parseNum($('#s-pack', b).value), n = parseNum($('#s-pieces', b).value);
      if (p != null && n > 0) $('#s-grams', b).value = fmtNum(Math.round(p / n * 10) / 10); };
    $('#s-pack', b).oninput = calc; $('#s-pieces', b).oninput = calc;
    $('#s-x', b).onclick = closeSheet;
    $('#s-remove', b)?.addEventListener('click', () => { patchFood(f, { servingGrams: null, servingLabel: null }); after(); closeSheet(); });
    $('#s-ok', b).onclick = () => { const g = parseNum($('#s-grams', b).value); if (!(g > 0)) return;
      patchFood(f, { servingGrams: g, servingLabel: $('#s-label', b).value.trim() || '1 Stück' }); after(); closeSheet(); };
  };
}
