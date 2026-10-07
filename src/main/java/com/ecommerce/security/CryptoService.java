package com.ecommerce.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Servicio de criptografía para proteger datos personales (PII) en reposo.
 * <ul>
 *   <li><b>Cifrado:</b> AES-256-GCM (confidencialidad + integridad) con un IV aleatorio
 *       de 96 bits por cada valor. Formato almacenado: {@code enc:v1:Base64(IV || cifrado+tag)}.</li>
 *   <li><b>Índice ciego:</b> HMAC-SHA256 con una clave distinta, para poder buscar/validar
 *       unicidad (p. ej. correo) sin guardar el dato en claro ni usar cifrado determinista.</li>
 * </ul>
 * Las claves se leen de variables de entorno (nunca van en el repositorio).
 */
@Component
public class CryptoService {
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec aesKey;
    private final SecretKeySpec hmacKey;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(@Value("${app.crypto.key:}") String encryptionKey,
                         @Value("${app.crypto.hmac-key:}") String hmacKeyValue) {
        byte[] aes = decodeKey(encryptionKey, "APP_CRYPTO_KEY");
        byte[] hmac = decodeKey(hmacKeyValue, "APP_HMAC_KEY");
        if (MessageDigest.isEqual(aes, hmac)) {
            throw new IllegalStateException("APP_CRYPTO_KEY y APP_HMAC_KEY deben ser claves distintas.");
        }
        this.aesKey = new SecretKeySpec(aes, "AES");
        this.hmacKey = new SecretKeySpec(hmac, "HmacSHA256");
    }

    private static byte[] decodeKey(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + envName
                    + ". Genera una clave de 32 bytes en Base64 (ver README, sección Seguridad).");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(envName + " no es un Base64 válido.");
        }
        if (key.length != 32) {
            throw new IllegalStateException(envName + " debe decodificar a exactamente 32 bytes (256 bits).");
        }
        return key;
    }

    /** Cifra un texto con AES-256-GCM. Devuelve {@code null} si la entrada es {@code null}. */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv).put(cipherText);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el dato", e);
        }
    }

    /**
     * Descifra un valor producido por {@link #encrypt(String)}. Si el valor no tiene el prefijo
     * {@code enc:v1:} se asume dato heredado en texto plano y se devuelve tal cual (migración gradual).
     */
    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return stored;
        try {
            byte[] data = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (data.length <= IV_LENGTH) {
                throw new IllegalArgumentException("formato inválido");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, data, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar el dato (¿clave incorrecta o dato alterado?)", e);
        }
    }

    /** Índice ciego (HMAC-SHA256 en hex) del valor normalizado (trim + minúsculas). */
    public String blindIndex(String value) {
        if (value == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] digest = mac.doFinal(value.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el índice ciego", e);
        }
    }
}
