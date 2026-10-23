package ar.com.gaslogix.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Formato único de las respuestas de error de la API: {"message", "code", "devInfo"}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StandartResponse {

	private String message;

	@JsonIgnore
	private Throwable ex;

	@JsonIgnore
	private HttpStatus httpStatus;

	// Si es true, devInfo incluye el stack trace (solo para desarrollo)
	@JsonIgnore
	private boolean devInfoEnabled;

	public int getCode() {
		return httpStatus.value();
	}

	public String getDevInfo() {
		if (!devInfoEnabled) {
			return null;
		}
		if (ex != null) {
			return ExceptionUtils.getStackTrace(ex);
		}
		return "No stack trace";
	}

	public String getMessage() {
		if (message != null) {
			return message;
		}
		if (ex != null) {
			return ex.getMessage();
		}
		return null;
	}
}
