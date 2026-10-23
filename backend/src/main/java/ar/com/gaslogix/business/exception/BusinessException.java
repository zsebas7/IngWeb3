package ar.com.gaslogix.business.exception;

import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Error inesperado al ejecutar la lógica de negocio o acceder a la BD. Se responde con 500.
 */
@NoArgsConstructor
public class BusinessException extends Exception {

	private static final long serialVersionUID = 1L;

	@Builder
	public BusinessException(String message, Throwable ex) {
		super(message, ex);
	}
}
