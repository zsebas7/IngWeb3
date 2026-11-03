package ar.com.gaslogix.business.exception;

import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * La orden no está en el estado que requiere la operación. Se responde con 409.
 */
@NoArgsConstructor
public class InvalidStateException extends Exception {

	private static final long serialVersionUID = 1L;

	@Builder
	public InvalidStateException(String message, Throwable ex) {
		super(message, ex);
	}
}
