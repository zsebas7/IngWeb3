package ar.com.gaslogix.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta del cierre de la orden: el estado nuevo y cuánto se cargó según el caudalímetro.
 */
public record CierreOrdenResponse(Long numeroOrden, int estado, BigDecimal ultimaMasaAcumulada,
		LocalDateTime fechaFinCarga) {
}
