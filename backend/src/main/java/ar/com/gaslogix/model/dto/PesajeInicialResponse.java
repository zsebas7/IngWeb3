package ar.com.gaslogix.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta del pesaje inicial, con la contraseña de activación que habilita la carga.
 */
public record PesajeInicialResponse(Long numeroOrden, BigDecimal tara, LocalDateTime fechaPesajeInicial,
		Integer passwordActivacion) {
}
