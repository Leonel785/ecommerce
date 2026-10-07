-- Ejecutar UNA vez, ANTES de iniciar la versión con cifrado, si ya tienes datos en la tabla `cliente`.
-- (En una base de datos nueva no hace falta: Hibernate crea las columnas con el tamaño correcto.)
-- Motivo: el texto cifrado en Base64 es más largo que el dato original.
USE ecommerce;

ALTER TABLE cliente
    MODIFY nombres   VARCHAR(768)  NOT NULL,
    MODIFY correo    VARCHAR(768)  NOT NULL,
    MODIFY direccion VARCHAR(1024) NULL,
    MODIFY telefono  VARCHAR(256)  NULL;

-- Al iniciar la aplicación, EncryptionBackfill cifra automáticamente los datos existentes
-- y calcula `correo_hash`. Las contraseñas en texto plano se convierten a BCrypt en el
-- primer inicio de sesión de cada usuario.
