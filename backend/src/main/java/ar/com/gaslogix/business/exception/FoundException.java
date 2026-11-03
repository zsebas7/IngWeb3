package ar.com.gaslogix.business.exception;

import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * La entidad que se intenta crear ya existe (por ejemplo, una orden duplicada). Se responde con 409.
 */
@NoArgsConstructor
public class FoundException extends Exception {

	private static final long serialVersionUID = 1L;

	@Builder
	public FoundException(String message, Throwable ex) {
		super(message, ex);
	}
}
