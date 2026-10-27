package ar.com.gaslogix.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import ar.com.gaslogix.auth.filters.JWTAuthorizationFilter;
import ar.com.gaslogix.util.IStandartResponseBusiness;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Responde los errores de Spring Security con el mismo formato JSON que el resto de la API (StandartResponse)
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	@Autowired
	private IStandartResponseBusiness response;

	@Autowired
	private ObjectMapper objectMapper;

	// 401: no hay usuario autenticado (sin token, token inválido o vencido)
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse httpResponse,
			AuthenticationException ex) throws IOException {
		Object errorToken = request.getAttribute(JWTAuthorizationFilter.ATTR_ERROR_TOKEN);
		String mensaje = errorToken != null ? errorToken.toString()
				: "Se requiere iniciar sesión: falta el token en el header Authorization";
		escribir(httpResponse, HttpStatus.UNAUTHORIZED, ex, mensaje);
	}

	// 403: el usuario está autenticado pero su rol no le permite usar el recurso
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse httpResponse, AccessDeniedException ex)
			throws IOException {
		escribir(httpResponse, HttpStatus.FORBIDDEN, ex, "No tiene permisos para acceder a este recurso");
	}

	private void escribir(HttpServletResponse httpResponse, HttpStatus status, Exception ex, String mensaje)
			throws IOException {
		httpResponse.setStatus(status.value());
		httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
		httpResponse.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(httpResponse.getWriter(), response.build(status, ex, mensaje));
	}
}
