// ===== Eintrag bearbeiten =====
function openEdit(e0) {
  const e = S.entries.find(x => x.id === e0.id) || e0;
  const f = food(e.foodId);
  const kind = e.unitLabel === PORTION_UNIT ? 'P' : e.grams != null ? 'G' : 'Q';
  const unitW = e.isLiquid ? 'ml' : 'g';
  const units = f && kind === 'G' ? portionsFor(f.name, f.isLiquid, f.servingGrams > 0 ? { label: f.servingLabel || '1 Portion', grams: f.servingGrams } : null)
    .filter(p => !isPlainAmount(p.label)).map(p => ({ label: unitName(p.label), grams: p.grams })) : [];
  // Einheit des Eintrags behalten (auch wenn sie nicht mehr vorgeschlagen wird)
  if (kind === 'G' && e.unitLabel && e.unitCount && !units.some(u => u.label === e.unitLabel)) units.unshift({ label: e.unitLabel, grams: e.grams / e.unitCount });
  const st = { meal: e.meal, date: e.date, unit: kind === 'G' && e.unitLabel ? units.find(u => u.label === e.unitLabel) : null,
    count: fmtNum(e.unitCount ?? 1), grams: e.grams != null ? fmtNum(e.grams) : '', name: e.name, kcal: String(e.kcal), ovOpen: false,
    ov: { kcal: String(e.kcal), protein: fmtNum(e.protein), carbs: fmtNum(e.carbs), fat: fmtNum(e.fat), sugar: e.sugar == null ? '' : fmtNum(e.sugar) } };
  const per = f || (e.grams ? { kcal100: e.kcal * 100 / e.grams, protein100: e.protein * 100 / e.grams, carbs100: e.carbs * 100 / e.grams, fat100: e.fat * 100 / e.grams,
    sugar100: e.sugar == null ? null : e.sugar * 100 / e.grams } : null);
  sheetStack = [];
  openSheet(function draw(b) {
    const cnt = parseNum(st.count);
    const value = kind === 'P' ? cnt : kind === 'G' ? (st.unit ? (cnt == null ? null : cnt * st.unit.grams) : parseNum(st.grams)) : null;
    const preview = kind === 'G' && value != null && per ? amountOf(per, value).kcal : kind === 'P' && value != null && f ? amountOf(f, value * 100).kcal : parseNum(st.kcal);
    b.innerHTML = `<div class="sheet-top"><h2 class="sheet-title">Eintrag bearbeiten</h2>
      <button type="button" class="icon-btn${f?.favorite ? ' fav' : ''}" id="e-fav" aria-label="Favorit">${ICON.heart}</button></div>
      ${kind === 'Q' ? `<label class="field"><span>Bezeichnung</span><input id="e-name" value="${esc(st.name)}"></label>
        <label class="field"><span>Kalorien (kcal)</span><input id="e-kcal" inputmode="decimal" value="${esc(st.kcal)}"></label>`
      : `<p class="ename-big">${esc(e.name)}</p><div class="bigkcal"><b>${preview != null ? fmtInt(preview) : '–'}</b> kcal</div>
        ${kind === 'P' ? `${chips(COUNTS, cnt, 'count')}<label class="field"><span>Anzahl Portionen</span><input id="e-count" inputmode="decimal" value="${esc(st.count)}"></label>`
        : `${units.length ? `<span class="label">Einheit</span><div class="chips"><button type="button" class="chip${!st.unit ? ' on' : ''}" data-unit="">${unitW}</button>
            ${units.map((u, i) => `<button type="button" class="chip${st.unit === u ? ' on' : ''}" data-unit="${i}">${esc(u.label)} (${fmtNum(u.grams)} ${unitW})</button>`).join('')}</div>` : ''}
          ${st.unit ? `${chips(COUNTS, cnt, 'count')}<label class="field"><span>Wie viele × ${esc(st.unit.label)}?</span><input id="e-count" inputmode="decimal" value="${esc(st.count)}"></label>`
          : `<label class="field"><span>Menge in ${unitW}</span><input id="e-grams" inputmode="decimal" value="${esc(st.grams)}"></label>`}`}`}
      <span class="label">Mahlzeit</span>${mealChips(st.meal)}
      <label class="field"><span>Tag</span><input id="e-date" type="date" max="${D.today()}" value="${st.date}"></label>
      <button type="button" class="link" id="e-ov">${st.ovOpen ? 'Eigene Werte ausblenden' : 'Werte nur für diesen Eintrag ändern'}</button>
      ${st.ovOpen ? `<p class="hint small">Nur dieser Eintrag – das Lebensmittel selbst bleibt unverändert.</p><div class="grid2">
        ${[['kcal', 'kcal'], ['protein', 'Eiweiß g'], ['carbs', 'Kohlenh. g'], ['fat', 'Fett g'], ['sugar', 'Zucker g']].map(([k, l]) =>
          `<label class="field"><span>${l}</span><input data-ov="${k}" inputmode="decimal" value="${esc(st.ov[k])}"></label>`).join('')}</div>` : ''}
      <div class="row-btns"><button type="button" class="btn ghost danger" id="e-del">Löschen</button><button type="button" class="btn" id="e-save">Speichern</button></div>`;
    const keep = () => { st.date = $('#e-date', b).value || st.date; const n = $('#e-name', b), k = $('#e-kcal', b), c = $('#e-count', b), g = $('#e-grams', b);
      if (n) st.name = n.value; if (k) st.kcal = k.value; if (c) st.count = c.value; if (g) st.grams = g.value;
      $$('[data-ov]', b).forEach(i => st.ov[i.dataset.ov] = i.value); };
    const re = () => { keep(); redrawSheet(); };
    $$('[data-meal]', b).forEach(c => c.onclick = () => { st.meal = c.dataset.meal; re(); });
    $$('[data-count]', b).forEach(c => c.onclick = () => { keep(); st.count = fmtNum(+c.dataset.count); redrawSheet(); });
    $$('[data-unit]', b).forEach(c => c.onclick = () => { keep(); if (c.dataset.unit === '') { if (value != null) st.grams = fmtNum(value); st.unit = null; } else { st.unit = units[+c.dataset.unit]; st.count = '1'; } redrawSheet(); });
    ['#e-count', '#e-grams', '#e-kcal'].forEach(s => { const el = $(s, b); if (el) el.onchange = re; });
    $('#e-ov', b).onclick = () => { keep(); st.ovOpen = !st.ovOpen; redrawSheet(); };
    $('#e-fav', b).onclick = () => {
      if (f) { patchFood(f, { favorite: !f.favorite }); closeSheet(); openEdit(e); return; }
      const id = newId('u:'); // Schnelleintrag → eigenes Portions-Lebensmittel
      S.own[id] = { id, name: e.name, brand: null, source: 'CUSTOM', kcal100: e.kcal, protein100: e.protein, carbs100: e.carbs, fat100: e.fat, sugar100: e.sugar,
        isLiquid: false, isPortionFood: true, favorite: true, useCount: 1, lastUsedAt: Date.now(), servingGrams: null, servingLabel: null, isAdjusted: false };
      updateEntry(e.id, { foodId: id }); closeSheet(); snack(`${e.name} als Favorit gemerkt`); openEdit(e);
    };
    $('#e-del', b).onclick = () => { closeSheet(); deleteWithUndo(e); };
    $('#e-save', b).onclick = () => {
      keep(); const before = { ...e }; let ch = { meal: st.meal, date: st.date > D.today() ? D.today() : st.date };
      const c2 = parseNum(st.count);
      if (kind === 'Q') { const k = quickCalc(st.kcal); if (k == null) return; ch = { ...ch, name: st.name.trim() || e.name, kcal: k }; }
      else if (kind === 'P') { if (!(c2 > 0)) return; if (f) { const a = amountOf(f, c2 * 100); ch = { ...ch, ...a }; } else { const r = c2 / (e.unitCount || 1);
        ch = { ...ch, kcal: Math.round(e.kcal * r), protein: e.protein * r, carbs: e.carbs * r, fat: e.fat * r, sugar: e.sugar == null ? null : e.sugar * r }; } ch.unitCount = c2; }
      else { const g = st.unit ? (c2 == null ? null : c2 * st.unit.grams) : parseNum(st.grams); if (!(g > 0)) return;
        const a = amountOf(per, g); ch = { ...ch, ...a, grams: g, unitLabel: st.unit ? st.unit.label : null, unitCount: st.unit ? c2 : null }; }
      if (st.ovOpen) { const n = k => parseNum(st.ov[k]); if (n('kcal') != null) ch = { ...ch, kcal: Math.round(n('kcal')), protein: n('protein') ?? 0, carbs: n('carbs') ?? 0, fat: n('fat') ?? 0, sugar: n('sugar') }; }
      updateEntry(e.id, ch); closeAllSheets();
      if (ch.date !== before.date) snack(`Gespeichert für ${dayLong(ch.date)}`, () => updateEntry(e.id, before));
    };
  });
}

// ===== Verlauf =====
let histSel = null;
function renderHistory(root) {
  const s = S.settings; const g = s.historyGrouping; const n = GROUPING[g].counts.includes(s.historyCount) ? s.historyCount : GROUPING[g].def;
  const today = D.today(); const from = firstDay(today, g, n);
  const totals = dayTotals(from, today); const gf = d => goalFor(s, d);
  const buckets = buildBuckets(today, g, n, totals, gf);
  const sum = summarize(today, D.diff(from, today) + 1, totals, gf);
  const isDays = g === 'DAYS'; const set = c => { Object.assign(S.settings, c); save(); histSel = null; render(); };
  const countLabel = { DAYS: 'Tage', WEEKS: 'Wochen', MONTHS: 'Monate' }[g];
  const sel = buckets.find(b => b.start === histSel);
  root.innerHTML = `<h1 class="pagetitle">Verlauf</h1>
    ${chips([['DAYS', 'Tage'], ['WEEKS', 'Wochen'], ['MONTHS', 'Monate']], g, 'grp')}
    ${chips(GROUPING[g].counts.map(c => [c, `${c} ${countLabel}`]), n, 'cnt')}
    <section class="card">
      <div class="kpis"><div><span>Durchschnitt pro Tag</span><b>${sum.average != null ? fmtInt(sum.average) + ' kcal' : '–'}</b></div>
        <div><span>Ziel im Schnitt</span><b>${fmtInt(sum.averageGoal)} kcal</b></div>
        <div><span>Tage mit Einträgen</span><b>${sum.loggedDays} von ${sum.days}</b></div><div><span>Tage über dem Ziel</span><b>${sum.overDays}</b></div></div>
      ${chips([['BARS', 'Balken'], ['LINE', 'Linie'], ['AREA', 'Fläche']], s.chartType, 'chart')}
      <div class="chart" id="chart"></div>
      <div class="legend"><span><i class="dot ok"></i>im Ziel</span><span><i class="dot ov"></i>über dem Ziel</span><span>- - - Ziel</span></div>
      ${sel ? `<div class="selbox"><div><b>${esc(bucketTitle(sel, g))}</b><small>${isDays ? `${fmtInt(sel.kcal ?? 0)} von ${fmtInt(sel.goal)} kcal`
        : `Ø ${fmtInt(sel.kcal ?? 0)} von ${fmtInt(sel.goal)} kcal pro Tag · ${sel.loggedDays} von ${sel.days} Tagen eingetragen`}</small></div>
        ${isDays ? `<button type="button" class="link" data-open="${sel.start}">Tag öffnen</button>` : ''}</div>` : ''}
    </section>
    <section class="card list">${[...buckets].reverse().map(bk => `<button type="button" class="hrow" data-${isDays ? 'open' : 'sel'}="${bk.start}">
      <span class="fname"><b>${esc(bucketTitle(bk, g))}</b><small>${bk.kcal == null ? 'keine Einträge' : !isDays ? `${bk.loggedDays} von ${bk.days} Tagen eingetragen`
        : s.showMacros ? `Eiweiß ${Math.round(bk.protein)} g · Kohlenhydrate ${Math.round(bk.carbs)} g · Fett ${Math.round(bk.fat)} g` : ''}</small></span>
      ${bk.kcal != null ? `<span class="hval"><b class="${bk.kcal > bk.goal ? 'over' : ''}">${isDays ? '' : 'Ø '}${fmtInt(bk.kcal)}</b><small> / ${fmtInt(bk.goal)}</small></span>` : ''}</button>`).join('')}</section>
    <p class="hint small">${isDays ? 'Der Durchschnitt zählt nur Tage mit Einträgen und lässt den heutigen Tag weg, solange er noch läuft.'
      : 'Wochen und Monate zeigen den Durchschnitt pro Tag – gezählt werden nur Tage mit Einträgen. Der heutige Tag zählt erst mit, wenn er vorbei ist.'}</p>`;
  $$('[data-grp]', root).forEach(c => c.onclick = () => set({ historyGrouping: c.dataset.grp, historyCount: GROUPING[c.dataset.grp].def }));
  $$('[data-cnt]', root).forEach(c => c.onclick = () => set({ historyCount: +c.dataset.cnt }));
  $$('[data-chart]', root).forEach(c => c.onclick = () => { S.settings.chartType = c.dataset.chart; save(); render(); });
  $$('[data-open]', root).forEach(c => c.onclick = () => { curDate = c.dataset.open; go('heute'); });
  $$('[data-sel]', root).forEach(c => c.onclick = () => { histSel = c.dataset.sel; render(); });
  drawChart($('#chart', root), buckets, s.chartType, g);
}
function bucketTitle(b, g) {
  if (g === 'DAYS') return dayTitle(b.start);
  const dt = x => { const d = new Date(D.ms(x)); return `${d.getUTCDate()}. ${MONTHS[d.getUTCMonth()].slice(0, 3)}`; };
  if (g === 'WEEKS') return `${dt(b.start)} – ${dt(D.add(b.start, 6))}`;
  const d = new Date(D.ms(b.start)); return `${MONTHS[d.getUTCMonth()]} ${d.getUTCFullYear()}`;
}
function drawChart(el, buckets, type, g) {
  const W = Math.max(280, el.clientWidth || 320), H = 170, LB = 22, n = buckets.length, slot = W / n;
  const max = Math.max(1, ...buckets.map(b => Math.max(b.kcal ?? 0, b.goal))) * 1.1;
  const y = v => (H - LB) * (1 - v / max); const cx = i => i * slot + slot / 2;
  let svg = `<svg viewBox="0 0 ${W} ${H}" width="100%" height="${H}" role="img" aria-label="Kalorien pro ${g === 'DAYS' ? 'Tag' : 'Abschnitt'}">`;
  // Gitter: 0 und Maximum
  svg += `<line x1="0" x2="${W}" y1="${H - LB}" y2="${H - LB}" class="axis"/>`;
  const dim = b => histSel && b.start !== histSel ? ' dim' : '';
  if (type === 'BARS') {
    const bw = slot * (n <= 8 ? 0.55 : 0.7);
    buckets.forEach((b, i) => { const x = i * slot + (slot - bw) / 2;
      svg += b.kcal != null ? `<rect x="${x.toFixed(1)}" y="${y(b.kcal).toFixed(1)}" width="${bw.toFixed(1)}" height="${(H - LB - y(b.kcal)).toFixed(1)}" rx="3" class="bar${b.kcal > b.goal ? ' over' : ''}${dim(b)}"/>`
        : `<rect x="${x.toFixed(1)}" y="${H - LB - 3}" width="${bw.toFixed(1)}" height="3" class="empty"/>`; });
  } else {
    let seg = []; const segs = [];
    buckets.forEach((b, i) => { if (b.kcal == null) { if (seg.length) segs.push(seg); seg = []; } else seg.push([cx(i), y(b.kcal)]); });
    if (seg.length) segs.push(seg);
    for (const sg of segs) { const d = sg.map((p, j) => `${j ? 'L' : 'M'}${p[0].toFixed(1)} ${p[1].toFixed(1)}`).join(' ');
      if (type === 'AREA' && sg.length > 1) svg += `<path d="${d} L${sg[sg.length - 1][0].toFixed(1)} ${H - LB} L${sg[0][0].toFixed(1)} ${H - LB} Z" class="area"/>`;
      svg += `<path d="${d}" class="line"/>`; }
    buckets.forEach((b, i) => { if (b.kcal != null) svg += `<circle cx="${cx(i).toFixed(1)}" cy="${y(b.kcal).toFixed(1)}" r="${b.start === histSel ? 6 : 4.5}" class="pt${b.kcal > b.goal ? ' over' : ''}${dim(b)}"/>`; });
  }
  buckets.forEach((b, i) => { svg += `<line x1="${(i * slot + 1).toFixed(1)}" x2="${((i + 1) * slot - 1).toFixed(1)}" y1="${y(b.goal).toFixed(1)}" y2="${y(b.goal).toFixed(1)}" class="goal"/>`; });
  const step = g === 'DAYS' ? (n <= 7 ? 1 : n <= 14 ? 2 : 5) : g === 'WEEKS' ? (n <= 4 ? 1 : 2) : (n <= 6 ? 1 : 2);
  buckets.forEach((b, i) => { if ((n - 1 - i) % step) return; const d = new Date(D.ms(b.start));
    const t = g === 'DAYS' ? (n <= 7 ? WD_LABEL[D.dow(b.start) - 1].slice(0, 2) : d.getUTCDate()) : g === 'WEEKS' ? `${d.getUTCDate()}.${d.getUTCMonth() + 1}.` : MONTHS[d.getUTCMonth()].slice(0, 3);
    svg += `<text x="${cx(i).toFixed(1)}" y="${H - 6}" text-anchor="middle" class="tick">${t}</text>`; });
  buckets.forEach((b, i) => { if (b.kcal != null) svg += `<rect x="${(i * slot).toFixed(1)}" y="0" width="${slot.toFixed(1)}" height="${H}" class="hit" data-hit="${b.start}"><title>${esc(bucketTitle(b, g))}: ${fmtInt(b.kcal)} kcal</title></rect>`; });
  el.innerHTML = svg + '</svg>';
  $$('[data-hit]', el).forEach(r => r.addEventListener('click', () => { histSel = histSel === r.dataset.hit ? null : r.dataset.hit; render(); }));
}
