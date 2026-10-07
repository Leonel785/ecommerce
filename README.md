# 🛒 Mini E-commerce

Sistema web de comercio electrónico académico desarrollado con **Spring Boot**. Permite a los clientes explorar un catálogo de productos, gestionar un carrito de compras y confirmar pedidos, mientras que un administrador gestiona el inventario y el estado de los pedidos.

## ✨ Características

- 🔐 Registro e inicio de sesión con manejo de sesión HTTP y roles (`CLIENTE` / `ADMIN`)
- 🛍️ Catálogo de productos con búsqueda por nombre y filtro por categoría
- 🛒 Carrito de compras con validación de stock en tiempo real
- 📦 Confirmación de pedidos con historial y seguimiento de estado (`PENDIENTE → PAGADO → ENVIADO → ENTREGADO`)
- 🖼️ Subida de imágenes de productos (admin) con validación de formato y tamaño
- 👨‍💼 Panel de administración para gestionar productos y pedidos
- ⚠️ Manejo centralizado de errores con respuestas JSON uniformes

## 🧰 Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | MySQL |
| Vistas | Thymeleaf |
| Validación | Spring Validation |
| Frontend | HTML, CSS, JavaScript |
| Build | Maven |

## 📁 Estructura del proyecto

```
com.ecommerce
├── controller     # Endpoints REST y vistas (Auth, Producto, Carrito, Pedido, View)
├── service        # Lógica de negocio
├── repository     # Acceso a datos (Spring Data JPA)
├── entity         # Entidades JPA (Usuario, Cliente, Producto, Carrito, Pedido, ...)
├── dto            # Objetos de transferencia de datos
├── exception       # Manejo centralizado de errores
├── util            # SessionGuard (control de acceso por sesión)
└── config          # DataInitializer (datos de prueba)
```

## 🗄️ Modelo de datos

El sistema define 7 tablas principales gestionadas por Hibernate: `usuario`, `cliente`, `producto`, `carrito`, `detalle_carrito`, `pedido` y `detalle_pedido`. El script de referencia se encuentra en [`database/schema.sql`](./database/schema.sql).

## 🚀 Cómo ejecutar el proyecto

### Requisitos previos

- Java 17+
- Maven 3.8+
- MySQL 8+

### 1. Clonar el repositorio

```bash
git clone https://github.com/Leonel785/ecommerce.git
cd ecommerce
```

### 2. Configurar variables de entorno (obligatorio)

Copia `.env.example` como `.env` (ya está en `.gitignore`; **nunca lo subas al repositorio**) y completa los valores. VS Code lo carga automáticamente mediante `.vscode/launch.json`.

Las claves de cifrado se generan **una sola vez** y deben conservarse: si se pierden, los datos cifrados no se pueden recuperar.

```bash
# Linux / macOS / Git Bash  (ejecutar dos veces: una para APP_CRYPTO_KEY y otra para APP_HMAC_KEY)
openssl rand -base64 32
```

```powershell
# Windows PowerShell (ejecutar dos veces)
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

| Variable | Descripción |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a MySQL (sin contraseña por defecto) |
| `APP_CRYPTO_KEY` | Clave AES-256 (32 bytes en Base64) para cifrar datos personales |
| `APP_HMAC_KEY` | Clave HMAC-SHA256 (32 bytes en Base64, distinta de la anterior) |
| `ADMIN_PASSWORD` | (Opcional) contraseña inicial del admin; si se omite se genera una y se muestra una vez en el log |
| `SEED_DEMO_USER` | (Opcional) `true` crea un cliente de demostración con contraseña aleatoria |
| `COOKIE_SECURE` | `true` cuando se sirva por HTTPS |

> **Base de datos existente:** ejecuta antes `database/migracion-cifrado.sql`. Al arrancar, los datos actuales se cifran solos y las contraseñas antiguas pasan a BCrypt en el siguiente login.

### 3. Ejecutar la aplicación

```bash
./mvnw spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`.

### 4. Usuario administrador

Ya no hay credenciales fijas. Al iniciar por primera vez se crea el usuario `admin` con la contraseña de `ADMIN_PASSWORD` o, si no se definió, con una contraseña aleatoria impresa **una sola vez** en la consola (`CREDENCIAL INICIAL`). Cámbiala/guárdala en un gestor de contraseñas.

## 🔒 Seguridad implementada

Ver el detalle completo en [`SEGURIDAD.md`](SEGURIDAD.md): contraseñas con BCrypt, cifrado AES-256-GCM de datos personales, CSRF, cabeceras HTTP de seguridad, límite de intentos de login, anti session-fixation, validación de entrada, subida de archivos por firma binaria y escape de HTML (XSS).

## 📡 API REST

Todas las rutas están bajo el prefijo `/api` y devuelven JSON.

<details>
<summary><strong>Autenticación — /api/auth</strong></summary>

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/api/auth/login` | Autentica y crea la sesión | Público |
| POST | `/api/auth/registro` | Registra un cliente e inicia sesión | Público |
| GET | `/api/auth/me` | Devuelve los datos del usuario en sesión | Público |
| POST | `/api/auth/logout` | Cierra la sesión actual | Sesión activa |

</details>

<details>
<summary><strong>Productos — /api/productos</strong></summary>

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| GET | `/api/productos` | Lista/busca productos (`buscar`, `categoria`) | Público |
| GET | `/api/productos/{id}` | Obtiene un producto por id | Público |
| POST | `/api/productos` | Crea un producto | ADMIN |
| PUT | `/api/productos/{id}` | Actualiza un producto | ADMIN |
| DELETE | `/api/productos/{id}` | Elimina un producto | ADMIN |
| POST | `/api/productos/upload` | Sube la imagen de un producto | ADMIN |

</details>

<details>
<summary><strong>Carrito — /api/carrito</strong></summary>

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| GET | `/api/carrito` | Obtiene (o crea) el carrito del cliente | CLIENTE |
| POST | `/api/carrito` | Agrega un producto al carrito | CLIENTE |
| PUT | `/api/carrito/detalle/{id}` | Actualiza la cantidad de un ítem | CLIENTE |
| DELETE | `/api/carrito/detalle/{id}` | Elimina un ítem del carrito | CLIENTE |
| DELETE | `/api/carrito` | Vacía el carrito completo | CLIENTE |

</details>

<details>
<summary><strong>Pedidos — /api/pedidos</strong></summary>

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/api/pedidos/confirmar` | Convierte el carrito en un pedido | CLIENTE |
| GET | `/api/pedidos` | Lista los pedidos del cliente autenticado | CLIENTE |
| GET | `/api/pedidos/{id}` | Obtiene un pedido por id | Dueño o ADMIN |
| GET | `/api/pedidos/admin` | Lista todos los pedidos del sistema | ADMIN |
| PUT | `/api/pedidos/admin/{id}/estado` | Cambia el estado de un pedido | ADMIN |

</details>

## 🖥️ Vistas

| Ruta | Descripción | Acceso |
|---|---|---|
| `/` `/index` `/productos` | Catálogo de productos | Público |
| `/login` | Inicio de sesión | Público |
| `/registro` | Registro de cliente | Público |
| `/carrito` | Carrito de compras | CLIENTE |
| `/mis-pedidos` | Historial de pedidos | CLIENTE |
| `/pedido/{id}` | Detalle de un pedido | Dueño o ADMIN |
| `/admin/productos` | Panel de administración | ADMIN |

## 👤 Autores

- Raymi Roman Quispe Ñahui
- Frank Joseph Pujaico Martinez
- Alex Kevin Conde Pariona
- Leonel Teófilo Campos Huamán

Estudiantes de Ingeniería de Sistemas de Información — Escuela de Educación Superior Tecnológica Privada La Pontificia, Ayacucho, Perú
