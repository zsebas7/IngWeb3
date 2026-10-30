package ar.com.gaslogix.model.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.gaslogix.model.OrdenEstadoLog;

@Repository
public interface OrdenEstadoLogRepository extends JpaRepository<OrdenEstadoLog, Long> {
}
