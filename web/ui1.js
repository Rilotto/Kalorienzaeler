// ===== Farben: dieselben 8 Schemas wie in der App (Color.kt) =====
const K = ['bg', 'surface', 'line', 'fg', 'muted', 'accent', 'accentSoft', 'over', 'overSoft', 'protein', 'carbs', 'fat', 'sugar'];
const L = (bg, line, fg, muted, accent, accentSoft, o = {}) => ({ bg, surface: o.surface || '#FFFFFF', line, fg, muted, accent, accentSoft,
  over: o.over || '#B4432F', overSoft: o.overSoft || '#F6DDD7', protein: o.protein || '#3C6FA8', carbs: o.carbs || '#C08A1E', fat: o.fat || '#8A4F9E', sugar: o.sugar || '#C2577A' });
const Dk = (bg, surface, line, fg, muted, accent, accentSoft, o = {}) => ({ bg, surface, line, fg, muted, accent, accentSoft,
  over: o.over || '#E7826D', overSoft: o.overSoft || '#3A221D', protein: o.protein || '#7FA9DA', carbs: o.carbs || '#E0B456', fat: o.fat || '#C595D6', sugar: o.sugar || '#E58FAE' });
const PALETTES = {
  DEFAULT: { name: 'Salbei', light: L('#F3F5F2', '#DDE3DC', '#18201B', '#5E6B63', '#2F6B4F', '#DCEBE2'), dark: Dk('#0F1411', '#18201B', '#2A342E', '#E8EEE9', '#95A39A', '#6FBF93', '#1F3328') },
  PEACH: { name: 'Pfirsich', light: L('#FFF6F1', '#F2DED3', '#2B1D17', '#7A625A', '#B5552F', '#FBE3D7', { over: '#B23A48', overSoft: '#F8DADF' }), dark: Dk('#1A1311', '#241A17', '#3A2C27', '#F5E9E3', '#B9A39A', '#F0A07F', '#3D2820', { over: '#F08A96', overSoft: '#3E2226' }) },
  LAVENDER: { name: 'Lavendel', light: L('#F6F4FB', '#E1DCEF', '#1F1B2B', '#6A6480', '#6B57B0', '#E6E0F6'), dark: Dk('#13111A', '#1C1926', '#2E2A3D', '#ECE9F5', '#A39DB8', '#B5A6F0', '#2C2645') },
  MINT: { name: 'Minze', light: L('#F1F8F6', '#D5EAE4', '#15231F', '#5A716A', '#1C7564', '#D3EFE8'), dark: Dk('#0E1513', '#16201D', '#26352F', '#E4F1EC', '#93ABA3', '#74D3BE', '#1A3530') },
  SKY: { name: 'Himmel', light: L('#F2F6FB', '#D9E3EF', '#172231', '#5C6B7E', '#2F6DA8', '#DCE8F6', { protein: '#5B5FC7' }), dark: Dk('#0F141B', '#172029', '#283444', '#E6EEF7', '#98A8BA', '#86B8EA', '#1E3147', { protein: '#A3A6F5' }) },
  ROSE: { name: 'Rosé', light: L('#FBF3F5', '#EFDCE2', '#2A1A20', '#7A6068', '#A3416A', '#F6DEE6', { over: '#C0392B', overSoft: '#F7DAD5', sugar: '#D9822B' }), dark: Dk('#1A1215', '#241A1E', '#3A2A30', '#F5E8EC', '#B89DA6', '#EE9AB8', '#3D2330', { over: '#F28B6E', sugar: '#F0B070' }) },
  MEMPHIS: { name: 'Memphis (90er)', light: L('#FFF8E7', '#1F1F1F', '#111111', '#55524A', '#007C7C', '#FFE45C', { over: '#D6006F', overSoft: '#FFD1E8', protein: '#3D5AFE', carbs: '#FF8F00', fat: '#7B2CBF', sugar: '#D6006F' }),
    dark: Dk('#141414', '#1F1F1F', '#4A4A4A', '#F5F5F5', '#A8A8A8', '#2EE6D6', '#3A3410', { over: '#FF4FA3', overSoft: '#3D1A2C', protein: '#8C9EFF', carbs: '#FFB84D', fat: '#C77DFF', sugar: '#FF4FA3' }) },
  VAPORWAVE: { name: 'Vaporwave (90er)', light: L('#FDF0FF', '#F1C6F5', '#2A1240', '#7A5C8F', '#8E2BC4', '#D9F7FF', { over: '#E0197A', overSoft: '#FFD6EA', protein: '#0391B5', carbs: '#E0951A', fat: '#B15CF0', sugar: '#F054B8' }),
    dark: Dk('#1A0B2E', '#241040', '#3D2266', '#F7E8FF', '#B9A3D6', '#01CDFE', '#2B1F5C', { over: '#FF71CE', overSoft: '#4A1840', protein: '#7DF9FF', carbs: '#FFFB96', fat: '#B967FF', sugar: '#FF71CE' }) },
};
const mq = window.matchMedia('(prefers-color-scheme: dark)');
function isDark() {
  const m = S.settings.themeMode; if (m === 'DARK') return true; if (m === 'LIGHT') return false;
  const host = document.documentElement.getAttribute('data-theme'); if (host === 'dark') return true; if (host === 'light') return false;
  return mq.matches;
}
let applyingTheme = false;
function applyTheme() {
  const pal = PALETTES[S.settings.palette] || PALETTES.DEFAULT; const dark = isDark(); const t = dark ? pal.dark : pal.light;
  const st = document.documentElement.style;
  applyingTheme = true;
  for (const k of K) st.setProperty('--' + k.replace(/[A-Z]/g, c => '-' + c.toLowerCase()), t[k]);
  st.setProperty('color-scheme', dark ? 'dark' : 'light');
  applyingTheme = false;
  const tc = document.querySelector('meta[name="theme-color"]'); if (tc) tc.content = t.bg;
}
mq.addEventListener?.('change', applyTheme);
new MutationObserver(() => { if (!applyingTheme) applyTheme(); }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });

// ===== kleine Helfer =====
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const ICON = {
  plus: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>',
  left: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 6l-6 6 6 6"/></svg>',
  right: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6l6 6-6 6"/></svg>',
  more: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="5" r="1.4"/><circle cx="12" cy="12" r="1.4"/><circle cx="12" cy="19" r="1.4"/></svg>',
  heart: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"/></svg>',
  pen: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20h4L19 9l-4-4L4 16v4z"/></svg>',
  search: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6"/><path d="M20 20l-4.5-4.5"/></svg>',
  trash: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12"/></svg>',
  home: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 11l8-6 8 6v8h-5v-5H9v5H4z"/></svg>',
  chart: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19V11M12 19V5M19 19v-8"/></svg>',
  gear: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="3"/><path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M5.6 18.4l2.1-2.1M16.3 7.7l2.1-2.1"/></svg>',
};
function dayTitle(d) {
  const t = D.today(); if (d === t) return 'Heute'; if (d === D.add(t, -1)) return 'Gestern';
  const dt = new Date(D.ms(d)); return `${WD_LABEL[D.dow(d) - 1]}, ${dt.getUTCDate()}. ${MONTHS[dt.getUTCMonth()]}`;
}
function dayLong(d) { const dt = new Date(D.ms(d)); return `${WD_LABEL[D.dow(d) - 1]}, ${dt.getUTCDate()}. ${MONTHS[dt.getUTCMonth()]} ${dt.getUTCFullYear()}`; }
function entryAmount(e) {
  if (e.unitLabel && e.unitCount != null && e.grams != null) return describeUnit(e.unitCount, e.unitLabel, e.grams, e.isLiquid);
  if (e.unitLabel && e.unitCount != null) return `${fmtNum(e.unitCount)} × ${e.unitLabel}`;
  if (e.grams != null) return `${fmtNum(e.grams)} ${e.isLiquid ? 'ml' : 'g'}`;
  return null;
}
function chips(items, sel, attr) {
  return `<div class="chips">${items.map(([v, l]) => `<button type="button" class="chip${String(v) === String(sel) ? ' on' : ''}" data-${attr}="${esc(v)}">${esc(l)}</button>`).join('')}</div>`;
}
const mealChips = (sel) => chips(MEALS.map(m => [m, MEAL_LABEL[m]]), sel, 'meal');
const COUNTS = [[0.5, '½'], [1, '1'], [1.5, '1½'], [2, '2'], [3, '3']];

// ===== Hinweisleiste mit „Rückgängig“ =====
let snackTimer;
function snack(msg, undo) {
  const el = $('#snack'); el.innerHTML = `<span>${esc(msg)}</span>${undo ? '<button type="button" id="snack-undo">Rückgängig</button>' : ''}`;
  el.hidden = false; clearTimeout(snackTimer);
  if (undo) $('#snack-undo').onclick = () => { el.hidden = true; undo(); render(); };
  snackTimer = setTimeout(() => { el.hidden = true; }, undo ? 6000 : 3000);
}

// ===== Unterfenster (Bottom-Sheet) =====
let sheetStack = [];
function openSheet(build) {
  sheetStack.push(build); drawSheet();
}
function replaceSheet(build) { sheetStack[sheetStack.length - 1] = build; drawSheet(); }
function closeSheet() { sheetStack.pop(); drawSheet(); render(); }
function closeAllSheets() { sheetStack = []; drawSheet(); render(); }
function drawSheet() {
  const wrap = $('#sheet-wrap'); const body = $('#sheet');
  if (!sheetStack.length) { wrap.hidden = true; body.innerHTML = ''; document.body.classList.remove('locked'); return; }
  wrap.hidden = false; document.body.classList.add('locked');
  const build = sheetStack[sheetStack.length - 1];
  const scroll = body.scrollTop; body.innerHTML = ''; build(body); if (body.dataset.keep) body.scrollTop = scroll;
}
function redrawSheet() { if (sheetStack.length) { $('#sheet').dataset.keep = '1'; drawSheet(); delete $('#sheet').dataset.keep; } }
