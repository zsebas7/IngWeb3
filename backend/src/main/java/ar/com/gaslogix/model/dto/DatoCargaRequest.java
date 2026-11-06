package ar.com.gaslogix.model.dto;

import java.math.BigDecimal;

/**
 * Lectura del caudalímetro: {"masaAcumulada": 1500.5, "densidad": 0.5432, "temperatura": 18.3, "caudal": 3200}.
 * Masa en kg, temperatura en °C y caudal en kg/h.
 */
public record DatoCargaRequest(BigDecimal masaAcumulada, BigDecimal densidad, BigDecimal temperatura,
		BigDecimal caudal) {
}
