// ===== Heute =====
let curDate = D.today();
function renderToday(root) {
  const s = S.settings; const list = dayEntries(curDate); const isToday = curDate === D.today();
  const total = list.reduce((a, e) => ({ kcal: a.kcal + e.kcal, protein: a.protein + e.protein, carbs: a.carbs + e.carbs, fat: a.fat + e.fat,
    sugar: a.sugar + (e.sugar || 0) }), { kcal: 0, protein: 0, carbs: 0, fat: 0, sugar: 0 });
  const goal = goalFor(s, curDate); const rest = goal - total.kcal; const over = rest < 0;
  const prog = goal > 0 ? Math.min(total.kcal / goal, 1) : 0;
  const t = macroTargets(goal, s); const noSugar = list.filter(e => e.sugar == null).length;
  const R = 50, C = 2 * Math.PI * R;
  const bar = (label, v, target, cls, limit) => {
    const p = target > 0 ? Math.min(v / target, 1) : 0; const ov = limit ? v > target : false;
    return `<div class="macro"><div class="mlabel"><span>${label}</span><span class="mval${ov ? ' over' : ''}">${Math.round(v)}<small> / ${target} g</small></span></div>
      <div class="track"><div class="fill ${cls}${ov ? ' over' : ''}" style="width:${(p * 100).toFixed(1)}%"></div></div></div>`;
  };
  const yesterdayMeals = new Set(dayEntries(D.add(curDate, -1)).map(e => e.meal));
  root.innerHTML = `
  <div class="swipe-day" id="swipe-day">
    <header class="dayhead">
      <button type="button" class="icon-btn" id="prev" aria-label="Vorheriger Tag">${ICON.left}</button>
      <div class="daytitle"><h1>${esc(dayTitle(curDate))}</h1>${isToday ? '' : '<button type="button" class="link" id="go-today">Zu heute</button>'}</div>
      <button type="button" class="icon-btn" id="next" aria-label="Nächster Tag" ${isToday ? 'disabled' : ''}>${ICON.right}</button>
    </header>
    <section class="card summary">
      <div class="ringrow">
        <div class="ring"><svg viewBox="0 0 120 120" aria-hidden="true"><circle cx="60" cy="60" r="${R}" class="ring-bg"/>
          <circle cx="60" cy="60" r="${R}" class="ring-fg${over ? ' over' : ''}" stroke-dasharray="${(C * prog).toFixed(1)} ${C.toFixed(1)}" transform="rotate(-90 60 60)"/></svg>
          <div class="ringtext"><b class="${over ? 'over' : ''}">${fmtInt(Math.abs(rest))}</b><span>${over ? 'KCAL DRÜBER' : 'KCAL ÜBRIG'}</span></div></div>
        <dl class="stats"><div><dt>Ziel</dt><dd>${fmtInt(goal)} kcal</dd></div><div><dt>Gegessen</dt><dd>${fmtInt(total.kcal)} kcal</dd></div>
          <div><dt>Einträge</dt><dd>${list.length}</dd></div></dl>
      </div>
      ${s.showMacros ? `<div class="macros${s.showSugar ? ' four' : ''}">${bar('Eiweiß', total.protein, t.protein, 'p')}${bar('Kohlenh.', total.carbs, t.carbs, 'c')}${bar('Fett', total.fat, t.fat, 'f')}
        ${s.showSugar ? bar('Zucker', total.sugar, t.sugar, 's', true) : ''}</div>
        ${s.showSugar && noSugar && list.length ? `<p class="hint">${noSugar} ${noSugar === 1 ? 'Eintrag' : 'Einträge'} ohne Zuckerangabe</p>` : ''}` : ''}
    </section>
  </div>
  ${MEALS.map(m => mealCard(m, list.filter(e => e.meal === m), yesterdayMeals.has(m))).join('')}
  ${!S.entries.length ? `<section class="card empty-start"><h2>Willkommen</h2><p>Das ist der Kalorienzähler im Browser – mit denselben Funktionen und 7.140 Lebensmitteln wie die App.
    Tippe auf „Eintragen“ oder bei einer Mahlzeit auf ＋. Deine Handy-Daten holst du unter Einstellungen → Sicherung herein.</p></section>` : ''}
  <button type="button" class="fab" id="fab">${ICON.plus}<span>Eintragen</span></button>`;

  $('#prev').onclick = () => { curDate = D.add(curDate, -1); render(); };
  $('#next').onclick = () => { if (curDate < D.today()) { curDate = D.add(curDate, 1); render(); } };
  $('#go-today')?.addEventListener('click', () => { curDate = D.today(); render(); });
  $('#fab').onclick = () => openAdd(mealForTime(new Date().getHours()));
  daySwipe($('#swipe-day'));
  $$('[data-add]', root).forEach(b => b.onclick = () => openAdd(b.dataset.add));
  $$('[data-yesterday]', root).forEach(b => b.onclick = () => {
    const m = b.dataset.yesterday; let now = Date.now();
    const copies = dayEntries(D.add(curDate, -1)).filter(e => e.meal === m).map(e => ({ ...e, id: newId('e'), date: curDate, createdAt: now++ }));
    restoreEntries(copies); copies.forEach(c => c.foodId && markUsed(c.foodId));
    snack(`${copies.length} ${copies.length === 1 ? 'Eintrag' : 'Einträge'} von gestern übernommen`, () => removeEntries(copies.map(c => c.id))); render();
  });
  $$('[data-mealmenu]', root).forEach(b => b.onclick = () => openMealMenu(b.dataset.mealmenu));
  $$('.entry', root).forEach(row => entryGestures(row, S.entries.find(e => e.id === row.dataset.id)));
}
function mealCard(m, list, canYesterday) {
  const sum = list.reduce((a, e) => a + e.kcal, 0);
  return `<section class="card meal">
    <div class="mealhead"><h2>${MEAL_LABEL[m]}</h2>${sum ? `<span class="muted">${fmtInt(sum)} kcal</span>
      <button type="button" class="icon-btn small" data-mealmenu="${m}" aria-label="Mehr">${ICON.more}</button>` : ''}
      <button type="button" class="add-btn" data-add="${m}" aria-label="Zu ${MEAL_LABEL[m]} hinzufügen">${ICON.plus}</button></div>
    ${!list.length ? `<div class="mealempty"><span>Noch nichts eingetragen</span>${canYesterday ? `<button type="button" class="link" data-yesterday="${m}">Wie gestern</button>` : ''}</div>` : ''}
    ${list.map(e => { const sub = [entryAmount(e), e.brand].filter(Boolean).join(' · ');
      return `<div class="entry-wrap"><div class="entry-del">Löschen</div><div class="entry" data-id="${e.id}" tabindex="0" role="button">
        <div class="ename"><b>${esc(e.name)}</b>${sub ? `<small>${esc(sub)}</small>` : ''}</div><span class="ekcal">${fmtInt(e.kcal)}</span></div></div>`; }).join('')}
  </section>`;
}
function deleteWithUndo(e) { removeEntries([e.id]); snack(`${e.name} gelöscht`, () => restoreEntries([e])); render(); }
function daySwipe(el) {
  let x0, y0;
  el.addEventListener('touchstart', ev => { x0 = ev.touches[0].clientX; y0 = ev.touches[0].clientY; }, { passive: true });
  el.addEventListener('touchend', ev => { if (x0 == null) return; const dx = ev.changedTouches[0].clientX - x0, dy = ev.changedTouches[0].clientY - y0;
    x0 = null; if (Math.abs(dx) > 70 && Math.abs(dy) < 50) { if (dx > 0) curDate = D.add(curDate, -1); else if (curDate < D.today()) curDate = D.add(curDate, 1); render(); } });
}
// Eintrag: Tippen = bearbeiten, lange drücken = Menü, nach links wischen = löschen
function entryGestures(row, e) {
  let x0, y0, dx = 0, long, moved = false, pressed = false;
  row.addEventListener('pointerdown', ev => { pressed = true; x0 = ev.clientX; y0 = ev.clientY; dx = 0; moved = false;
    long = setTimeout(() => { if (!moved) { pressed = false; openEntryMenu(e); } }, 550); });
  row.addEventListener('pointermove', ev => { if (!pressed) return; const ddx = ev.clientX - x0, ddy = ev.clientY - y0;
    if (Math.abs(ddx) > 8 || Math.abs(ddy) > 8) { moved = true; clearTimeout(long); }
    if (ev.pointerType !== 'mouse' && Math.abs(ddx) > Math.abs(ddy) && ddx < 0) { dx = ddx; row.style.transform = `translateX(${dx}px)`; } });
  const end = () => { clearTimeout(long); if (!pressed) return; pressed = false;
    if (dx < -row.offsetWidth * 0.35) { row.style.transform = `translateX(-110%)`; setTimeout(() => deleteWithUndo(e), 150); return; }
    row.style.transform = ''; if (!moved) openEdit(e); };
  row.addEventListener('pointerup', end); row.addEventListener('pointercancel', () => { clearTimeout(long); pressed = false; row.style.transform = ''; });
  row.addEventListener('contextmenu', ev => { ev.preventDefault(); clearTimeout(long); pressed = false; openEntryMenu(e); });
  row.addEventListener('keydown', ev => { if (ev.key === 'Enter') openEdit(e); if (ev.key === 'Delete') deleteWithUndo(e); });
}
function openEntryMenu(e) {
  openSheet(b => { b.innerHTML = `<h2 class="sheet-title">${esc(e.name)}</h2><div class="menu">
    <button type="button" id="m-again">Heute nochmal</button><button type="button" id="m-edit">Bearbeiten / verschieben</button>
    <button type="button" id="m-del" class="danger">Löschen</button><button type="button" id="m-x">Abbrechen</button></div>`;
    $('#m-again', b).onclick = () => { const c = { ...e, id: newId('e'), date: D.today(), createdAt: Date.now() }; restoreEntries([c]); if (c.foodId) markUsed(c.foodId);
      closeSheet(); snack(`${e.name} heute nochmal eingetragen`, () => removeEntries([c.id])); };
    $('#m-edit', b).onclick = () => { closeSheet(); openEdit(e); };
    $('#m-del', b).onclick = () => { closeSheet(); deleteWithUndo(e); };
    $('#m-x', b).onclick = closeSheet; });
}
function openMealMenu(m) {
  openSheet(b => { const sug = `${MEAL_LABEL[m]}-Vorlage`;
    b.innerHTML = `<h2 class="sheet-title">Als Vorlage speichern</h2><p class="hint">Alle Einträge dieser Mahlzeit werden gespeichert. Beim Eintragen findest du die Vorlage oben unter „Vorlagen“.</p>
      <label class="field"><span>Name der Vorlage</span><input id="tpl-name" maxlength="40" value="${esc(sug)}"></label>
      <div class="row-btns"><button type="button" class="btn ghost" id="tpl-x">Abbrechen</button><button type="button" class="btn" id="tpl-ok">Speichern</button></div>`;
    $('#tpl-x', b).onclick = closeSheet;
    $('#tpl-ok', b).onclick = () => { const name = $('#tpl-name', b).value.trim() || sug; const src = dayEntries(curDate).filter(e => e.meal === m);
      if (!src.length) { closeSheet(); snack('Die Mahlzeit ist leer – nichts gespeichert'); return; }
      S.templates.push({ id: newId('t'), name, meal: m, createdAt: Date.now(), useCount: 0,
        items: src.map(({ id, date, meal, createdAt, ...i }) => i) }); save(); closeSheet(); snack(`Vorlage „${name}“ gespeichert`); };
  });
}
