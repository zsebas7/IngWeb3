package ar.com.gaslogix.model.dto;

import java.math.BigDecimal;

/**
 * Cuerpo del pesaje inicial: {"tara": 8500}.
 */
public record PesajeInicialRequest(BigDecimal tara) {
}
