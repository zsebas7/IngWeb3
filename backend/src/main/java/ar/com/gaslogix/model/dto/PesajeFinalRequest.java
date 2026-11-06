package ar.com.gaslogix.model.dto;

import java.math.BigDecimal;

/**
 * Cuerpo del pesaje final (peso del camión cargado, en kg): {"pesajeFinal": 18500}.
 */
public record PesajeFinalRequest(BigDecimal pesajeFinal) {
}
