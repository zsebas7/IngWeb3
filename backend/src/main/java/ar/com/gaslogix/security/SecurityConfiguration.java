package ar.com.gaslogix.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ar.com.gaslogix.auth.IUsuarioBusiness;
import ar.com.gaslogix.auth.JwtService;
import ar.com.gaslogix.auth.custom.CustomAuthenticationManager;
import ar.com.gaslogix.auth.filters.JWTAuthorizationFilter;
import ar.com.gaslogix.controllers.Constants;

/**
 * Configuración central de seguridad: qué rutas son públicas, cómo se autentica cada request (JWT) y
 * cómo se responden los errores 401/403.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize("hasRole('...')") en los controllers
public class SecurityConfiguration {

	@Autowired
	private IUsuarioBusiness usuarioBusiness;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JsonSecurityErrorHandler errorHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager() {
		return new CustomAuthenticationManager(passwordEncoder(), usuarioBusiness);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// API REST con tokens: no se usan cookies de sesión, así que CSRF no aplica
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// Sin login por formulario ni Basic: la única forma de autenticarse es el token
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, Constants.URL_LOGIN).permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(e -> e
						.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler))
				.addFilterBefore(new JWTAuthorizationFilter(jwtService, usuarioBusiness),
						UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
