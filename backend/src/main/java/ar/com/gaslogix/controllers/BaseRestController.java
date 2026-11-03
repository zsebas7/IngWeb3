package ar.com.gaslogix.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.com.gaslogix.auth.Usuario;

/**
 * Clase base de los controllers. Da acceso al usuario logueado, que el filtro JWT dejó en el SecurityContext.
 */
public class BaseRestController {

	protected Usuario getUserLogged() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (Usuario) auth.getPrincipal();
	}
}
