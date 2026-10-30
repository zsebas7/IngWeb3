package ar.com.gaslogix.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.gaslogix.model.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	// SELECT ... FROM cliente WHERE codigo_externo = ?
	Optional<Cliente> findByCodigoExterno(String codigoExterno);
}
