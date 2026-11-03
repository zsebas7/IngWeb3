package ar.com.gaslogix.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta a una lectura aceptada: los últimos valores que quedaron en la orden y si la lectura además se guardó
 * en el detalle de carga (depende de la frecuencia de almacenamiento).
 */
public record DatoCargaResponse(Long numeroOrden, BigDecimal masaAcumulada, BigDecimal densidad,
		BigDecimal temperatura, BigDecimal caudal, LocalDateTime fechaUltimoDato, boolean guardadoEnDetalle) {
}
