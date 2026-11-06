package ar.com.gaslogix.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ar.com.gaslogix.model.DetalleCarga;
import ar.com.gaslogix.model.Orden;

@Repository
public interface DetalleCargaRepository extends JpaRepository<DetalleCarga, Long> {

	// SELECT ... FROM detalle_carga WHERE id_orden = ? ORDER BY fecha DESC LIMIT 1  (el último detalle guardado)
	Optional<DetalleCarga> findTopByOrdenOrderByFechaDesc(Orden orden);

	// Promedios para la conciliación: el AVG lo calcula MySQL. JPQL devuelve AVG como Double (null si no hay filas)
	@Query("SELECT AVG(d.temperatura) FROM DetalleCarga d WHERE d.orden = :orden")
	Double promedioTemperatura(@Param("orden") Orden orden);

	@Query("SELECT AVG(d.densidad) FROM DetalleCarga d WHERE d.orden = :orden")
	Double promedioDensidad(@Param("orden") Orden orden);

	@Query("SELECT AVG(d.caudal) FROM DetalleCarga d WHERE d.orden = :orden")
	Double promedioCaudal(@Param("orden") Orden orden);
}
