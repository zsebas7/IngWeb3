package ar.com.gaslogix.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.gaslogix.model.Chofer;

@Repository
public interface ChoferRepository extends JpaRepository<Chofer, Long> {

	// SELECT ... FROM chofer WHERE codigo_externo = ?
	Optional<Chofer> findByCodigoExterno(String codigoExterno);

	// SELECT ... FROM chofer WHERE documento = ?  (identificador alternativo; el documento es único)
	Optional<Chofer> findByDocumento(String documento);
}
