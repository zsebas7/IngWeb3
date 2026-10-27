package ar.com.gaslogix.auth;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario del sistema (tabla usuario). Implementa UserDetails para que Spring Security
 * pueda autenticarlo y conocer sus roles.
 */
@Entity
@Table(name = "usuario")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Usuario implements UserDetails {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 100, nullable = false, unique = true)
	private String username;

	@Column(length = 255, nullable = false, unique = true)
	private String email;

	// Hash BCrypt; nunca se envía en las respuestas JSON
	@JsonIgnore
	@Column(nullable = false)
	private String password;

	// Estado de la cuenta: Lombok genera isEnabled(), isAccountNonExpired(), etc., que pide UserDetails
	private boolean enabled = true;
	private boolean accountNonExpired = true;
	private boolean accountNonLocked = true;
	private boolean credentialsNonExpired = true;

	// EAGER: los roles se necesitan en cada request autenticado
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "usuario_rol",
			joinColumns = @JoinColumn(name = "id_usuario"),
			inverseJoinColumns = @JoinColumn(name = "id_rol"))
	private Set<Rol> roles;

	/**
	 * Roles del usuario en el formato que usa Spring Security para autorizar (hasRole / @PreAuthorize).
	 */
	@JsonIgnore
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roles.stream().map(rol -> new SimpleGrantedAuthority(rol.getNombre())).toList();
	}

	/**
	 * Nombres de los roles (ej.: ROLE_OPERADOR), para incluirlos en el token y en la respuesta del login.
	 */
	public List<String> getRolesNombres() {
		return roles.stream().map(Rol::getNombre).toList();
	}
}
