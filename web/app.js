const state = { user: null, menu: [], reservations: [], orders: [] };
const $ = s => document.querySelector(s), $$ = s => [...document.querySelectorAll(s)];
const money = n => `BDT ${Number(n || 0).toFixed(2)}`;

async function api(path, options = {}) {
  const r = await fetch(path, options);
  const data = await r.json();
  if (!r.ok) throw new Error(data.error || 'Something went wrong');
  return data;
}

const formBody = form => new URLSearchParams(new FormData(form));

function toast(message) {
  const t = $('#toast');
  t.textContent = message;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2200);
}

function showPage(id) {
  $$('.page').forEach(x => x.classList.toggle('active', x.id === id));
  $$('nav button').forEach(x => x.classList.toggle('active', x.dataset.page === id));
  const names = { overview: `Good day, ${state.user?.role || 'there'}`, reservations: 'Reservations', orders: 'Orders', menu: 'Food menu', billing: 'Billing' };
  $('#pageTitle').textContent = names[id];
  document.querySelector('.app').classList.remove('menu-open');
  if (id === 'orders') loadOrders();
}

async function refresh() {
  [state.menu, state.reservations] = await Promise.all([api('/api/menu'), api('/api/reservations')]);
  renderAll();
  await loadOrders();
}

function renderAll() {
  const active = state.reservations.filter(r => r.status === 'Active');
  $('#statReservations').textContent = active.length;
  $('#statMenu').textContent = state.menu.length;
  $('#reservationRows').innerHTML = state.reservations.map(r => `<tr><td>#${r.id}</td><td><b>${esc(r.guestName)}</b></td><td>Table ${r.tableNumber}</td><td>${esc(r.contactNo)}</td><td><span class="badge ${r.status.toLowerCase()}">${r.status}</span></td><td class="manager-only"><button class="icon-btn danger" onclick="removeReservation(${r.id})">Remove</button></td></tr>`).join('') || emptyRow(6, 'No reservations yet.');
  $('#recentReservations').innerHTML = `<div class="reservation-cards">${state.reservations.slice(-3).reverse().map(r => `<div class="mini-reservation"><b>${esc(r.guestName)}</b><span>Table ${r.tableNumber} - #${r.id}</span><span class="badge ${r.status.toLowerCase()}">${r.status}</span></div>`).join('') || '<div class="empty-state">No reservations yet.</div>'}</div>`;
  $('#menuGrid').innerHTML = state.menu.map(i => `<article class="menu-card"><span class="eyebrow">ITEM #${i.id}</span><h3>${esc(i.name)}</h3><b>${money(i.price)}</b><div class="menu-actions manager-only"><button class="secondary" onclick="editPrice(${i.id},'${escAttr(i.name)}',${i.price})">Edit price</button><button class="secondary danger" onclick="removeMenu(${i.id})">Remove</button></div></article>`).join('');
  const activeOptions = active.map(r => `<option value="${r.id}">#${r.id} - ${esc(r.guestName)} - Table ${r.tableNumber}</option>`).join('');
  const allOptions = state.reservations.map(r => `<option value="${r.id}">#${r.id} - ${esc(r.guestName)} (${r.status})</option>`).join('');
  ['#orderReservation', '#modalReservation'].forEach(s => { $(s).innerHTML = activeOptions || '<option value="">No active reservations</option>'; });
  $('#billReservation').innerHTML = allOptions || '<option value="">No reservations</option>';
  $('#modalMenu').innerHTML = state.menu.map(i => `<option value="${i.id}">${esc(i.name)} - ${money(i.price)}</option>`).join('');
  applyRole();
}

async function loadOrders() {
  const rid = $('#orderReservation')?.value;
  if (!rid) {
    state.orders = [];
    renderOrders();
    return;
  }
  state.orders = await api(`/api/orders?reservationId=${rid}`);
  renderOrders();
}

function renderOrders() {
  const total = state.orders.reduce((s, o) => s + o.total, 0);
  $('#statOrders').textContent = state.orders.length;
  $('#statSales').textContent = money(total);
  $('#orderSummary').textContent = `${state.orders.length} items - ${money(total)}`;
  $('#orderRows').innerHTML = state.orders.map(o => `<tr><td><b>${esc(o.itemName)}</b></td><td>${money(o.price)}</td><td>${o.quantity}</td><td><b>${money(o.total)}</b></td><td class="waiter-only"><button class="icon-btn danger" onclick="removeOrder(${o.id})">Remove</button></td></tr>`).join('') || emptyRow(5, 'No order items for this reservation.');
  applyRole();
}

function applyRole() {
  if (!state.user) return;
  const manager = state.user.role === 'Manager';
  $$('.manager-only').forEach(x => x.classList.toggle('hidden', !manager));
  $$('.waiter-only').forEach(x => x.classList.toggle('hidden', manager));
}

$('#loginForm').addEventListener('submit', async e => {
  e.preventDefault();
  $('#loginError').textContent = '';
  try {
    state.user = await api('/api/login', { method: 'POST', body: formBody(e.target) });
    $('#loginView').classList.add('hidden');
    $('#appView').classList.remove('hidden');
    $('#userName').textContent = state.user.username;
    $('#userRole').textContent = state.user.role;
    $('#userInitial').textContent = state.user.role[0];
    await refresh();
    showPage('overview');
  } catch (err) {
    $('#loginError').textContent = err.message;
  }
});

$$('nav button').forEach(b => b.addEventListener('click', () => showPage(b.dataset.page)));
$$('[data-go]').forEach(b => b.addEventListener('click', () => showPage(b.dataset.go)));
$$('[data-modal]').forEach(b => b.addEventListener('click', () => $('#' + b.dataset.modal).showModal()));
$('#menuToggle').onclick = () => $('#appView').classList.toggle('menu-open');
$('#logoutBtn').onclick = () => location.reload();
$('#orderReservation').onchange = loadOrders;

function bindForm(id, path, message) {
  $(id).addEventListener('submit', async e => {
    e.preventDefault();
    if (e.submitter?.value === 'cancel') {
      e.target.closest('dialog').close();
      return;
    }
    try {
      await api(path, { method: 'POST', body: formBody(e.target) });
      e.target.reset();
      e.target.closest('dialog').close();
      await refresh();
      toast(message);
    } catch (err) {
      toast(err.message);
    }
  });
}

bindForm('#reservationForm', '/api/reservations', 'Reservation created');
bindForm('#orderForm', '/api/orders', 'Order item added');
bindForm('#menuForm', '/api/menu', 'Menu item added');

async function removeOrder(id) {
  try {
    await api(`/api/orders?id=${id}`, { method: 'DELETE' });
    await loadOrders();
    toast('Order item removed');
  } catch (e) {
    toast(e.message);
  }
}

async function removeReservation(id) {
  if (!confirm('Remove this reservation? This will also remove its orders.')) return;
  try {
    await api(`/api/reservations?id=${id}`, { method: 'DELETE' });
    await refresh();
    toast('Reservation removed');
  } catch (e) {
    toast(e.message);
  }
}

async function removeMenu(id) {
  if (!confirm('Remove this menu item?')) return;
  try {
    await api(`/api/menu?id=${id}`, { method: 'DELETE' });
    await refresh();
    toast('Menu item removed');
  } catch (e) {
    toast(e.message);
  }
}

async function editPrice(id, name, current) {
  const price = prompt(`New price for ${name}:`, current);
  if (!price) return;
  try {
    await api('/api/menu', { method: 'PUT', body: new URLSearchParams({ id, price }) });
    await refresh();
    toast('Price updated');
  } catch (e) {
    toast(e.message);
  }
}

$('#previewBill').onclick = async () => {
  const rid = $('#billReservation').value;
  if (!rid) return;
  try {
    renderReceipt(await api(`/api/bill?reservationId=${rid}`));
  } catch (e) {
    toast(e.message);
  }
};

function renderReceipt(b) {
  $('#receipt').innerHTML = `<div class="receipt-head"><span class="logo" style="margin:auto">R</span><h2>Restaurant Bill</h2><p>#${b.reservation.id} - ${esc(b.reservation.guestName)} - Table ${b.reservation.tableNumber}</p></div>${b.orders.map(o => `<div class="receipt-line"><span>${esc(o.itemName)} x ${o.quantity}</span><b>${money(o.total)}</b></div>`).join('')}<div class="receipt-totals"><div class="receipt-line"><span>Subtotal</span><b>${money(b.subtotal)}</b></div><div class="receipt-line"><span>Tax (5%)</span><b>${money(b.tax)}</b></div><div class="receipt-line total"><span>Total</span><b>${money(b.total)}</b></div></div><div class="receipt-actions"><button class="secondary" id="printBill">Print bill</button><button class="primary" id="completeBill">Complete billing</button></div>`;
  $('#printBill').onclick = () => printBill(b);
  $('#completeBill').onclick = async () => {
    try {
      await api('/api/bill', { method: 'POST', body: new URLSearchParams({ reservationId: b.reservation.id }) });
      await refresh();
      $('#receipt').innerHTML = '<div class="empty-state"><b>Billing completed.</b><br>The reservation is now closed.</div>';
      toast('Billing completed');
    } catch (e) {
      toast(e.message);
    }
  };
}

function printBill(b) {
  const w = window.open('', '_blank', 'width=900,height=900');
  if (!w) {
    toast('Allow pop-ups to print the bill');
    return;
  }
  const items = b.orders.map(o => `<div class="line"><span>${esc(o.itemName)} x ${o.quantity}</span><b>${money(o.total)}</b></div>`).join('');
  w.document.write(`<!doctype html><html><head><title>Restaurant Bill #${b.reservation.id}</title><style>body{font-family:Arial,sans-serif;margin:40px;color:#222}.wrap{max-width:720px;margin:0 auto}h1,h2,p{margin:0}.head{text-align:center;padding-bottom:18px;border-bottom:2px dashed #bbb}.line{display:flex;justify-content:space-between;padding:8px 0}.totals{border-top:2px dashed #bbb;margin-top:10px;padding-top:10px}.total{font-size:1.2rem;font-weight:700}@media print{button{display:none}}</style></head><body><div class="wrap"><div class="head"><h1>Restaurant Bill</h1><p>#${b.reservation.id} - ${esc(b.reservation.guestName)} - Table ${b.reservation.tableNumber}</p></div>${items}<div class="totals"><div class="line"><span>Subtotal</span><b>${money(b.subtotal)}</b></div><div class="line"><span>Tax (5%)</span><b>${money(b.tax)}</b></div><div class="line total"><span>Total</span><b>${money(b.total)}</b></div></div></div><script>window.onload=()=>{window.print();setTimeout(()=>window.close(),250)}<\/script></body></html>`);
  w.document.close();
}

function esc(v) { return String(v).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[c])); }
function escAttr(v) { return esc(v).replace(/'/g, '&#039;'); }
function emptyRow(n, msg) { return `<tr><td colspan="${n}" class="empty-state">${msg}</td></tr>`; }

$('#dateLabel').textContent = new Intl.DateTimeFormat('en', { weekday: 'long', month: 'long', day: 'numeric' }).format(new Date());
