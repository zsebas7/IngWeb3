package ar.com.gaslogix.auth;

import java.util.List;

/**
 * Respuesta de un login exitoso: el token y los datos básicos del usuario.
 */
public record LoginResponse(String token, String username, List<String> roles) {
}
