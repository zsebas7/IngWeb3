package ar.com.gaslogix.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.gaslogix.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

	// SELECT ... FROM producto WHERE codigo_externo = ?
	Optional<Producto> findByCodigoExterno(String codigoExterno);
}
