const state = { user: null, cart: null };
const $ = (selector) => document.querySelector(selector);
const money = (value) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value || 0);

const api = async (url, options = {}) => {
  const response = await fetch(url, { headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }, ...options });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(data?.message || 'No se pudo completar la operación');
  return data;
};

const toast = (message) => { 
  const node = $('#toast'); 
  if (node) {
    node.textContent = message; 
    node.classList.add('show'); 
    setTimeout(() => node.classList.remove('show'), 2800); 
  } else {
    alert(message);
  }
};

async function loadSession() {
  try {
    const session = await api('/api/auth/me');
    if (session.authenticated) {
      state.user = session;
      updateSessionUI();
      if (session.rol === 'CLIENTE') {
        await loadCartCount();
      }
    } else {
      state.user = null;
      updateSessionUI();
    }
  } catch (error) { 
    console.error(error); 
    state.user = null;
    updateSessionUI();
  }
}

function updateSessionUI() {
  const user = state.user;
  const label = $('#session-label');
  const loginBtn = $('#login-open');
  const logoutBtn = $('#logout-button');
  const ordersLnk = $('#orders-link');
  const adminLnk = $('#admin-link');
  
  if (label) label.textContent = user ? `${user.nombres || user.username} · ${user.rol}` : 'Visitante';
  if (loginBtn) loginBtn.classList.toggle('hidden', !!user);
  if (logoutBtn) logoutBtn.classList.toggle('hidden', !user);
  if (ordersLnk) ordersLnk.classList.toggle('hidden', user?.rol !== 'CLIENTE');
  if (adminLnk) adminLnk.classList.toggle('hidden', user?.rol !== 'ADMIN');
}

async function loadCartCount() {
  try {
    const cart = await api('/api/carrito');
    state.cart = cart;
    const countNode = $('#cart-count');
    if (countNode) {
      countNode.textContent = cart?.detalles?.reduce((total, detail) => total + detail.cantidad, 0) || 0;
    }
  } catch (error) {
    console.error('Error loading cart count:', error);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  loadSession();
  const logoutBtn = $('#logout-button');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', async () => {
      try {
        await api('/api/auth/logout', { method: 'POST' });
        window.location.href = '/login';
      } catch (error) {
        toast(error.message);
      }
    });
  }
});
