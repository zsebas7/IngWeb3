package ar.com.gaslogix.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

/**
 * Genera y valida los tokens JWT. La clave y la duración se leen de application.properties.
 */
@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiracion-horas:8}")
	private long expiracionHoras;

	/**
	 * Crea un token firmado con HMAC-SHA256 que identifica al usuario (subject = username)
	 */
	public String generarToken(Usuario usuario) {
		Instant ahora = Instant.now();
		return JWT.create()
				.withSubject(usuario.getUsername())
				.withClaim("id", usuario.getId())
				.withClaim("roles", usuario.getRolesNombres())
				.withIssuedAt(ahora)
				.withExpiresAt(ahora.plus(expiracionHoras, ChronoUnit.HOURS))
				.sign(Algorithm.HMAC256(secret));
	}

	/**
	 * Verifica la firma y la expiración del token y devuelve el username que contiene.
	 *
	 * @throws JWTVerificationException si el token fue alterado, está mal formado o venció
	 */
	public String validarToken(String token) throws JWTVerificationException {
		return JWT.require(Algorithm.HMAC256(secret))
				.build()
				.verify(token)
				.getSubject();
	}
}
