package com.ecommerce.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Conversor JPA que cifra el atributo al escribir en la base de datos y lo descifra al leerlo.
 * Se aplica con {@code @Convert(converter = EncryptedStringConverter.class)}.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    private final CryptoService crypto;

    public EncryptedStringConverter(CryptoService crypto) {
        this.crypto = crypto;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return crypto.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return crypto.decrypt(dbData);
    }
}
