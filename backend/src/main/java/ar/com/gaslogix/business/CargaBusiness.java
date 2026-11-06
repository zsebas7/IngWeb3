package ar.com.gaslogix.business;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.gaslogix.business.exception.BadRequestException;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.InvalidStateException;
import ar.com.gaslogix.business.exception.NotFoundException;
import ar.com.gaslogix.controllers.Constants;
import ar.com.gaslogix.model.DetalleCarga;
import ar.com.gaslogix.model.Orden;
import ar.com.gaslogix.model.dto.DatoCargaRequest;
import ar.com.gaslogix.model.dto.DatoCargaResponse;
import ar.com.gaslogix.model.persistence.DetalleCargaRepository;
import ar.com.gaslogix.model.persistence.OrdenRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CargaBusiness implements ICargaBusiness {

	@Autowired
	private IOrdenBusiness ordenBusiness;

	@Autowired
	private OrdenRepository ordenDAO;

	@Autowired
	private DetalleCargaRepository detalleCargaDAO;

	// Cada cuántos segundos se guarda una lectura en detalle_carga (application.properties)
	@Value("${carga.detalle.frecuencia-segundos:10}")
	private long frecuenciaSegundos;

	/**
	 * Orden de las validaciones: datos (400) → orden inexistente (404) → estado (409) → masa menor a la última (400).
	 * No cambia el estado de la orden, así que no escribe en orden_estado_log.
	 * Es transaccional porque puede escribir en dos tablas (orden y detalle_carga): o se guardan las dos o ninguna.
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public DatoCargaResponse recibirDatoCarga(Long numeroOrden, DatoCargaRequest dato)
			throws BadRequestException, NotFoundException, InvalidStateException, BusinessException {
		validarDato(dato);
		Orden orden = ordenBusiness.cargarPorNumeroOrden(numeroOrden);
		if (orden.getEstado() != Constants.ESTADO_CON_PESAJE_INICIAL) {
			throw InvalidStateException.builder().message("La orden número " + numeroOrden + " está en estado "
					+ orden.getEstado() + "; la recepción de datos de carga requiere estado 2").build();
		}
		// La masa acumulada nunca baja: una lectura menor a la última es un error del equipo y se descarta
		if (orden.getUltimaMasaAcumulada() != null
				&& dato.masaAcumulada().compareTo(orden.getUltimaMasaAcumulada()) < 0) {
			throw BadRequestException.builder().message("Lectura descartada: la masa acumulada ("
					+ dato.masaAcumulada() + ") es menor a la última recibida (" + orden.getUltimaMasaAcumulada() + ")")
					.build();
		}

		try {
			// Sin milisegundos: la columna DATETIME guarda segundos y así la comparación con la frecuencia es exacta
			LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

			// Siempre se sobrescriben los últimos valores de la orden
			orden.setUltimaMasaAcumulada(dato.masaAcumulada());
			orden.setUltimaDensidad(dato.densidad());
			orden.setUltimaTemperatura(dato.temperatura());
			orden.setUltimoCaudal(dato.caudal());
			orden.setFechaUltimoDato(ahora);
			// Inicio de carga = primer dato válido; fin de carga = último dato válido
			if (orden.getFechaInicioCarga() == null) {
				orden.setFechaInicioCarga(ahora);
			}
			orden.setFechaFinCarga(ahora);
			orden = ordenDAO.save(orden);

			boolean guardadoEnDetalle = false;
			if (correspondeGuardarDetalle(orden, ahora)) {
				guardarDetalle(orden, dato, ahora);
				guardadoEnDetalle = true;
			}

			return new DatoCargaResponse(orden.getNumeroOrden(), orden.getUltimaMasaAcumulada(),
					orden.getUltimaDensidad(), orden.getUltimaTemperatura(), orden.getUltimoCaudal(),
					orden.getFechaUltimoDato(), guardadoEnDetalle);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	// Los cuatro valores tienen que venir; caudal y masa además deben ser > 0. Densidad y temperatura no se validan
	private void validarDato(DatoCargaRequest dato) throws BadRequestException {
		if (dato == null || dato.masaAcumulada() == null || dato.densidad() == null || dato.temperatura() == null
				|| dato.caudal() == null) {
			throw BadRequestException.builder()
					.message("Faltan datos de la lectura: masaAcumulada, densidad, temperatura y caudal son obligatorios")
					.build();
		}
		if (dato.caudal().compareTo(BigDecimal.ZERO) <= 0) {
			throw BadRequestException.builder().message("Lectura descartada: el caudal debe ser mayor a 0").build();
		}
		if (dato.masaAcumulada().compareTo(BigDecimal.ZERO) <= 0) {
			throw BadRequestException.builder().message("Lectura descartada: la masa acumulada debe ser mayor a 0")
					.build();
		}
	}

	// La primera lectura siempre se guarda; las siguientes, solo si pasó la frecuencia desde el último detalle guardado
	private boolean correspondeGuardarDetalle(Orden orden, LocalDateTime ahora) {
		Optional<DetalleCarga> ultimo = detalleCargaDAO.findTopByOrdenOrderByFechaDesc(orden);
		if (ultimo.isEmpty()) {
			return true;
		}
		return Duration.between(ultimo.get().getFecha(), ahora).getSeconds() >= frecuenciaSegundos;
	}

	private void guardarDetalle(Orden orden, DatoCargaRequest dato, LocalDateTime fecha) {
		DetalleCarga detalle = new DetalleCarga();
		detalle.setOrden(orden);
		detalle.setMasaAcumulada(dato.masaAcumulada());
		detalle.setDensidad(dato.densidad());
		detalle.setTemperatura(dato.temperatura());
		detalle.setCaudal(dato.caudal());
		detalle.setFecha(fecha);
		detalleCargaDAO.save(detalle);
	}
}
