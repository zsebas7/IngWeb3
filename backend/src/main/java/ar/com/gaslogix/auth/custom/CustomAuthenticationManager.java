package ar.com.gaslogix.auth.custom;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.com.gaslogix.auth.IUsuarioBusiness;
import ar.com.gaslogix.auth.Usuario;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.NotFoundException;

/**
 * Valida usuario y contraseña contra la BD. Se registra como bean en SecurityConfiguration.
 */
public class CustomAuthenticationManager implements AuthenticationManager {

	// Mismo mensaje si no existe el usuario o si la contraseña es incorrecta, para no revelar qué usuarios existen
	private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos";

	private final PasswordEncoder passwordEncoder;
	private final IUsuarioBusiness usuarioBusiness;

	public CustomAuthenticationManager(PasswordEncoder passwordEncoder, IUsuarioBusiness usuarioBusiness) {
		this.passwordEncoder = passwordEncoder;
		this.usuarioBusiness = usuarioBusiness;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();
		String password = authentication.getCredentials().toString();

		Usuario usuario;
		try {
			usuario = usuarioBusiness.load(username);
		} catch (NotFoundException e) {
			throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
		} catch (BusinessException e) {
			throw new AuthenticationServiceException(e.getMessage(), e);
		}

		// Compara la contraseña ingresada con el hash BCrypt guardado
		if (!passwordEncoder.matches(password, usuario.getPassword())) {
			throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
		}

		// El estado de la cuenta se informa solo a quien ya demostró conocer la contraseña
		if (!usuario.isEnabled()) {
			throw new DisabledException("El usuario está deshabilitado");
		}
		if (!usuario.isAccountNonLocked()) {
			throw new LockedException("El usuario está bloqueado");
		}
		if (!usuario.isAccountNonExpired()) {
			throw new AccountExpiredException("La cuenta del usuario expiró");
		}
		if (!usuario.isCredentialsNonExpired()) {
			throw new CredentialsExpiredException("La contraseña del usuario expiró");
		}

		// Autenticación exitosa: el principal es el propio Usuario, con sus roles y sin la contraseña
		return new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
	}
}
