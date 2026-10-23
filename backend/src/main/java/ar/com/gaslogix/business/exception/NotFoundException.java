package ar.com.gaslogix.business.exception;

import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * La entidad buscada no existe (por ejemplo, una orden inexistente). Se responde con 404.
 */
@NoArgsConstructor
public class NotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	@Builder
	public NotFoundException(String message, Throwable ex) {
		super(message, ex);
	}
}
