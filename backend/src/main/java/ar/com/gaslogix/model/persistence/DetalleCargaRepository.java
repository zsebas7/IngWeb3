package ar.com.gaslogix.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.gaslogix.model.DetalleCarga;
import ar.com.gaslogix.model.Orden;

@Repository
public interface DetalleCargaRepository extends JpaRepository<DetalleCarga, Long> {

	// SELECT ... FROM detalle_carga WHERE id_orden = ? ORDER BY fecha DESC LIMIT 1  (el último detalle guardado)
	Optional<DetalleCarga> findTopByOrdenOrderByFechaDesc(Orden orden);
}
