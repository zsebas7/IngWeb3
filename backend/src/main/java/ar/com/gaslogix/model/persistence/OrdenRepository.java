package ar.com.gaslogix.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.gaslogix.model.Orden;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {

	// SELECT ... FROM orden WHERE numero_orden = ?  (los endpoints buscan la orden por el número de SAP)
	Optional<Orden> findByNumeroOrden(Long numeroOrden);

	// SELECT ... FROM orden WHERE codigo_externo = ?
	Optional<Orden> findByCodigoExterno(String codigoExterno);
}
