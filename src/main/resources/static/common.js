/**
 * Estado global compartido de la sesión de usuario y el carrito.
 */
const state = { user: null, cart: null };

/**
 * Atajo utilitario para seleccionar un elemento del DOM por su selector CSS.
 * @param {string} selector selector CSS.
 * @returns {Element|null} elemento encontrado.
 */
const $ = (selector) => document.querySelector(selector);

/**
 * Formatea un valor numérico como moneda en Soles Peruanos (PEN).
 * @param {number} value valor a formar.
 * @returns {string} cadena formateada en moneda.
 */
const money = (value) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value || 0);

/**
 * Escapa texto para insertarlo de forma segura en HTML (previene XSS).
 * Usar SIEMPRE en datos dinámicos que se inyecten con innerHTML / plantillas.
 * @param {*} value valor a escapar.
 * @returns {string} texto con &, <, >, " y ' escapados.
 */
const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
));

/**
 * Lee el token CSRF que el servidor entrega en la cookie XSRF-TOKEN.
 * @returns {string} token CSRF (vacío si aún no existe).
 */
const csrfToken = () => {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : '';
};

/**
 * Envoltorio para realizar peticiones HTTP Fetch a la API REST del backend con manejo automático de JSON y errores.
 * Adjunta el token CSRF en la cabecera X-XSRF-TOKEN en todas las peticiones.
 * @param {string} url endpoint de la API.
 * @param {Object} options opciones adicionales de la petición fetch.
 * @returns {Promise<any>} datos parseados del cuerpo de la respuesta.
 */
const api = async (url, options = {}) => {
  const response = await fetch(url, {
    ...options,
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken(), ...(options.headers || {}) }
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(data?.message || 'No se pudo completar la operación');
  return data;
};

/**
 * Muestra una notificación emergente (toast) en la interfaz.
 * @param {string} message mensaje a desplegar.
 */
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

/**
 * Consulta la sesión activa del usuario mediante GET `/api/auth/me` y actualiza los elementos UI globales.
 */
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

/**
 * Actualiza la visibilidad de los elementos de navegación y etiquetas de sesión según el estado del usuario.
 */
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

/**
 * Obtiene el carrito actual del cliente e incrementa la insignia con la suma de unidades.
 */
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

// Inicialización de sesión y evento de cierre de sesión al cargar el DOM
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
