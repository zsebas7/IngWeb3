package ar.com.gaslogix.business;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.gaslogix.auth.Usuario;
import ar.com.gaslogix.business.exception.BadPasswordException;
import ar.com.gaslogix.business.exception.BadRequestException;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.FoundException;
import ar.com.gaslogix.business.exception.InvalidStateException;
import ar.com.gaslogix.business.exception.NotFoundException;
import ar.com.gaslogix.controllers.Constants;
import ar.com.gaslogix.model.Camion;
import ar.com.gaslogix.model.Chofer;
import ar.com.gaslogix.model.Cisterna;
import ar.com.gaslogix.model.Cliente;
import ar.com.gaslogix.model.Orden;
import ar.com.gaslogix.model.OrdenEstadoLog;
import ar.com.gaslogix.model.Producto;
import ar.com.gaslogix.model.deserializer.OrdenJsonDeserializer;
import ar.com.gaslogix.model.dto.ConciliacionResponse;
import ar.com.gaslogix.model.persistence.CamionRepository;
import ar.com.gaslogix.model.persistence.ChoferRepository;
import ar.com.gaslogix.model.persistence.ClienteRepository;
import ar.com.gaslogix.model.persistence.DetalleCargaRepository;
import ar.com.gaslogix.model.persistence.OrdenEstadoLogRepository;
import ar.com.gaslogix.model.persistence.OrdenRepository;
import ar.com.gaslogix.model.persistence.ProductoRepository;
import ar.com.gaslogix.util.JsonUtiles;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class OrdenBusiness implements IOrdenBusiness {

	private static final SecureRandom RANDOM = new SecureRandom();

	@Autowired
	private OrdenRepository ordenDAO;

	@Autowired
	private OrdenEstadoLogRepository ordenEstadoLogDAO;

	@Autowired
	private CamionRepository camionDAO;

	@Autowired
	private ChoferRepository choferDAO;

	@Autowired
	private ClienteRepository clienteDAO;

	@Autowired
	private ProductoRepository productoDAO;

	@Autowired
	private DetalleCargaRepository detalleCargaDAO;

	/**
	 * Todo en una sola transacción: si falla algo (por ejemplo, el log), no queda ni la orden ni las entidades nuevas.
	 * rollbackFor = Exception.class porque nuestras excepciones son checked y, por defecto, Spring solo hace
	 * rollback con RuntimeException.
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Orden recibirOrden(String json, Usuario usuario) throws BadRequestException, FoundException, BusinessException {
		Orden orden = leerJson(json);
		validarOrden(orden);
		validarDuplicada(orden);

		try {
			orden.setCamion(obtenerOCrearCamion(orden.getCamion()));
			orden.setChofer(obtenerOCrearChofer(orden.getChofer()));
			orden.setCliente(obtenerOCrearCliente(orden.getCliente()));
			orden.setProducto(obtenerOCrearProducto(orden.getProducto()));

			LocalDateTime ahora = LocalDateTime.now();
			orden.setEstado(Constants.ESTADO_PENDIENTE_PESAJE_INICIAL);
			orden.setFechaRecepcion(ahora);
			orden = ordenDAO.save(orden);

			registrarCambioEstado(orden, null, Constants.ESTADO_PENDIENTE_PESAJE_INICIAL, ahora, usuario,
					"Orden recibida desde SAP");
			return orden;
		} catch (BadRequestException e) {
			throw e;
		} catch (FoundException e) {
			// Conflicto al sincronizar (patente o documento de otra entidad) → 409, no 500
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	@Override
	public Orden cargarPorNumeroOrden(Long numeroOrden) throws NotFoundException, BusinessException {
		Optional<Orden> r;
		try {
			r = ordenDAO.findByNumeroOrden(numeroOrden);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
		if (r.isEmpty()) {
			throw NotFoundException.builder().message("No se encuentra la orden número " + numeroOrden).build();
		}
		return r.get();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Orden registrarPesajeInicial(Long numeroOrden, BigDecimal tara, Usuario usuario)
			throws BadRequestException, NotFoundException, InvalidStateException, BusinessException {
		if (tara == null || tara.compareTo(BigDecimal.ZERO) <= 0) {
			throw BadRequestException.builder().message("Falta la tara o no es mayor a 0").build();
		}
		Orden orden = cargarPorNumeroOrden(numeroOrden);
		if (orden.getEstado() != Constants.ESTADO_PENDIENTE_PESAJE_INICIAL) {
			throw InvalidStateException.builder().message("La orden número " + numeroOrden + " está en estado "
					+ orden.getEstado() + "; el pesaje inicial requiere estado 1").build();
		}

		try {
			LocalDateTime ahora = LocalDateTime.now();
			orden.setTara(tara);
			orden.setFechaPesajeInicial(ahora);
			orden.setPasswordActivacion(generarPasswordActivacion());
			orden.setEstado(Constants.ESTADO_CON_PESAJE_INICIAL);
			orden = ordenDAO.save(orden);

			registrarCambioEstado(orden, Constants.ESTADO_PENDIENTE_PESAJE_INICIAL, Constants.ESTADO_CON_PESAJE_INICIAL,
					ahora, usuario, "Pesaje inicial registrado");
			return orden;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	// 5 dígitos (10000 a 99999). SecureRandom porque la contraseña habilita la carga: no tiene que ser predecible
	private int generarPasswordActivacion() {
		return RANDOM.nextInt(10000, 100000);
	}

	// Solo lectura: no cambia el estado ni escribe el log, por eso se puede repetir mientras siga en estado 2
	@Override
	public Orden validarPasswordActivacion(Long numeroOrden, Integer password) throws BadRequestException,
			NotFoundException, InvalidStateException, BadPasswordException, BusinessException {
		if (password == null) {
			throw BadRequestException.builder().message("Falta la contraseña de activación").build();
		}
		Orden orden = cargarPorNumeroOrden(numeroOrden);
		if (orden.getEstado() != Constants.ESTADO_CON_PESAJE_INICIAL) {
			throw InvalidStateException.builder().message("La orden número " + numeroOrden + " está en estado "
					+ orden.getEstado() + "; la validación de la contraseña requiere estado 2").build();
		}
		if (!password.equals(orden.getPasswordActivacion())) {
			throw BadPasswordException.builder().message("Contraseña de activación incorrecta").build();
		}
		return orden;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Orden cerrarOrden(Long numeroOrden, Usuario usuario)
			throws NotFoundException, InvalidStateException, BusinessException {
		Orden orden = cargarPorNumeroOrden(numeroOrden);
		if (orden.getEstado() != Constants.ESTADO_CON_PESAJE_INICIAL) {
			throw InvalidStateException.builder().message("La orden número " + numeroOrden + " está en estado "
					+ orden.getEstado() + "; el cierre requiere estado 2").build();
		}
		// La primera lectura válida fija la fecha de inicio de carga (ticket 07)
		if (orden.getFechaInicioCarga() == null) {
			throw InvalidStateException.builder().message("La orden número " + numeroOrden
					+ " no recibió datos de carga; no se puede cerrar").build();
		}

		try {
			orden.setEstado(Constants.ESTADO_CERRADA_PARA_CARGA);
			orden = ordenDAO.save(orden);

			registrarCambioEstado(orden, Constants.ESTADO_CON_PESAJE_INICIAL, Constants.ESTADO_CERRADA_PARA_CARGA,
					LocalDateTime.now(), usuario, "Orden cerrada para carga");
			return orden;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	/**
	 * Validaciones, en este orden: pesaje faltante o ≤ 0 (400) → orden inexistente (404) → estado ≠ 3 (409)
	 * → pesaje ≤ tara (400, necesita la orden). Pesaje, estado 4 y log 3 → 4 en una sola transacción.
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ConciliacionResponse registrarPesajeFinal(Long numeroOrden, BigDecimal pesajeFinal, Usuario usuario)
			throws BadRequestException, NotFoundException, InvalidStateException, BusinessException {
		if (pesajeFinal == null || pesajeFinal.compareTo(BigDecimal.ZERO) <= 0) {
			throw BadRequestException.builder().message("Falta el pesaje final o no es mayor a 0").build();
		}
		Orden orden = cargarPorNumeroOrden(numeroOrden);
		if (orden.getEstado() != Constants.ESTADO_CERRADA_PARA_CARGA) {
			throw InvalidStateException.builder().message("La orden número " + numeroOrden + " está en estado "
					+ orden.getEstado() + "; el pesaje final requiere estado 3").build();
		}
		// El camión cargado tiene que pesar más que vacío
		if (pesajeFinal.compareTo(orden.getTara()) <= 0) {
			throw BadRequestException.builder().message("El pesaje final (" + pesajeFinal
					+ ") debe ser mayor a la tara (" + orden.getTara() + ")").build();
		}

		try {
			LocalDateTime ahora = LocalDateTime.now();
			orden.setPesajeFinal(pesajeFinal);
			orden.setFechaPesajeFinal(ahora);
			orden.setEstado(Constants.ESTADO_FINALIZADA);
			orden = ordenDAO.save(orden);

			registrarCambioEstado(orden, Constants.ESTADO_CERRADA_PARA_CARGA, Constants.ESTADO_FINALIZADA, ahora,
					usuario, "Pesaje final registrado");
			return armarConciliacion(orden);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	// Solo lectura (sin @Transactional, como la validación de contraseña): se puede consultar las veces que haga falta
	@Override
	public ConciliacionResponse obtenerConciliacion(Long numeroOrden)
			throws NotFoundException, InvalidStateException, BusinessException {
		Orden orden = cargarPorNumeroOrden(numeroOrden);
		if (orden.getEstado() != Constants.ESTADO_FINALIZADA) {
			throw InvalidStateException.builder().message("La orden número " + numeroOrden + " está en estado "
					+ orden.getEstado() + "; la conciliación solo está disponible en estado 4").build();
		}
		try {
			return armarConciliacion(orden);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
	}

	// La conciliación no se guarda: se calcula con los datos de la orden y los promedios de detalle_carga
	private ConciliacionResponse armarConciliacion(Orden orden) {
		BigDecimal netoPorBalanza = orden.getPesajeFinal().subtract(orden.getTara());
		BigDecimal diferencia = netoPorBalanza.subtract(orden.getUltimaMasaAcumulada());
		return new ConciliacionResponse(orden.getNumeroOrden(), orden.getTara(), orden.getPesajeFinal(),
				orden.getUltimaMasaAcumulada(), netoPorBalanza, diferencia,
				redondear(detalleCargaDAO.promedioTemperatura(orden), 2),
				redondear(detalleCargaDAO.promedioDensidad(orden), 4),
				redondear(detalleCargaDAO.promedioCaudal(orden), 2));
	}

	// AVG devuelve Double: se pasa a BigDecimal con los mismos decimales que la columna. Toda orden cerrada tiene
	// al menos un detalle (la primera lectura siempre se guarda), pero se cubre el null por las dudas
	private BigDecimal redondear(Double promedio, int decimales) {
		if (promedio == null) {
			return null;
		}
		return BigDecimal.valueOf(promedio).setScale(decimales, RoundingMode.HALF_UP);
	}

	// JSON → Orden con el deserializador propio; cualquier error de formato es culpa del cliente → 400
	private Orden leerJson(String json) throws BadRequestException {
		if (json == null || json.isBlank()) {
			throw BadRequestException.builder().message("El cuerpo del request está vacío").build();
		}
		try {
			ObjectMapper mapper = JsonUtiles.getObjectMapper(Orden.class, new OrdenJsonDeserializer());
			return mapper.readValue(json, Orden.class);
		} catch (Exception e) {
			throw BadRequestException.builder().message("JSON de la orden inválido: " + e.getMessage()).ex(e).build();
		}
	}

	private void validarOrden(Orden orden) throws BadRequestException {
		if (orden.getNumeroOrden() == null) {
			throw BadRequestException.builder().message("Falta el número de orden (entero)").build();
		}
		if (orden.getPreset() == null || orden.getPreset().compareTo(BigDecimal.ZERO) <= 0) {
			throw BadRequestException.builder().message("Falta el preset o no es mayor a 0").build();
		}
		if (orden.getFechaCargaPrevista() == null) {
			throw BadRequestException.builder().message("Falta la fecha de carga prevista").build();
		}
		if (orden.getFechaCargaPrevista().isBefore(LocalDateTime.now())) {
			throw BadRequestException.builder().message("La fecha de carga prevista no puede ser pasada").build();
		}
		if (orden.getCamion() == null || orden.getChofer() == null || orden.getCliente() == null
				|| orden.getProducto() == null) {
			throw BadRequestException.builder().message("Faltan datos del camión, chofer, cliente o producto").build();
		}
	}

	// 409 si la orden ya fue recibida (mismo número, o mismo código externo cuando viene)
	private void validarDuplicada(Orden orden) throws FoundException, BusinessException {
		boolean existe;
		try {
			existe = ordenDAO.findByNumeroOrden(orden.getNumeroOrden()).isPresent()
					|| (orden.getCodigoExterno() != null
							&& ordenDAO.findByCodigoExterno(orden.getCodigoExterno()).isPresent());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
		if (existe) {
			throw FoundException.builder().message("Ya existe la orden número " + orden.getNumeroOrden()
					+ " o con código externo '" + orden.getCodigoExterno() + "'").build();
		}
	}

	private Camion obtenerOCrearCamion(Camion camion) throws BadRequestException, FoundException {
		// Un texto vacío se guarda NULL para no chocar con el UNIQUE
		if (camion.getPatente() != null && camion.getPatente().isBlank()) {
			camion.setPatente(null);
		}
		if (camion.getCodigoExterno() != null && camion.getCodigoExterno().isBlank()) {
			camion.setCodigoExterno(null);
		}
		if (camion.getPatente() == null && camion.getCodigoExterno() == null) {
			throw BadRequestException.builder().message("El camión necesita código externo o patente").build();
		}
		if (camion.getCisternas().isEmpty()) {
			throw BadRequestException.builder().message("El camión necesita al menos una cisterna").build();
		}
		for (Cisterna c : camion.getCisternas()) {
			if (c.getNombre() == null || c.getCapacidadLitros() == null) {
				throw BadRequestException.builder().message("Cada cisterna necesita nombre y capacidad en litros").build();
			}
		}

		// Sin patente no se busca: findByPatente(null) hace "patente IS NULL" y encontraría cualquier camión sin patente
		Optional<Camion> conPatente = camion.getPatente() != null
				? camionDAO.findByPatente(camion.getPatente())
				: Optional.empty();
		Optional<Camion> existente = camion.getCodigoExterno() != null
				? camionDAO.findByCodigoExterno(camion.getCodigoExterno())
				: conPatente;

		// La patente es única: si ya la tiene otro camión (distinto del que se sincroniza) → 409
		if (conPatente.isPresent() && (existente.isEmpty() || !conPatente.get().getId().equals(existente.get().getId()))) {
			throw FoundException.builder().message("La patente " + camion.getPatente() + " pertenece a otro camión").build();
		}

		if (existente.isEmpty()) {
			// cascade = ALL: guarda también las cisternas
			return camionDAO.save(camion);
		}

		Camion c = existente.get();
		// Si la orden no trae patente se conserva la que ya tenía el camión
		if (camion.getPatente() != null) {
			c.setPatente(camion.getPatente());
		}
		c.setDescripcion(camion.getDescripcion());
		// Se reemplazan todas las cisternas: clear() + orphanRemoval borra las viejas.
		// No usar setCisternas(otraLista): con orphanRemoval Hibernate exige conservar la misma lista
		c.getCisternas().clear();
		for (Cisterna cisterna : camion.getCisternas()) {
			cisterna.setCamion(c);
			c.getCisternas().add(cisterna);
		}
		return camionDAO.save(c);
	}

	private Chofer obtenerOCrearChofer(Chofer chofer) throws BadRequestException, FoundException {
		if (chofer.getCodigoExterno() == null && chofer.getDocumento() == null) {
			throw BadRequestException.builder().message("El chofer necesita código externo o documento").build();
		}
		if (chofer.getNombre() == null || chofer.getApellido() == null) {
			throw BadRequestException.builder().message("Faltan el nombre o el apellido del chofer").build();
		}

		Optional<Chofer> conDocumento = chofer.getDocumento() != null
				? choferDAO.findByDocumento(chofer.getDocumento())
				: Optional.empty();
		Optional<Chofer> existente = chofer.getCodigoExterno() != null
				? choferDAO.findByCodigoExterno(chofer.getCodigoExterno())
				: conDocumento;

		// El documento es único: si ya lo tiene otro chofer (distinto del que se sincroniza) → 409
		if (conDocumento.isPresent() && (existente.isEmpty() || !conDocumento.get().getId().equals(existente.get().getId()))) {
			throw FoundException.builder().message("El documento " + chofer.getDocumento() + " pertenece a otro chofer").build();
		}

		if (existente.isEmpty()) {
			return choferDAO.save(chofer);
		}

		Chofer c = existente.get();
		c.setNombre(chofer.getNombre());
		c.setApellido(chofer.getApellido());
		c.setDocumento(chofer.getDocumento());
		return choferDAO.save(c);
	}

	private Cliente obtenerOCrearCliente(Cliente cliente) throws BadRequestException {
		// La razón social no es única, así que el cliente solo se identifica por código externo
		if (cliente.getCodigoExterno() == null) {
			throw BadRequestException.builder().message("Falta el código externo del cliente").build();
		}
		if (cliente.getRazonSocial() == null) {
			throw BadRequestException.builder().message("Falta la razón social del cliente").build();
		}

		Optional<Cliente> existente = clienteDAO.findByCodigoExterno(cliente.getCodigoExterno());
		if (existente.isEmpty()) {
			return clienteDAO.save(cliente);
		}

		Cliente c = existente.get();
		c.setRazonSocial(cliente.getRazonSocial());
		c.setContacto(cliente.getContacto());
		return clienteDAO.save(c);
	}

	private Producto obtenerOCrearProducto(Producto producto) throws BadRequestException {
		// El nombre no es único, así que el producto solo se identifica por código externo
		if (producto.getCodigoExterno() == null) {
			throw BadRequestException.builder().message("Falta el código externo del producto").build();
		}
		if (producto.getNombre() == null) {
			throw BadRequestException.builder().message("Falta el nombre del producto").build();
		}

		Optional<Producto> existente = productoDAO.findByCodigoExterno(producto.getCodigoExterno());
		if (existente.isEmpty()) {
			return productoDAO.save(producto);
		}

		Producto p = existente.get();
		p.setNombre(producto.getNombre());
		p.setDescripcion(producto.getDescripcion());
		return productoDAO.save(p);
	}

	// Auditoría: quién cambió el estado, cuándo y de cuál a cuál (los tickets 05 a 09 repiten este patrón)
	private void registrarCambioEstado(Orden orden, Integer estadoAnterior, int estadoNuevo, LocalDateTime fecha,
			Usuario usuario, String observaciones) {
		OrdenEstadoLog registro = new OrdenEstadoLog();
		registro.setOrden(orden);
		registro.setEstadoAnterior(estadoAnterior);
		registro.setEstadoNuevo(estadoNuevo);
		registro.setFecha(fecha);
		registro.setUsuario(usuario);
		registro.setObservaciones(observaciones);
		ordenEstadoLogDAO.save(registro);
	}
}
