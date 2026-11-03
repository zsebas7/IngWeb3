package ar.com.gaslogix.business.exception;

import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * La contraseña de activación de la orden no coincide. Se responde con 401.
 */
@NoArgsConstructor
public class BadPasswordException extends Exception {

	private static final long serialVersionUID = 1L;

	@Builder
	public BadPasswordException(String message, Throwable ex) {
		super(message, ex);
	}
}
