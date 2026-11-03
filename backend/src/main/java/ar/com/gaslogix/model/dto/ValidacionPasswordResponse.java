package ar.com.gaslogix.model.dto;

import java.math.BigDecimal;

/**
 * Respuesta de la validación: {"preset": 10000}, los kg a cargar.
 */
public record ValidacionPasswordResponse(BigDecimal preset) {
}
