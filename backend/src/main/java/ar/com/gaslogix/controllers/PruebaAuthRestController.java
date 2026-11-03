package ar.com.gaslogix.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.gaslogix.auth.Usuario;

/**
 * TEMPORAL: solo para probar roles (403) y getUserLogged().
 */
@RestController
@RequestMapping(Constants.URL_BASE + "/prueba-auth")
public class PruebaAuthRestController extends BaseRestController {

	// Cualquier usuario autenticado: devuelve quién es según el token
	@GetMapping("/yo")
	public ResponseEntity<?> yo() {
		Usuario u = getUserLogged();
		return new ResponseEntity<>(Map.of("id", u.getId(), "username", u.getUsername(), "roles", u.getRolesNombres()),
				HttpStatus.OK);
	}

	// Solo ROLE_ADMIN: un operador debe recibir 403
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/solo-admin")
	public ResponseEntity<?> soloAdmin() {
		return new ResponseEntity<>(Map.of("mensaje", "Acceso permitido para " + getUserLogged().getUsername()),
				HttpStatus.OK);
	}
}
