package com.ecommerce.security;

import com.ecommerce.entity.Rol;
import java.io.Serializable;

/**
 * Identidad del usuario autenticado. Es el "principal" que Spring Security guarda en el
 * {@code SecurityContext} (y este, a su vez, en la sesión HTTP). Se inyecta en los
 * controladores con {@code @AuthenticationPrincipal AuthUser user}.
 * Es serializable para que la sesión pueda persistirse o replicarse.
 */
public record AuthUser(Long id, String username, Rol rol) implements Serializable {
}
