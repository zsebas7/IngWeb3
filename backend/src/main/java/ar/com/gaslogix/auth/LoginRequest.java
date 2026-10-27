package ar.com.gaslogix.auth;

/**
 * Cuerpo del request de login: {"username": "...", "password": "..."}.
 */
public record LoginRequest(String username, String password) {
}
