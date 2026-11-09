// ===== Daten: eingebaute BLS-Liste + eigene Daten im Browser (localStorage) =====
const STORE_KEY = 'kalorienzaehler-web';
const BLS = new Map(); // code → Lebensmittel
(function loadBls() {
  for (const line of BLS_TSV.split('\n')) {
    const p = line.split('\t'); if (p.length < 7 || p[0] === 'code') continue;
    const num = i => Number((p[i] || '').replace(',', '.')) || 0;
    BLS.set(p[0], { id: 'b:' + p[0], code: p[0], name: p[1].trim(), source: 'BLS', sourceId: p[0], kcal100: num(2), protein100: num(3),
      carbs100: num(4), fat100: num(5), isLiquid: p[6].trim() === '1', sugar100: p.length >= 8 && p[7] !== '' ? num(7) : null,
      norm: normalize(p[1]) });
  }
})();

const DEFAULT_SETTINGS = { dailyGoal: 2200, showMacros: true, showSugar: true, proteinGoal: null, carbsGoal: null, fatGoal: null,
  sugarLimit: null, weekdayGoals: {}, themeMode: 'SYSTEM', startTab: 0, palette: 'DEFAULT', historyGrouping: 'DAYS', historyCount: 7,
  chartType: 'BARS' };

let S = { settings: { ...DEFAULT_SETTINGS }, own: {}, blsOverlay: {}, entries: [], templates: [], seq: 1 };
let storageOk = true;
try { const raw = localStorage.getItem(STORE_KEY); if (raw) { const o = JSON.parse(raw); S = { ...S, ...o, settings: { ...DEFAULT_SETTINGS, ...o.settings } }; } }
catch (e) { storageOk = false; }
function save() { try { localStorage.setItem(STORE_KEY, JSON.stringify(S)); storageOk = true; } catch (e) { storageOk = false; } }
const newId = p => p + (S.seq++);

/** Lebensmittel nach id: „b:CODE“ (BLS, mit eigenen Änderungen überlagert) oder „u:N“ (eigen/gescannt). */
function food(id) {
  if (!id) return null;
  if (id.startsWith('b:')) { const b = BLS.get(id.slice(2)); if (!b) return null; const o = S.blsOverlay[b.code];
    return o ? { ...b, ...o, id: b.id, code: b.code, norm: b.norm } : { ...b, favorite: false, useCount: 0 }; }
  return S.own[id] || null;
}
function patchFood(f, changes) {
  if (f.id.startsWith('b:')) { S.blsOverlay[f.code] = { ...(S.blsOverlay[f.code] || {}), ...changes }; }
  else S.own[f.id] = { ...S.own[f.id], ...changes };
  save(); return food(f.id);
}
function markUsed(id) { const f = food(id); if (f) patchFood(f, { lastUsedAt: Date.now(), useCount: (f.useCount || 0) + 1 }); }
function per100Label(f) { return f.isPortionFood ? `${Math.round(f.kcal100)} kcal/Portion` : `${Math.round(f.kcal100)} kcal/100 ${f.isLiquid ? 'ml' : 'g'}`; }
function sourceLabel(f) { return f.source === 'BLS' ? 'BLS' : f.source === 'OFF' ? 'Open Food Facts' : f.source === 'DISH' ? 'Gericht' : f.source === 'MENSA' ? 'Mensa' : 'Eigenes'; }
function allFoods() {
  const own = Object.values(S.own).map(f => ({ ...f, norm: normalize([f.name, f.brand].filter(Boolean).join(' ')) }));
  const ov = Object.keys(S.blsOverlay);
  return { own, bls: [...BLS.values()], overlayCodes: ov };
}
function searchFoods(q) {
  const { own } = allFoods();
  const cands = [];
  for (const f of own) cands.push({ id: f.id, norm: f.norm, favorite: !!f.favorite, useCount: f.useCount || 0, isOwn: f.source === 'CUSTOM' });
  for (const b of BLS.values()) { const o = S.blsOverlay[b.code];
    cands.push({ id: b.id, norm: b.norm, favorite: !!o?.favorite, useCount: o?.useCount || 0, isOwn: false }); }
  return rankSearch(q, cands).map(c => food(c.id));
}
function favorites() {
  const list = Object.values(S.own).filter(f => f.favorite);
  for (const [code, o] of Object.entries(S.blsOverlay)) if (o.favorite) list.push(food('b:' + code));
  return list.filter(Boolean).sort((a, b) => a.name.localeCompare(b.name, 'de'));
}
function recent() {
  const list = Object.values(S.own).filter(f => f.lastUsedAt);
  for (const [code, o] of Object.entries(S.blsOverlay)) if (o.lastUsedAt) list.push(food('b:' + code));
  return list.filter(Boolean).sort((a, b) => b.lastUsedAt - a.lastUsedAt).slice(0, 12);
}

// ---- Einträge
const dayEntries = d => S.entries.filter(e => e.date === d).sort((a, b) => a.createdAt - b.createdAt);
function addEntry(e) { const x = { id: newId('e'), createdAt: Date.now(), ...e }; S.entries.push(x); if (x.foodId) markUsed(x.foodId); save(); return x; }
function removeEntries(ids) { const set = new Set(ids); S.entries = S.entries.filter(e => !set.has(e.id)); save(); }
function restoreEntries(list) { S.entries.push(...list); save(); }
function updateEntry(id, changes) { const i = S.entries.findIndex(e => e.id === id); if (i >= 0) S.entries[i] = { ...S.entries[i], ...changes }; save(); return S.entries[i]; }
function entryFromFood(f, grams, meal, date, unitLabel, unitCount) {
  const a = amountOf(f, grams);
  return { date, meal, foodId: f.id, name: f.name, brand: f.brand || null, grams, isLiquid: !!f.isLiquid, kcal: a.kcal, protein: a.protein,
    carbs: a.carbs, fat: a.fat, sugar: a.sugar, unitLabel: unitLabel || null, unitCount: unitCount ?? null };
}
function entryPortions(f, count, meal, date) {
  const a = amountOf(f, count * 100);
  return { date, meal, foodId: f.id, name: f.name, brand: f.brand || null, grams: null, isLiquid: !!f.isLiquid, kcal: a.kcal,
    protein: a.protein, carbs: a.carbs, fat: a.fat, sugar: a.sugar, unitLabel: PORTION_UNIT, unitCount: count };
}
function recalcForFood(f, date) {
  for (const e of S.entries.filter(e => e.foodId === f.id && e.date === date)) {
    const a = (f.isPortionFood || e.grams == null) ? amountOf(f, (e.unitCount ?? 1) * 100) : amountOf(f, e.grams);
    Object.assign(e, { kcal: a.kcal, protein: a.protein, carbs: a.carbs, fat: a.fat, sugar: a.sugar });
  }
  save();
}
function dayTotals(from, to) {
  const m = new Map();
  for (const e of S.entries) { if (e.date < from || e.date > to) continue;
    const t = m.get(e.date) || { date: e.date, kcal: 0, protein: 0, carbs: 0, fat: 0, entries: 0 };
    t.kcal += e.kcal; t.protein += e.protein; t.carbs += e.carbs; t.fat += e.fat; t.entries++; m.set(e.date, t); }
  return [...m.values()];
}
function correctFood(f, per, name) {
  if (f.source === 'CUSTOM') return patchFood(f, { name: name.trim() || f.name, kcal100: per.kcal, protein100: per.protein, carbs100: per.carbs, fat100: per.fat, sugar100: per.sugar });
  const orig = f.isAdjusted ? {} : { origKcal100: f.kcal100, origProtein100: f.protein100, origCarbs100: f.carbs100, origFat100: f.fat100, origSugar100: f.sugar100 };
  return patchFood(f, { ...orig, isAdjusted: true, kcal100: per.kcal, protein100: per.protein, carbs100: per.carbs, fat100: per.fat, sugar100: per.sugar });
}
function resetFood(f) {
  if (!f.isAdjusted) return f;
  if (f.id.startsWith('b:')) { const o = { ...S.blsOverlay[f.code] };
    for (const k of ['kcal100', 'protein100', 'carbs100', 'fat100', 'sugar100', 'isAdjusted', 'origKcal100', 'origProtein100', 'origCarbs100', 'origFat100', 'origSugar100']) delete o[k];
    S.blsOverlay[f.code] = o; save(); return food(f.id); }
  return patchFood(f, { kcal100: f.origKcal100 ?? f.kcal100, protein100: f.origProtein100 ?? f.protein100, carbs100: f.origCarbs100 ?? f.carbs100,
    fat100: f.origFat100 ?? f.fat100, sugar100: f.origSugar100 ?? f.sugar100, isAdjusted: false, origKcal100: null, origProtein100: null,
    origCarbs100: null, origFat100: null, origSugar100: null });
}
function createCustom({ name, brand, basis, per, servingGrams, favorite, ean }) {
  const id = newId('u:');
  S.own[id] = { id, name: name.trim(), brand: brand?.trim() || null, source: 'CUSTOM', ean: ean || null, kcal100: per.kcal, protein100: per.protein,
    carbs100: per.carbs, fat100: per.fat, sugar100: per.sugar, isLiquid: basis === 'ML', isPortionFood: basis === 'PORTION', favorite: !!favorite,
    useCount: 0, lastUsedAt: null, servingGrams: servingGrams || null, servingLabel: null, isAdjusted: false };
  save(); return S.own[id];
}
function deleteOwn(f) {
  if (f.id.startsWith('b:')) return false;
  for (const e of S.entries) if (e.foodId === f.id) e.foodId = null;
  for (const t of S.templates) for (const i of t.items) if (i.foodId === f.id) i.foodId = null;
  delete S.own[f.id]; save(); return true;
}

// ---- Sicherung im Format der Handy-App (Format 2) – in beide Richtungen austauschbar
function exportBackup() {
  const ids = new Map(); let n = 1;
  const foods = [];
  const add = f => { if (!f || ids.has(f.id)) return; const num = n++; ids.set(f.id, num);
    const { norm, code, id, ...rest } = f; foods.push({ ...rest, id: num, source: f.source, sourceId: f.source === 'BLS' ? f.code : f.sourceId || null }); };
  Object.values(S.own).forEach(add);
  Object.keys(S.blsOverlay).forEach(c => add(food('b:' + c)));
  S.entries.forEach(e => e.foodId && add(food(e.foodId)));
  S.templates.forEach(t => t.items.forEach(i => i.foodId && add(food(i.foodId))));
  const s = S.settings;
  const strip = ({ id, ...r }) => r;
  return JSON.stringify({ format: 2, app: 'Kalorienzaehler', appVersion: 'web-0.7', createdAt: Date.now(),
    settings: { dailyGoal: s.dailyGoal, showMacros: s.showMacros, showSugar: s.showSugar, proteinGoal: s.proteinGoal, carbsGoal: s.carbsGoal,
      fatGoal: s.fatGoal, sugarLimit: s.sugarLimit, weekdayGoals: formatWeekGoals(s.weekdayGoals), themeMode: s.themeMode, startTab: s.startTab, palette: s.palette },
    foods, entries: S.entries.map(e => ({ ...strip(e), foodId: e.foodId ? ids.get(e.foodId) ?? null : null })),
    templates: S.templates.map(t => ({ name: t.name, meal: t.meal, createdAt: t.createdAt, useCount: t.useCount || 0,
      items: t.items.map((i, k) => ({ ...i, position: k, foodId: i.foodId ? ids.get(i.foodId) ?? null : null })) })) });
}
function importBackup(json) {
  const o = JSON.parse(json);
  if (o.app !== 'Kalorienzaehler') throw new Error('Das ist keine Sicherung des Kalorienzählers.');
  // Format 3 (App 0.8): Gerichte/Mensa-Plätze kennt die Browser-Version noch nicht – Gerichte kommen als eigene Lebensmittel mit
  if (!(o.format >= 1 && o.format <= 3)) throw new Error('Die Sicherung ist von einer neueren App-Version.');
  const st = o.settings || {};
  const map = new Map(); const own = {}; const overlay = {};
  let seq = 1;
  for (const f of o.foods || []) {
    const base = { name: f.name, brand: f.brand ?? null, source: f.source, sourceId: f.sourceId ?? null, ean: f.ean ?? null, kcal100: f.kcal100 ?? 0,
      protein100: f.protein100 ?? 0, carbs100: f.carbs100 ?? 0, fat100: f.fat100 ?? 0, sugar100: f.sugar100 ?? null, isLiquid: !!f.isLiquid,
      favorite: !!f.favorite, lastUsedAt: f.lastUsedAt ?? null, useCount: f.useCount || 0, isPortionFood: !!f.isPortionFood,
      servingGrams: f.servingGrams ?? null, servingLabel: f.servingLabel ?? null, isAdjusted: !!f.isAdjusted, origKcal100: f.origKcal100 ?? null,
      origProtein100: f.origProtein100 ?? null, origCarbs100: f.origCarbs100 ?? null, origFat100: f.origFat100 ?? null, origSugar100: f.origSugar100 ?? null };
    if (f.source === 'BLS' && f.sourceId && BLS.has(f.sourceId)) {
      const ov = { favorite: base.favorite, useCount: base.useCount, lastUsedAt: base.lastUsedAt, servingGrams: base.servingGrams, servingLabel: base.servingLabel };
      if (base.isAdjusted) Object.assign(ov, { isAdjusted: true, kcal100: base.kcal100, protein100: base.protein100, carbs100: base.carbs100, fat100: base.fat100,
        sugar100: base.sugar100, origKcal100: base.origKcal100, origProtein100: base.origProtein100, origCarbs100: base.origCarbs100, origFat100: base.origFat100, origSugar100: base.origSugar100 });
      overlay[f.sourceId] = ov; map.set(f.id, 'b:' + f.sourceId);
    } else { const id = 'u:' + (seq++); own[id] = { ...base, id }; map.set(f.id, id); }
  }
  const entries = (o.entries || []).map(e => ({ id: 'e' + (seq++), date: e.date, meal: MEALS.includes(e.meal) ? e.meal : 'SNACK',
    foodId: e.foodId != null ? map.get(e.foodId) ?? null : null, name: e.name, brand: e.brand ?? null, grams: e.grams ?? null, isLiquid: !!e.isLiquid,
    kcal: e.kcal || 0, protein: e.protein || 0, carbs: e.carbs || 0, fat: e.fat || 0, sugar: e.sugar ?? null, createdAt: e.createdAt || 0,
    unitLabel: e.unitLabel ?? null, unitCount: e.unitCount ?? null }));
  const templates = (o.templates || []).map(t => ({ id: 't' + (seq++), name: t.name, meal: t.meal, createdAt: t.createdAt || 0, useCount: t.useCount || 0,
    items: [...(t.items || [])].sort((a, b) => (a.position || 0) - (b.position || 0)).map(i => ({ ...i, foodId: i.foodId != null ? map.get(i.foodId) ?? null : null })) }));
  const settings = { ...S.settings, dailyGoal: st.dailyGoal ?? 2200, showMacros: st.showMacros ?? true, showSugar: st.showSugar ?? true,
    proteinGoal: st.proteinGoal ?? null, carbsGoal: st.carbsGoal ?? null, fatGoal: st.fatGoal ?? null, sugarLimit: st.sugarLimit ?? null,
    weekdayGoals: parseWeekGoals(st.weekdayGoals), themeMode: st.themeMode || S.settings.themeMode, startTab: st.startTab ?? 0, palette: st.palette || S.settings.palette };
  // ältere Sicherungen ohne Vorlagen behalten die vorhandenen
  S = { ...S, settings, own, blsOverlay: overlay, entries, templates: o.templates?.length ? templates : S.templates, seq: seq + 1 };
  save(); return entries.length;
}
function exportCsv() {
  const f1 = v => v == null ? '' : v.toFixed(1).replace('.', ',');
  const rows = ['Datum;Mahlzeit;Lebensmittel;Menge;Einheit;kcal;Eiweiß (g);Kohlenhydrate (g);Fett (g);Zucker (g)'];
  for (const e of [...S.entries].sort((a, b) => a.date.localeCompare(b.date) || a.createdAt - b.createdAt)) {
    const amount = e.unitLabel && e.unitCount != null ? `${f1(e.unitCount)};${e.unitLabel}` : e.grams != null ? `${f1(e.grams)};${e.isLiquid ? 'ml' : 'g'}` : ';';
    rows.push(`${e.date};${MEAL_LABEL[e.meal]};${e.name.replace(/;/g, ',')};${amount};${e.kcal};${f1(e.protein)};${f1(e.carbs)};${f1(e.fat)};${f1(e.sugar)}`);
  }
  return rows.join('\n');
}
