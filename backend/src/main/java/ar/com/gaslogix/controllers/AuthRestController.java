package ar.com.gaslogix.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.com.gaslogix.auth.JwtService;
import ar.com.gaslogix.auth.LoginRequest;
import ar.com.gaslogix.auth.LoginResponse;
import ar.com.gaslogix.auth.Usuario;
import ar.com.gaslogix.util.IStandartResponseBusiness;
import ar.com.gaslogix.util.StandartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Autenticación", description = "Login de usuarios y emisión del token JWT")
public class AuthRestController extends BaseRestController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private IStandartResponseBusiness response;

	@Operation(operationId = "login", summary = "Inicia sesión y devuelve un token JWT válido por 8 horas.")
	@SecurityRequirements // endpoint público: no requiere token
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Credenciales correctas.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)) }),
			@ApiResponse(responseCode = "400", description = "Falta el usuario o la contraseña.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "401", description = "Credenciales incorrectas o usuario deshabilitado.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }) })
	@PostMapping(value = Constants.URL_LOGIN, consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> login(@RequestBody LoginRequest login) {
		if (login.username() == null || login.username().isBlank() || login.password() == null
				|| login.password().isBlank()) {
			return new ResponseEntity<>(response.build(HttpStatus.BAD_REQUEST, null, "Faltan el usuario o la contraseña"),
					HttpStatus.BAD_REQUEST);
		}
		try {
			Authentication auth = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(login.username(), login.password()));
			Usuario usuario = (Usuario) auth.getPrincipal();
			String token = jwtService.generarToken(usuario);
			return new ResponseEntity<>(new LoginResponse(token, usuario.getUsername(), usuario.getRolesNombres()),
					HttpStatus.OK);
		} catch (AuthenticationServiceException e) {
			// Falla técnica al buscar el usuario (por ejemplo, BD caída)
			return new ResponseEntity<>(response.build(HttpStatus.INTERNAL_SERVER_ERROR, e, e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (AuthenticationException e) {
			// Credenciales incorrectas o cuenta deshabilitada, bloqueada o vencida
			return new ResponseEntity<>(response.build(HttpStatus.UNAUTHORIZED, e, e.getMessage()),
					HttpStatus.UNAUTHORIZED);
		}
	}
}
