// ===== Rechenlogik – 1:1 aus der App (core/domain) übernommen =====
const MEALS = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
const MEAL_LABEL = { BREAKFAST: 'Frühstück', LUNCH: 'Mittagessen', DINNER: 'Abendessen', SNACK: 'Snacks' };
const PORTION_UNIT = 'Portion';

function mealForTime(h) { return h >= 4 && h <= 10 ? 'BREAKFAST' : h >= 11 && h <= 14 ? 'LUNCH' : h >= 17 && h <= 20 ? 'DINNER' : 'SNACK'; }

// Zahlen wie in der App: „12,5“, „2.200“
function fmtNum(v) { const r = Math.round(v * 10) / 10; return Number.isInteger(r) ? String(r) : String(r).replace('.', ','); }
function fmtInt(v) { return Math.round(v).toLocaleString('de-DE'); }
function parseNum(t) {
  const s = String(t ?? '').trim().replace(/\s/g, '').replace(',', '.');
  if (!s) return null; const n = Number(s); return Number.isFinite(n) && n >= 0 ? n : null;
}

function normalize(t) {
  return String(t).toLowerCase().replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue').replace(/ß/g, 'ss')
    .replace(/[éè]/g, 'e').replace(/à/g, 'a').replace(/ç/g, 'c').replace(/[^a-z0-9]+/g, ' ').trim();
}
function tokens(t) { return normalize(t).split(' ').filter(Boolean); }

function amountOf(food, grams) {
  const f = grams / 100;
  return { kcal: Math.round(food.kcal100 * f), protein: food.protein100 * f, carbs: food.carbs100 * f, fat: food.fat100 * f,
    sugar: food.sugar100 == null ? null : food.sugar100 * f };
}

function macroTargets(goal, s) {
  return {
    protein: s.proteinGoal ?? Math.round(goal * 0.2 / 4), carbs: s.carbsGoal ?? Math.round(goal * 0.5 / 4),
    fat: s.fatGoal ?? Math.round(goal * 0.3 / 9), sugar: s.sugarLimit ?? Math.round(goal * 0.1 / 4),
  };
}

// ---- Suche (SearchRanker)
const SYN = { nudel: ['teigwaren'], nudeln: ['teigwaren'], pasta: ['teigwaren'], spaghetti: ['teigwaren'], penne: ['teigwaren'],
  makkaroni: ['teigwaren'], ei: ['huehnerei'], eier: ['huehnerei'], spiegelei: ['huehnerei gebraten'], quark: ['speisequark'],
  magerquark: ['speisequark magerstufe'], milch: ['vollmilch'], pommes: ['pommes frites'], kaffee: ['kaffee getraenk'], toast: ['toastbrot'] };
const BASE_WORDS = new Set(['roh', 'frisch', 'gekocht', 'pasteurisiert', 'natur']);
const SIDE_WORDS = ['dip', 'dressing', 'sauce', 'creme', 'kuchen', 'torte', 'plunder', 'riegel', 'kekse', 'plaetzchen', 'auflauf', 'suppe'];

function rankSearch(query, cands, limit = 60) {
  const tk = tokens(query); if (!tk.length) return [];
  const out = [];
  for (const c of cands) {
    const compact = c.norm.replace(/ /g, '');
    const mapped = [];
    let ok = true;
    for (const t of tk) {
      const alt = [...(SYN[t] || []), t].find(a => a.split(' ').every(p => compact.includes(p)));
      if (!alt) { ok = false; break; } mapped.push(alt);
    }
    if (!ok) continue;
    out.push([c, score(tk, mapped, c, compact)]);
  }
  out.sort((a, b) => b[1] - a[1] || a[0].norm.length - b[0].norm.length);
  return out.slice(0, limit).map(x => x[0]);
}
function score(tk, mapped, c, compact) {
  const words = c.norm.split(' ').filter(Boolean); if (!words.length) return -1e9;
  let s = 0;
  if (c.favorite) s += 1000; if (c.isOwn) s += 300; s += Math.min(c.useCount * 20, 600);
  if (compact === tk.join('') || compact === mapped.join('').replace(/ /g, '')) s += 600;
  const firstAlt = mapped[0].split(' ')[0];
  if (words[0] === firstAlt && mapped[0] !== tk[0]) s += 320;
  else if (words[0] === tk[0] || words[0] === firstAlt) s += 220;
  else if (compact.startsWith(firstAlt) || compact.startsWith(tk[0])) s += 90;
  for (const m of mapped) for (const p of m.split(' ')) s += words.includes(p) ? 60 : words.some(w => w.startsWith(p)) ? 30 : 0;
  if (words.length <= 4 && words.some(w => BASE_WORDS.has(w))) s += 50;
  if (words.includes('mit') && !tk.includes('mit')) s -= 80;
  const isSide = words.some(w => SIDE_WORDS.some(x => w.includes(x)));
  const wantsSide = tk.some(t => SIDE_WORDS.some(x => t.includes(x)));
  if (isSide && !wantsSide) s -= 60;
  return s - words.length * 12;
}

// ---- Portionsvorschläge (PortionGuesser, Stand 0.7: Hauptwort, Wortende zählt)
const P = (label, grams) => ({ label, grams });
const RULES = [
  [['brot', 'toast'], [P('1 Scheibe', 50)]], [['broetchen', 'semmel', 'croissant'], [P('1 Stück', 60)]],
  [['ei ', 'huehnerei', 'eier'], [P('1 Stück (M)', 58)]], [['banane'], [P('1 Stück', 120)]], [['apfel'], [P('1 Stück', 180)]],
  [['birne', 'orange', 'pfirsich'], [P('1 Stück', 160)]], [['kiwi', 'mandarine', 'clementine'], [P('1 Stück', 75)]],
  [['tomate'], [P('1 Stück', 80)]], [['kartoffel'], [P('1 mittelgroße', 90)]],
  [['butter', 'margarine'], [P('1 TL', 5), P('dünn aufs Brot', 8)]], [['oel', 'olivenoel', 'rapsoel'], [P('1 TL', 5), P('1 EL', 10)]],
  [['kaese', 'gouda', 'emmentaler'], [P('1 Scheibe', 25)]], [['schinken', 'salami', 'wurst', 'aufschnitt'], [P('1 Scheibe', 20)]],
  [['joghurt', 'quark', 'skyr'], [P('1 Becher', 150), P('2 EL', 60)]], [['haferflocken', 'muesli', 'cornflakes'], [P('1 EL', 10), P('1 Portion', 50)]],
  [['nudel', 'spaghetti', 'pasta', 'teigwaren', 'reis'], [P('1 Portion gekocht', 200)]],
  [['zucker', 'honig', 'marmelade', 'konfituere'], [P('1 TL', 7), P('1 EL', 20)]], [['nuss', 'nuesse', 'mandel'], [P('1 Handvoll', 25)]],
  [['schokolade'], [P('1 Riegel', 25), P('1 Tafel', 100)]], [['pizza'], [P('1 Pizza', 350), P('½ Pizza', 175)]],
  [['doener', 'kebab', 'duerueum'], [P('1 Stück', 350)]], [['brustfilet', 'schnitzel', 'steak'], [P('1 Stück', 150)]],
  [['lachs', 'fischfilet', 'seelachs'], [P('1 Filet', 125)]], [['pommes'], [P('kleine Portion', 120), P('große Portion', 200)]],
  [['bratwurst', 'currywurst', 'wiener', 'wuerstchen'], [P('1 Stück', 100)]],
];
const ENDINGS = ['', 'n', 'en', 'e', 's', 'es', 'er'];
function kwMatch(head, k) { return k.endsWith(' ') ? head === k.trim() : ENDINGS.some(e => head.endsWith(k + e)); }
function portionsFor(name, isLiquid, serving) {
  const w = tokens(name);
  const rulesFor = head => RULES.filter(([ks]) => ks.some(k => kwMatch(head, k))).flatMap(([, ps]) => ps);
  let found = [];
  if (!isLiquid && w.length) { found = rulesFor(w[0]); if (!found.length && w.length > 1) found = rulesFor(w[0] + w[1]); }
  const unit = isLiquid ? 'ml' : 'g';
  const hundred = P('100 ' + unit, 100);
  const list = isLiquid ? [P('1 Tasse', 200), P('1 Glas', 250), P('0,5 l', 500), hundred]
    : !found.length ? [P('50 g', 50), hundred, P('150 g', 150), P('200 g', 200)] : [...found, hundred];
  const all = [...(serving ? [serving] : []), ...list];
  const seen = new Set(); return all.filter(p => !seen.has(p.grams) && seen.add(p.grams)).slice(0, 5);
}
const unitName = l => l.replace(/^1 /, '').trim();
const isPlainAmount = l => /^\d+ (g|ml)$/.test(l);
function describeUnit(count, unit, grams, liquid) { return `${fmtNum(count)} × ${unit} (${Math.round(grams)} ${liquid ? 'ml' : 'g'})`; }

// ---- Schnelleintrag rechnen (QuickCalc)
function quickCalc(input) {
  let s = String(input).trim().toLowerCase().replace(/ /g, '').replace(/,/g, '.');
  if (!s) return null;
  const kj = s.endsWith('kj'); s = s.replace(/kj$/, '').replace(/kcal$/, '');
  if (!s || !/^[0-9+\-*×x.]+$/.test(s)) return null;
  s = s.replace(/[×x]/g, '*');
  let total = 0, sign = 1, term = '';
  const flush = () => { if (!term) return false; const f = term.split('*').map(Number); if (f.some(n => !Number.isFinite(n))) return false;
    total += sign * f.reduce((a, b) => a * b, 1); term = ''; return true; };
  for (const ch of s) { if (ch === '+' || ch === '-') { if (!flush()) return null; sign = ch === '+' ? 1 : -1; } else term += ch; }
  if (!flush()) return null;
  const k = Math.round(kj ? total / 4.184 : total); return k >= 0 && k <= 20000 ? k : null;
}
const isCalculation = i => /kj/i.test(i) || /[+\-*×x]/.test(String(i).trim().slice(1));

// ---- Datum (immer „YYYY-MM-DD“, ohne Zeitzonen-Fallen)
const D = {
  today() { const n = new Date(); return D.fmt(Date.UTC(n.getFullYear(), n.getMonth(), n.getDate())); },
  fmt(ms) { return new Date(ms).toISOString().slice(0, 10); },
  ms(d) { const [y, m, dd] = d.split('-').map(Number); return Date.UTC(y, m - 1, dd); },
  add(d, n) { return D.fmt(D.ms(d) + n * 864e5); },
  dow(d) { return (new Date(D.ms(d)).getUTCDay() + 6) % 7 + 1; }, // 1 = Montag
  addMonths(d, n) { const [y, m] = d.split('-').map(Number); const t = new Date(Date.UTC(y, m - 1 + n, 1)); return D.fmt(t.getTime()); },
  firstOfMonth(d) { return d.slice(0, 8) + '01'; },
  diff(a, b) { return Math.round((D.ms(b) - D.ms(a)) / 864e5); },
};
const WEEKDAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const WD_LABEL = ['Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag', 'Sonntag'];
const MONTHS = ['Januar', 'Februar', 'März', 'April', 'Mai', 'Juni', 'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'];
function goalFor(s, d) { const g = s.weekdayGoals?.[WEEKDAYS[D.dow(d) - 1]]; return g ?? s.dailyGoal; }
function parseWeekGoals(t) { const o = {}; String(t || '').split(';').forEach(p => { const [k, v] = p.split('='); const n = parseInt(v, 10);
  if (WEEKDAYS.includes((k || '').trim()) && Number.isFinite(n)) o[k.trim()] = n; }); return o; }
function formatWeekGoals(o) { return WEEKDAYS.filter(k => o[k] != null).map(k => `${k}=${o[k]}`).join(';'); }

// ---- Verlauf (History + HistoryBuckets)
const GROUPING = { DAYS: { counts: [7, 14, 30], def: 7 }, WEEKS: { counts: [4, 8, 12], def: 8 }, MONTHS: { counts: [3, 6, 12], def: 6 } };
function firstDay(today, g, n) {
  if (g === 'DAYS') return D.add(today, -(n - 1));
  if (g === 'WEEKS') return D.add(D.add(today, -(D.dow(today) - 1)), -7 * (n - 1));
  return D.addMonths(D.firstOfMonth(today), -(n - 1));
}
function buildBuckets(today, g, n, totals, goalOf) {
  const by = new Map(totals.map(t => [t.date, t]));
  const ranges = [];
  for (let back = n - 1; back >= 0; back--) {
    if (g === 'DAYS') { const d = D.add(today, -back); ranges.push([d, d]); }
    else if (g === 'WEEKS') { const s = D.add(D.add(today, -(D.dow(today) - 1)), -7 * back); ranges.push([s, D.add(s, 6)]); }
    else { const s = D.addMonths(D.firstOfMonth(today), -back); ranges.push([s, D.add(D.addMonths(s, 1), -1)]); }
  }
  const avg = a => a.reduce((x, y) => x + y, 0) / a.length;
  return ranges.map(([start, endFull]) => {
    const end = endFull > today ? today : endFull;
    const dates = []; for (let d = start; d <= end; d = D.add(d, 1)) dates.push(d);
    const logged = dates.map(d => by.get(d)).filter(t => t && t.entries > 0);
    const past = logged.filter(t => t.date !== today); const base = past.length ? past : logged;
    return { start, end, days: dates.length, loggedDays: logged.length,
      kcal: base.length ? Math.round(avg(base.map(t => t.kcal))) : null, goal: Math.round(avg(dates.map(goalOf))),
      protein: base.length ? avg(base.map(t => t.protein)) : 0, carbs: base.length ? avg(base.map(t => t.carbs)) : 0,
      fat: base.length ? avg(base.map(t => t.fat)) : 0 };
  });
}
function summarize(today, nDays, totals, goalOf) {
  const days = buildBuckets(today, 'DAYS', nDays, totals, goalOf);
  const logged = days.filter(d => d.kcal != null || d.loggedDays);
  const real = days.filter(d => d.loggedDays > 0);
  const past = real.filter(d => d.start !== today); const base = past.length ? past : real;
  const avg = a => Math.round(a.reduce((x, y) => x + y, 0) / a.length);
  const raw = d => (totals.find(t => t.date === d.start) || {}).kcal || 0;
  return { days: days.length, loggedDays: real.length, overDays: real.filter(d => raw(d) > d.goal).length,
    average: base.length ? avg(base.map(raw)) : null, averageGoal: avg((base.length ? base : days).map(d => d.goal)) };
}
if (typeof module !== 'undefined') module.exports = { fmtNum, parseNum, normalize, rankSearch, portionsFor, quickCalc, isCalculation, D,
  buildBuckets, summarize, firstDay, parseWeekGoals, formatWeekGoals, goalFor, macroTargets, amountOf, describeUnit };
