const state = { user: null, products: [], cart: null };
const $ = (selector) => document.querySelector(selector);
const money = (value) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value || 0);
const api = async (url, options = {}) => {
  const response = await fetch(url, { headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }, ...options });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(data?.message || 'No se pudo completar la operación');
  return data;
};
const toast = (message) => { const node = $('#toast'); node.textContent = message; node.classList.add('show'); setTimeout(() => node.classList.remove('show'), 2800); };
const requireLogin = () => { if (!state.user) { openLogin(); return false; } return true; };
function openLogin() { $('#login-modal').classList.remove('hidden'); $('#username').focus(); }
function closeLogin() { $('#login-modal').classList.add('hidden'); $('#login-form').reset(); }
function showView(view) {
  document.querySelectorAll('.view').forEach((node) => node.classList.add('hidden'));
  $(`#${view}-view`).classList.remove('hidden');
  document.querySelectorAll('.nav-link').forEach((node) => node.classList.toggle('active', node.dataset.view === view));
  if (view === 'carrito' && state.user?.rol === 'CLIENTE') loadCart();
  if (view === 'pedidos' && state.user?.rol === 'CLIENTE') loadOrders();
  if (view === 'admin' && state.user?.rol === 'ADMIN') renderAdmin();
}
async function loadSession() {
  try {
    const session = await api('/api/auth/me');
    if (session.authenticated) state.user = session;
  } catch (error) { console.error(error); }
  updateSessionUI();
}
function updateSessionUI() {
  const user = state.user;
  $('#session-label').textContent = user ? `${user.username} · ${user.rol}` : 'Visitante';
  $('#login-open').classList.toggle('hidden', !!user);
  $('#logout-button').classList.toggle('hidden', !user);
  $('#orders-link').classList.toggle('hidden', user?.rol !== 'CLIENTE');
  $('#admin-link').classList.toggle('hidden', user?.rol !== 'ADMIN');
}
async function loadProducts() {
  try { state.products = await api('/api/productos'); renderProducts(); populateCategories(); } catch (error) { $('#products-grid').innerHTML = `<div class="empty"><h3>Catálogo no disponible</h3><p>${error.message}</p></div>`; }
}
function populateCategories() {
  const categories = [...new Set(state.products.map((product) => product.categoria))];
  $('#category-filter').innerHTML = '<option value="">Todas las categorías</option>' + categories.map((category) => `<option>${category}</option>`).join('');
}
function renderProducts() {
  const query = $('#search-input').value.toLowerCase();
  const category = $('#category-filter').value;
  const products = state.products.filter((product) => (!category || product.categoria === category) && (!query || `${product.nombre} ${product.categoria}`.toLowerCase().includes(query)));
  $('#products-grid').innerHTML = products.length ? products.map((product, index) => `
    <article class="product-card">
      <div class="product-visual">${product.nombre.slice(0, 1)}</div>
      <span class="product-category">${product.categoria}</span><div class="product-name">${product.nombre}</div>
      <div class="product-meta"><span class="price">${money(product.precio)}</span><span class="stock ${product.stock === 0 ? 'out' : ''}">${product.stock ? `${product.stock} disponibles` : 'Agotado'}</span></div>
      <button class="add-button" data-add="${product.id}" ${product.stock === 0 ? 'disabled' : ''}>${product.stock ? 'Agregar al carrito' : 'Sin stock'}</button>
    </article>`).join('') : '<div class="empty"><h3>Sin coincidencias</h3><p>Prueba otra búsqueda o categoría.</p></div>';
  document.querySelectorAll('[data-add]').forEach((button) => button.addEventListener('click', () => addToCart(Number(button.dataset.add))));
}
async function addToCart(productoId) {
  if (!requireLogin()) return;
  if (state.user.rol !== 'CLIENTE') { toast('Solo los clientes pueden comprar'); return; }
  try { state.cart = await api('/api/carrito', { method: 'POST', body: JSON.stringify({ productoId, cantidad: 1 }) }); updateCartCount(); toast('Producto agregado al carrito'); } catch (error) { toast(error.message); }
}
async function loadCart() {
  try { state.cart = await api('/api/carrito'); renderCart(); updateCartCount(); } catch (error) { $('#cart-content').innerHTML = `<div class="empty"><h3>No se pudo cargar el carrito</h3><p>${error.message}</p></div>`; }
}
function updateCartCount() { $('#cart-count').textContent = state.cart?.detalles?.reduce((total, detail) => total + detail.cantidad, 0) || 0; }
function renderCart() {
  if (!state.cart?.detalles?.length) { $('#cart-content').innerHTML = '<div class="empty"><h3>Tu carrito está esperando</h3><p>Agrega algo especial desde el catálogo.</p><button class="button button-accent" data-view="catalogo">Ver catálogo</button></div>'; document.querySelector('[data-view="catalogo"]')?.addEventListener('click', () => showView('catalogo')); return; }
  $('#cart-content').innerHTML = `<div class="cart-layout"><div class="panel">${state.cart.detalles.map((detail) => `<div class="cart-row"><div class="cart-product"><span class="mini-visual">${detail.producto.slice(0, 1)}</span><span>${detail.producto}</span></div><div class="qty"><button data-qty="${detail.id}" data-delta="-1">−</button><strong>${detail.cantidad}</strong><button data-qty="${detail.id}" data-delta="1">+</button></div><strong>${money(detail.subtotal)}</strong><button class="cart-remove" data-remove="${detail.id}">Eliminar</button></div>`).join('')}</div><aside class="panel summary"><p class="eyebrow">RESUMEN</p><h3>Tu pedido</h3><div class="summary-line"><span>Productos</span><span>${state.cart.detalles.length}</span></div><div class="summary-line summary-total"><span>Total</span><span>${money(state.cart.total)}</span></div><button class="button button-accent wide" id="confirm-order">Confirmar compra</button><button class="button button-light wide" id="clear-cart" style="margin-top:10px">Vaciar carrito</button></aside></div>`;
  document.querySelectorAll('[data-qty]').forEach((button) => button.addEventListener('click', () => changeQuantity(Number(button.dataset.qty), Number(button.dataset.delta))));
  document.querySelectorAll('[data-remove]').forEach((button) => button.addEventListener('click', () => removeDetail(Number(button.dataset.remove))));
  $('#confirm-order').addEventListener('click', confirmOrder); $('#clear-cart').addEventListener('click', clearCart);
}
async function changeQuantity(id, delta) {
  const detail = state.cart.detalles.find((item) => item.id === id); const quantity = detail.cantidad + delta;
  if (quantity < 1) return removeDetail(id);
  try { state.cart = await api(`/api/carrito/detalle/${id}`, { method: 'PUT', body: JSON.stringify({ productoId: detail.productoId, cantidad: quantity }) }); renderCart(); updateCartCount(); } catch (error) { toast(error.message); }
}
async function removeDetail(id) { try { state.cart = await api(`/api/carrito/detalle/${id}`, { method: 'DELETE' }); renderCart(); updateCartCount(); toast('Producto eliminado'); } catch (error) { toast(error.message); } }
async function clearCart() { try { await api('/api/carrito', { method: 'DELETE' }); state.cart = { detalles: [], total: 0 }; renderCart(); updateCartCount(); toast('Carrito vaciado'); } catch (error) { toast(error.message); } }
async function confirmOrder() { try { const order = await api('/api/pedidos/confirmar', { method: 'POST' }); state.cart = { detalles: [], total: 0 }; updateCartCount(); renderCart(); toast(`Pedido #${order.id} confirmado`); } catch (error) { toast(error.message); } }
async function loadOrders() {
  try { const orders = await api('/api/pedidos'); $('#orders-content').innerHTML = orders.length ? orders.map((order) => `<article class="order-card"><div class="order-top"><div><strong>Pedido #${order.id}</strong><div class="muted">${new Date(order.fecha).toLocaleString('es-PE')}</div></div><span class="badge">${order.estado}</span></div>${order.detalles.map((detail) => `<div class="order-item"><span>${detail.cantidad} × ${detail.producto}</span><strong>${money(detail.subtotal)}</strong></div>`).join('')}<div class="summary-line summary-total"><span>Total</span><span>${money(order.total)}</span></div></article>`).join('') : '<div class="empty"><h3>Aún no tienes pedidos</h3><p>Tu historial aparecerá aquí después de confirmar una compra.</p></div>'; } catch (error) { $('#orders-content').innerHTML = `<div class="empty"><p>${error.message}</p></div>`; }
}
function renderAdmin() {
  $('#admin-products').innerHTML = state.products.map((product) => `<div class="admin-product"><div><strong>${product.nombre}</strong><small>${product.categoria}</small></div><span>${money(product.precio)}</span><span class="admin-stock">${product.stock} uds.</span><div class="admin-actions"><button data-edit="${product.id}">Editar</button><button data-delete="${product.id}">Eliminar</button></div></div>`).join('');
  document.querySelectorAll('[data-edit]').forEach((button) => button.addEventListener('click', () => editProduct(Number(button.dataset.edit))));
  document.querySelectorAll('[data-delete]').forEach((button) => button.addEventListener('click', () => deleteProduct(Number(button.dataset.delete))));
}
function editProduct(id) { const product = state.products.find((item) => item.id === id); $('#product-id').value = product.id; $('#product-name').value = product.nombre; $('#product-category').value = product.categoria; $('#product-price').value = product.precio; $('#product-stock').value = product.stock; $('#form-title').textContent = 'Editar producto'; $('#cancel-edit').classList.remove('hidden'); }
function resetProductForm() { $('#product-form').reset(); $('#product-id').value = ''; $('#form-title').textContent = 'Nuevo producto'; $('#cancel-edit').classList.add('hidden'); }
async function deleteProduct(id) { if (!confirm('¿Eliminar este producto?')) return; try { await api(`/api/productos/${id}`, { method: 'DELETE' }); await loadProducts(); renderAdmin(); toast('Producto eliminado'); } catch (error) { toast(error.message); } }
$('#product-form').addEventListener('submit', async (event) => { event.preventDefault(); const id = $('#product-id').value; const body = { nombre: $('#product-name').value, categoria: $('#product-category').value, precio: Number($('#product-price').value), stock: Number($('#product-stock').value) }; try { await api(id ? `/api/productos/${id}` : '/api/productos', { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) }); resetProductForm(); await loadProducts(); renderAdmin(); toast(id ? 'Producto actualizado' : 'Producto creado'); } catch (error) { toast(error.message); } });
$('#login-form').addEventListener('submit', async (event) => { event.preventDefault(); try { state.user = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username: $('#username').value, password: $('#password').value }) }); closeLogin(); updateSessionUI(); toast(`Bienvenido, ${state.user.username}`); if (state.user.rol === 'CLIENTE') showView('catalogo'); else showView('admin'); } catch (error) { toast(error.message); } });
document.querySelectorAll('[data-demo]').forEach((button) => button.addEventListener('click', () => { const admin = button.dataset.demo === 'admin'; $('#username').value = admin ? 'admin' : 'cliente'; $('#password').value = admin ? 'admin123' : 'cliente123'; }));
document.querySelectorAll('.nav-link').forEach((button) => button.addEventListener('click', () => { if (button.dataset.view === 'carrito' && !requireLogin()) return; showView(button.dataset.view); }));
$('#login-open').addEventListener('click', openLogin); $('#login-close').addEventListener('click', closeLogin); $('#logout-button').addEventListener('click', async () => { await api('/api/auth/logout', { method: 'POST' }); state.user = null; state.cart = null; updateSessionUI(); showView('catalogo'); toast('Sesión cerrada'); });
$('#search-input').addEventListener('input', renderProducts); $('#category-filter').addEventListener('change', renderProducts); $('#cancel-edit').addEventListener('click', resetProductForm); $('[data-scroll-products]').addEventListener('click', () => $('#products-anchor').scrollIntoView({ behavior: 'smooth' }));
loadSession().then(loadProducts);