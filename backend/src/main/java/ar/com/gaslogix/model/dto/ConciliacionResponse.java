package ar.com.gaslogix.model.dto;

import java.math.BigDecimal;

/**
 * Conciliación de una orden finalizada (no se guarda en la BD, se calcula cada vez). Masas en kg, temperatura en °C
 * y caudal en kg/h.
 * - netoPorBalanza = pesajeFinal - pesajeInicial
 * - diferenciaBalanzaCaudalimetro = netoPorBalanza - productoCargado (puede ser negativa)
 * - los promedios se calculan sobre detalle_carga
 */
public record ConciliacionResponse(Long numeroOrden, BigDecimal pesajeInicial, BigDecimal pesajeFinal,
		BigDecimal productoCargado, BigDecimal netoPorBalanza, BigDecimal diferenciaBalanzaCaudalimetro,
		BigDecimal promedioTemperatura, BigDecimal promedioDensidad, BigDecimal promedioCaudal) {
}
