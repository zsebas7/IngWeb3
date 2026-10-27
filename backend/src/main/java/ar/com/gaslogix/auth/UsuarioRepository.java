package ar.com.gaslogix.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	// Spring Data genera la consulta a partir del nombre: SELECT ... FROM usuario WHERE username = ?
	Optional<Usuario> findByUsername(String username);
}
