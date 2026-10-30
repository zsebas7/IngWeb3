package ar.com.gaslogix.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.gaslogix.model.Camion;

@Repository
public interface CamionRepository extends JpaRepository<Camion, Long> {

	// SELECT ... FROM camion WHERE codigo_externo = ?
	Optional<Camion> findByCodigoExterno(String codigoExterno);

	// SELECT ... FROM camion WHERE patente = ?  (identificador alternativo; la patente es única)
	Optional<Camion> findByPatente(String patente);
}
