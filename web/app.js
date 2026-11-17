const state = {
  products: [],
  cart: { items: [], quantidadeTotal: 0, subtotalCentavos: 0 },
  orders: [],
  orderView: 'active'
};

const el = (id) => document.getElementById(id);
const money = (cents) => (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

async function api(url, options = {}) {
  const response = await fetch(url, options);
  const data = await response.json();
  if (!response.ok) throw new Error(data.erro || `Falha HTTP ${response.status}`);
  return data;
}
function post(url, fields = {}) {
  return api(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
    body: new URLSearchParams(fields)
  });
}

function safeText(value) {
  return String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

let toastTimer;
function toast(message, error = false) {
  const target = el('toast');
  target.textContent = message;
  target.classList.toggle('error', error);
  target.classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => target.classList.remove('visible'), 3100);
}
function showShop() {
  el('shopView').hidden = false;
  el('ordersView').hidden = true;
  el('hero').hidden = false;
  el('tabShop').classList.add('selected');
  el('tabOrders').classList.remove('selected');
  closeCart();
}

async function showOrders() {
  el('shopView').hidden = true;
  el('ordersView').hidden = false;
  el('hero').hidden = true;
  el('tabOrders').classList.add('selected');
  el('tabShop').classList.remove('selected');
  closeCart();
  await refreshOrders();
}
function openCart() {
  el('cartOverlay').hidden = false;
  el('cartPanel').classList.add('open');
  el('cartPanel').setAttribute('aria-hidden', 'false');
}
function closeCart() {
  el('cartOverlay').hidden = true;
  el('cartPanel').classList.remove('open');
  el('cartPanel').setAttribute('aria-hidden', 'true');
}

function renderProducts() {
  el('products').innerHTML = state.products.map(p => `
    <article class="product-card">
      <div class="product-art ${p.tom}"><span class="stock-label">${p.estoque} em estoque</span><span class="emoji">${p.emoji}</span></div>
      <div class="product-body">
        <span class="product-category">${safeText(p.categoria)}</span>
        <h3>${safeText(p.nome)}</h3><p>${safeText(p.descricao)}</p>
        <div class="product-bottom"><span class="product-price">${money(p.precoCentavos)}</span>
          <button class="add-button" data-add="${p.id}" ${p.estoque === 0 ? 'disabled' : ''}>+ Adicionar</button>
        </div>
      </div>
    </article>`).join('');
}

async function refreshProducts() {
  state.products = await api('/api/products');
  renderProducts();
}

function renderCart() {
  el('cartCount').textContent = state.cart.quantidadeTotal;
  el('cartTitleCount').textContent = `(${state.cart.quantidadeTotal})`;
  el('cartSubtotal').textContent = money(state.cart.subtotalCentavos);
  el('checkoutButton').disabled = state.cart.items.length === 0;
  if (state.cart.items.length === 0) {
    el('cartItems').innerHTML = '<div class="empty-state"><div class="empty-emoji">🛒</div><h3>Seu carrinho está vazio</h3><p>Escolha um produto para começar.</p></div>';
    return;
  }
  el('cartItems').innerHTML = state.cart.items.map(item => `
    <div class="cart-item">
      <div class="item-thumb">${item.emoji}</div>
      <div class="item-info"><strong>${safeText(item.nome)}</strong><small>${money(item.precoCentavos)} / unidade</small>
      <div class="item-quantity">Quantidade: ${item.quantidade} · ${money(item.totalCentavos)}</div></div>
      <button class="remove-button" type="button" data-remove="${item.produtoId}" aria-label="Remover ${safeText(item.nome)}">✕</button>
    </div>`).join('');
}

async function refreshCart() {
  state.cart = await api('/api/cart');
  renderCart();
}

async function addProduct(productId) {
  const alreadyListed = state.cart.items.some(item => item.produtoId === productId);
  try {
    await post('/api/cart/items', { produtoId: productId, quantidade: 1});
    await refreshCart();
    toast('Produto adicionado ao carrinho.');
  } catch (error) { toast(error.message, true); }
}

async function removeProduct(productId) {
  try {
    await api(`/api/cart/items/${productId}`, { method: 'DELETE' });
    await refreshCart();
    toast('Produto removido.');
  } catch (error) { toast(error.message, true); }
}

async function checkout() {
  try {
    const cupom = el('couponInput').value.trim();
    const pedido = await post('/api/checkout', { cupom });
    await Promise.all([refreshCart(), refreshProducts(), refreshOrders()]);
    el('couponInput').value = '';
    closeCart();
    toast(`Pedido #${pedido.id} confirmado! Total: ${money(pedido.totalCentavos)}`);
    await showOrders();
  } catch (error) { toast(error.message, true); }
}

function statusLabel(status) {
  return status === 'EM_ABERTO' ? 'Em aberto' : status === 'CANCELADO' ? 'Cancelado' : 'Entregue';
}

function renderOrders() {
  el('ordersCount').textContent = state.orders.filter(o => o.status === 'EM_ABERTO').length;
  if (state.orders.length === 0) {
    el('ordersList').innerHTML = '<div class="empty-state"><div class="empty-emoji">📦</div><h3>Nenhum pedido nesta visualização</h3><p>Suas compras aparecerão aqui.</p></div>';
    return;
  }
  el('ordersList').innerHTML = state.orders.map(pedido => `
    <article class="order-card">
      <div><h3>Pedido #${pedido.id}</h3><p>${pedido.itens.map(i => `${i.quantidade}× ${safeText(i.nome)}`).join(' · ')}</p>
      <p>Pedido registrado no ambiente demonstrativo</p></div>
      <div class="order-right"><span class="status ${pedido.status === 'CANCELADO' ? 'cancelled' : ''}">${statusLabel(pedido.status)}</span>
      <strong>${money(pedido.totalCentavos)}</strong>
      ${pedido.status === 'EM_ABERTO' ? `<button class="tiny-button" data-cancel="${pedido.id}">Cancelar pedido</button>` : ''}</div>
    </article>`).join('');
}

async function refreshOrders() {
  state.orders = await api(`/api/orders?view=${state.orderView}`);
  renderOrders();
}

async function cancelOrder(orderId) {
  try {
    await post(`/api/orders/${orderId}/cancel`);
    await refreshOrders();
    toast(`Pedido #${orderId} cancelado.`);
  } catch (error) { toast(error.message, true); }
}

async function lookupProduct(event) {
  event.preventDefault();
  const id = el('lookupId').value.trim();
  const output = el('lookupResult');
  output.textContent = 'Consultando...';
  try {
    const produto = await api(`/api/products/${encodeURIComponent(id)}`);
    output.textContent = `${produto.emoji} ${produto.nome} · ${money(produto.precoCentavos)} · estoque: ${produto.estoque}`;
  } catch (error) {
    output.textContent = `Não foi possível consultar: ${error.message}`;
  }
}

function setOrderView(view) {
  state.orderView = view;
  el('ordersActive').classList.toggle('current', view === 'active');
  el('ordersAll').classList.toggle('current', view === 'all');
  refreshOrders().catch(err => toast(err.message, true));
}

async function initialize() {
  el('tabShop').addEventListener('click', showShop);
  el('tabOrders').addEventListener('click', () => showOrders().catch(err => toast(err.message, true)));
  document.querySelectorAll('[data-go="shop"]').forEach(a => a.addEventListener('click', (e) => { e.preventDefault(); showShop(); }));
  el('heroButton').addEventListener('click', () => el('products').scrollIntoView({ behavior: 'smooth' }));
  el('cartToggle').addEventListener('click', openCart);
  el('cartClose').addEventListener('click', closeCart);
  el('cartOverlay').addEventListener('click', closeCart);
  el('checkoutButton').addEventListener('click', checkout);
  el('lookupForm').addEventListener('submit', lookupProduct);
  el('ordersActive').addEventListener('click', () => setOrderView('active'));
  el('ordersAll').addEventListener('click', () => setOrderView('all'));
  document.addEventListener('click', (event) => {
    const add = event.target.closest('[data-add]');
    const remove = event.target.closest('[data-remove]');
    const cancel = event.target.closest('[data-cancel]');
    if (add) addProduct(Number(add.dataset.add));
    if (remove) removeProduct(Number(remove.dataset.remove));
    if (cancel) cancelOrder(Number(cancel.dataset.cancel));
  });
  try {
    await Promise.all([refreshProducts(), refreshCart(), refreshOrders()]);
  } catch (error) { toast('Não foi possível conectar ao servidor: ' + error.message, true); }
}
initialize();
