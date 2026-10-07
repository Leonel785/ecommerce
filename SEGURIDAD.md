# Informe de seguridad — mini-ecommerce

Revisión de código (SAST manual) del proyecto y medidas aplicadas. Clasificación según OWASP Top 10 2021.

## 1. Hallazgos iniciales

| # | Hallazgo | Severidad | OWASP |
|---|---|---|---|
| 1 | Contraseñas guardadas y comparadas en **texto plano** (`Usuario.password`, `authenticate`) | Crítica | A02 Fallas criptográficas |
| 2 | Credenciales por defecto conocidas: `admin/admin123`, `cliente/cliente123`, y `root/root` como contraseña de BD | Crítica | A07 Fallas de autenticación |
| 3 | Datos personales (nombre, correo, dirección, teléfono) en texto plano en la BD | Alta | A02 |
| 4 | Sin protección **CSRF** (no existía Spring Security) | Alta | A01 Control de acceso |
| 5 | Sin límite de intentos de login (fuerza bruta / credential stuffing) | Alta | A07 |
| 6 | **Path traversal** en el borrado de imágenes: `ProductoRequest.imagen` llegaba sin validar a `new File(dir, filename).delete()` | Alta | A01 / A03 |
| 7 | Subida de archivos validada solo por la extensión del nombre (falsificable); error 500 devolvía `e.getMessage()` | Media | A04 / A05 |
| 8 | **XSS**: datos dinámicos (`nombre`, `categoria`, `imagen`, `error.message`) insertados con `innerHTML` sin escapar | Media | A03 Inyección |
| 9 | Sin cabeceras de seguridad (CSP, X-Frame-Options, HSTS, nosniff, Referrer-Policy) | Media | A05 |
| 10 | Sin política de contraseñas ni límites de longitud en los DTO | Media | A07 |
| 11 | Sin rotación de ID de sesión al autenticar (session fixation); sin timeout explícito | Media | A07 |
| 12 | `.gitignore` sin `.env` (riesgo de subir secretos al repositorio) | Media | A05 |
| 13 | Login con tiempos distintos si el usuario existe o no (enumeración por timing) | Baja | A07 |

## 2. Medidas aplicadas

- **Hash de contraseñas**: BCrypt (factor 12). Las contraseñas heredadas en texto plano se migran a hash de forma transparente en el siguiente login. Se iguala el tiempo de respuesta cuando el usuario no existe.
- **Cifrado en reposo (AES-256-GCM)** de `nombres`, `correo`, `direccion` y `telefono` mediante un `AttributeConverter` de JPA (`EncryptedStringConverter` + `CryptoService`). IV aleatorio de 96 bits por valor y etiqueta de integridad GCM.
- **Índice ciego** (`correo_hash`, HMAC-SHA256 con otra clave) para validar la unicidad del correo sin guardarlo en claro.
- **Claves fuera del código**: `APP_CRYPTO_KEY` y `APP_HMAC_KEY` por variables de entorno; la aplicación no arranca si faltan o son inválidas.
- **Migración automática** de datos existentes (`EncryptionBackfill`) + script `database/migracion-cifrado.sql`.
- **Sin credenciales por defecto**: el admin se crea con `ADMIN_PASSWORD` o una contraseña aleatoria mostrada una vez; el cliente demo es opcional. Se elimina el `root` por defecto de la BD.
- **Autorización con Spring Security** (reemplaza a `SessionGuard`, que fue eliminado): la identidad (`AuthUser` + rol) vive en el `SecurityContext` y se persiste en la sesión; las rutas se protegen en `SecurityConfig` con política *denegar por defecto* (ADMIN, CLIENTE, autenticado, público) y, como segunda barrera, con `@PreAuthorize` en cada controlador. La propiedad de un pedido (el cliente solo ve los suyos) se sigue validando en `PedidoController`. Respuestas 401/403 en JSON para `/api/**` y redirección a `/login` (o a la página del rol) para las páginas.
- **CSRF** con token en cookie `XSRF-TOKEN` y cabecera `X-XSRF-TOKEN` (ya integrado en `common.js` y en la subida de imágenes).
- **Cabeceras**: CSP, `X-Frame-Options: DENY`, HSTS, `Referrer-Policy`, `X-Content-Type-Options: nosniff`.
- **Anti fuerza bruta**: 5 intentos fallidos por IP+usuario → bloqueo de 15 min (HTTP 429) y log de eventos de seguridad.
- **Sesión**: rotación del ID al iniciar sesión/registrarse, timeout de 30 min, cookie `HttpOnly`, `SameSite=Lax` y `Secure` configurable.
- **Validación de entrada**: política de contraseñas (10–72 caracteres, letras + números), formato de usuario y teléfono, longitudes máximas, rango de precio y stock, nombre de imagen restringido por regex.
- **Archivos**: tipo real detectado por firma binaria (JPG/PNG/WEBP), nombre generado con UUID, límite de 5 MB, sin filtrar errores internos, y borrado protegido contra path traversal.
- **XSS**: función `esc()` aplicada a todos los datos dinámicos insertados vía `innerHTML`.
- **Errores**: `server.error.include-*=never` para no exponer detalles internos.

## 3. Riesgos pendientes (recomendado para siguientes iteraciones)

1. **Pago simulado**: `PedidoService.confirm` marca el pedido como `PAGADO` sin pasarela de pago. Integrar un proveedor (Culqi, Niubiz, Stripe) y no almacenar datos de tarjeta (PCI-DSS).
2. **Condición de carrera en el stock**: dos compras simultáneas pueden vender stock inexistente. Usar bloqueo (`@Lock(PESSIMISTIC_WRITE)`) o `@Version`.
3. **CSP con `'unsafe-inline'`**: los templates usan scripts y `onclick` en línea. Moverlos a archivos `.js` y usar `addEventListener` permitiría una CSP estricta sin `unsafe-inline`.
4. **Rate limiting general y HTTPS**: solo el login tiene límite de intentos; falta limitar el resto de la API (p. ej. con Bucket4j) y servir la aplicación por HTTPS con `COOKIE_SECURE=true`.
5. **Límite de intentos en memoria**: válido para una instancia; con varias usar Redis. Detrás de un proxy configurar `server.forward-headers-strategy` para que la IP sea la real.
6. **Rotación de claves**: el prefijo `enc:v1:` permite añadir versiones; falta un procedimiento de rotación. En producción guardar las claves en un gestor (Vault, AWS KMS, Azure Key Vault).
7. **Base de datos**: usar un usuario con mínimos privilegios (no `root`), TLS en la conexión (`sslMode=REQUIRED`), `ddl-auto=validate` en producción y copias de seguridad cifradas.
8. **Imágenes subidas** se guardan dentro de `src/` y `target/`; en producción usar un directorio externo o almacenamiento de objetos.
9. `static/app.js` no está referenciado por ninguna plantilla (código heredado con los mismos patrones de `innerHTML`); eliminarlo o escaparlo si se reutiliza.
10. Añadir MFA para el administrador, política de bloqueo/recuperación de contraseña y dependencias auditadas (`mvn dependency-check:check`).

## 4. Cómo probar

- Sin `APP_CRYPTO_KEY` la app debe negarse a arrancar con un mensaje claro.
- Registrar un cliente y consultar la tabla: `SELECT nombres, correo, correo_hash FROM cliente;` → valores `enc:v1:...` y un hash hex.
- `SELECT username, password FROM usuario;` → hashes que empiezan por `$2a$12$`.
- 6 logins fallidos seguidos → HTTP 429.
- `POST` sin la cabecera `X-XSRF-TOKEN` → HTTP 403.
- Crear un producto con nombre `<img src=x onerror=alert(1)>` → se muestra como texto, no se ejecuta.
- Subir un `.php` renombrado a `.jpg` → rechazado por firma binaria.

### Matriz de autorización (verificación manual)

| Petición | Sin sesión | CLIENTE | ADMIN |
|---|---|---|---|
| `GET /api/productos` | 200 | 200 | 200 |
| `POST /api/productos` (con token CSRF) | 401 | 403 | 200/201 |
| `GET /api/carrito` | 401 | 200 | 403 |
| `POST /api/pedidos/confirmar` | 401 | 200 | 403 |
| `GET /api/pedidos/admin` | 401 | 403 | 200 |
| `GET /api/pedidos/{id}` de otro cliente | 401 | 403 | 200 |
| Página `/admin/productos` | redirige a `/login` | redirige a `/` | 200 |
| Página `/carrito` | redirige a `/login` | 200 | redirige a `/admin/productos` |
| Cualquier ruta no listada (p. ej. `/actuator`, `/app.js`) | redirige a `/login` | 403 (redirige a `/`) | 403 (redirige a `/admin/productos`) |
