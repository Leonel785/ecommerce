package com.ecommerce.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Limitador de intentos de inicio de sesión (mitigación de fuerza bruta y credential stuffing).
 * Tras {@value #MAX_ATTEMPTS} fallos consecutivos para una combinación IP + usuario, se bloquea
 * esa combinación durante 15 minutos. Es en memoria: suficiente para una sola instancia.
 * En producción con varias instancias conviene usar Redis o similar.
 */
@Component
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED = 10_000;

    private record Entry(int failures, Instant firstFailure) {
        boolean expired() {
            return Instant.now().isAfter(firstFailure.plus(WINDOW));
        }
    }

    private final Map<String, Entry> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Entry entry = attempts.get(key);
        if (entry == null) return false;
        if (entry.expired()) {
            attempts.remove(key);
            return false;
        }
        return entry.failures() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String key) {
        if (attempts.size() > MAX_TRACKED) {
            attempts.entrySet().removeIf(e -> e.getValue().expired());
        }
        attempts.merge(key, new Entry(1, Instant.now()),
                (old, ignored) -> old.expired()
                        ? new Entry(1, Instant.now())
                        : new Entry(old.failures() + 1, old.firstFailure()));
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
