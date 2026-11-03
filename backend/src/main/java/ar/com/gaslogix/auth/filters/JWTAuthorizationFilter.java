package ar.com.gaslogix.auth.filters;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;

import ar.com.gaslogix.auth.IUsuarioBusiness;
import ar.com.gaslogix.auth.JwtService;
import ar.com.gaslogix.auth.Usuario;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.NotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class JWTAuthorizationFilter extends OncePerRequestFilter {

	public static final String TOKEN_PREFIX = "Bearer ";

	// Atributo del request donde se deja el motivo del rechazo, para incluirlo en la respuesta 401
	public static final String ATTR_ERROR_TOKEN = "errorToken";

	private final JwtService jwtService;
	private final IUsuarioBusiness usuarioBusiness;

	public JWTAuthorizationFilter(JwtService jwtService, IUsuarioBusiness usuarioBusiness) {
		this.jwtService = jwtService;
		this.usuarioBusiness = usuarioBusiness;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(TOKEN_PREFIX)) {
			// Sin token: puede ser una ruta pública (login, Swagger)
			chain.doFilter(request, response);
			return;
		}

		String token = header.substring(TOKEN_PREFIX.length());
		try {
			String username = jwtService.validarToken(token);
			Usuario usuario = usuarioBusiness.load(username);
			if (usuario.isEnabled() && usuario.isAccountNonLocked()) {
				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(usuario, null,
						usuario.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(auth);
			} else {
				request.setAttribute(ATTR_ERROR_TOKEN, "El usuario del token está deshabilitado o bloqueado");
			}
		} catch (TokenExpiredException e) {
			request.setAttribute(ATTR_ERROR_TOKEN, "El token venció, hay que volver a iniciar sesión");
		} catch (JWTVerificationException e) {
			request.setAttribute(ATTR_ERROR_TOKEN, "El token es inválido");
		} catch (NotFoundException e) {
			request.setAttribute(ATTR_ERROR_TOKEN, "El usuario del token ya no existe");
		} catch (BusinessException e) {
			log.error(e.getMessage(), e);
			request.setAttribute(ATTR_ERROR_TOKEN, "No se pudo validar el token");
		}

		chain.doFilter(request, response);
	}
}
