package ar.com.gaslogix.business;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.gaslogix.auth.Usuario;
import ar.com.gaslogix.business.exception.BadRequestException;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.FoundException;
import ar.com.gaslogix.controllers.Constants;
import ar.com.gaslogix.model.Camion;
import ar.com.gaslogix.model.Chofer;
import ar.com.gaslogix.model.Cisterna;
import ar.com.gaslogix.model.Cliente;
import ar.com.gaslogix.model.Orden;
import ar.com.gaslogix.model.OrdenEstadoLog;
import ar.com.gaslogix.model.Producto;
import ar.com.gaslogix.model.deserializer.OrdenJsonDeserializer;
import ar.com.gaslogix.model.persistence.CamionRepository;
import ar.com.gaslogix.model.persistence.ChoferRepository;
import ar.com.gaslogix.model.persistence.ClienteRepository;
import ar.com.gaslogix.model.persistence.OrdenEstadoLogRepository;
import ar.com.gaslogix.model.persistence.OrdenRepository;
import ar.com.gaslogix.model.persistence.ProductoRepository;
import ar.com.gaslogix.util.JsonUtiles;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class OrdenBusiness implements IOrdenBusiness {

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

			registrarCambioEstado(orden, null, Constants.ESTADO_PENDIENTE_PESAJE_INICIAL, ahora, usuario);
			return orden;
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
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

	/*
	 * obtenerOCrear...: si viene con un código externo que ya está en la BD, se reutiliza la entidad existente
	 * (sin actualizarla). Si no, se valida que traiga los datos obligatorios y se crea.
	 * El ticket 04 amplía estos métodos (sincronizar datos, buscar por patente/documento/etc.).
	 */

	private Camion obtenerOCrearCamion(Camion camion) throws BadRequestException {
		if (camion.getCodigoExterno() != null) {
			Optional<Camion> existente = camionDAO.findByCodigoExterno(camion.getCodigoExterno());
			if (existente.isPresent()) {
				return existente.get();
			}
		}
		if (camion.getPatente() == null) {
			throw BadRequestException.builder().message("Falta la patente del camión").build();
		}
		for (Cisterna c : camion.getCisternas()) {
			if (c.getNombre() == null || c.getCapacidadLitros() == null) {
				throw BadRequestException.builder().message("Cada cisterna necesita nombre y capacidad en litros").build();
			}
		}
		// cascade = ALL: guarda también las cisternas
		return camionDAO.save(camion);
	}

	private Chofer obtenerOCrearChofer(Chofer chofer) throws BadRequestException {
		if (chofer.getCodigoExterno() != null) {
			Optional<Chofer> existente = choferDAO.findByCodigoExterno(chofer.getCodigoExterno());
			if (existente.isPresent()) {
				return existente.get();
			}
		}
		if (chofer.getNombre() == null || chofer.getApellido() == null) {
			throw BadRequestException.builder().message("Faltan el nombre o el apellido del chofer").build();
		}
		return choferDAO.save(chofer);
	}

	private Cliente obtenerOCrearCliente(Cliente cliente) throws BadRequestException {
		if (cliente.getCodigoExterno() != null) {
			Optional<Cliente> existente = clienteDAO.findByCodigoExterno(cliente.getCodigoExterno());
			if (existente.isPresent()) {
				return existente.get();
			}
		}
		if (cliente.getRazonSocial() == null) {
			throw BadRequestException.builder().message("Falta la razón social del cliente").build();
		}
		return clienteDAO.save(cliente);
	}

	private Producto obtenerOCrearProducto(Producto producto) throws BadRequestException {
		if (producto.getCodigoExterno() != null) {
			Optional<Producto> existente = productoDAO.findByCodigoExterno(producto.getCodigoExterno());
			if (existente.isPresent()) {
				return existente.get();
			}
		}
		if (producto.getNombre() == null) {
			throw BadRequestException.builder().message("Falta el nombre del producto").build();
		}
		return productoDAO.save(producto);
	}

	// Auditoría: quién cambió el estado, cuándo y de cuál a cuál (los tickets 05 a 09 repiten este patrón)
	private void registrarCambioEstado(Orden orden, Integer estadoAnterior, int estadoNuevo, LocalDateTime fecha,
			Usuario usuario) {
		OrdenEstadoLog registro = new OrdenEstadoLog();
		registro.setOrden(orden);
		registro.setEstadoAnterior(estadoAnterior);
		registro.setEstadoNuevo(estadoNuevo);
		registro.setFecha(fecha);
		registro.setUsuario(usuario);
		registro.setObservaciones("Orden recibida desde SAP");
		ordenEstadoLogDAO.save(registro);
	}
}
