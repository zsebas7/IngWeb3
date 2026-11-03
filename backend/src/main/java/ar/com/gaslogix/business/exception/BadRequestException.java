package ar.com.gaslogix.business.exception;

import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Los datos recibidos son inválidos o están incompletos. Se responde con 400.
 */
@NoArgsConstructor
public class BadRequestException extends Exception {

	private static final long serialVersionUID = 1L;

	@Builder
	public BadRequestException(String message, Throwable ex) {
		super(message, ex);
	}
}
