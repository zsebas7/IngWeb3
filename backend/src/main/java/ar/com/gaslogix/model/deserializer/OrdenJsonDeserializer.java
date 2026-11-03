package ar.com.gaslogix.model.deserializer;

import java.time.LocalDateTime;

import ar.com.gaslogix.model.Camion;
import ar.com.gaslogix.model.Chofer;
import ar.com.gaslogix.model.Cisterna;
import ar.com.gaslogix.model.Cliente;
import ar.com.gaslogix.model.Orden;
import ar.com.gaslogix.model.Producto;
import ar.com.gaslogix.util.JsonUtiles;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Convierte el JSON de una orden de SAP en una Orden con sus entidades (camión con cisternas, chofer,
 * cliente y producto). Acepta distintos nombres para cada atributo.
 * Solo arma los objetos: validar datos y buscar entidades existentes es tarea de OrdenBusiness.
 */
public class OrdenJsonDeserializer extends StdDeserializer<Orden> {

	// Nombres aceptados para cada atributo (se usa el primero que venga en el JSON)
	private static final String[] NUMERO_ORDEN = { "numeroOrden", "numero_orden", "orderNumber", "order_number" };
	private static final String[] CODIGO_EXTERNO = { "codigoExterno", "codigo_externo", "externalCode", "external_code", "codigo", "code" };
	private static final String[] FECHA_CARGA_PREVISTA = { "fechaCargaPrevista", "fecha_carga_prevista", "fechaPrevista", "expectedLoadDate" };
	private static final String[] PRESET = { "preset", "presetKg", "preset_kg" };

	private static final String[] CAMION = { "camion", "truck" };
	private static final String[] PATENTE = { "patente", "dominio", "plate", "licensePlate" };
	private static final String[] CISTERNAS = { "cisternas", "cisternado", "tanks" };
	private static final String[] CAPACIDAD_LITROS = { "capacidadLitros", "capacidad_litros", "capacidad", "capacity" };

	private static final String[] CHOFER = { "chofer", "conductor", "driver" };
	private static final String[] APELLIDO = { "apellido", "lastName", "last_name" };
	private static final String[] DOCUMENTO = { "documento", "dni", "document" };

	private static final String[] CLIENTE = { "cliente", "customer", "client" };
	private static final String[] RAZON_SOCIAL = { "razonSocial", "razon_social", "businessName" };
	private static final String[] CONTACTO = { "contacto", "contact", "email" };

	private static final String[] PRODUCTO = { "producto", "product" };
	private static final String[] NOMBRE = { "nombre", "name" };
	private static final String[] DESCRIPCION = { "descripcion", "description" };

	public OrdenJsonDeserializer() {
		super(Orden.class);
	}

	@Override
	public Orden deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
		// El JSON completo como árbol de nodos
		JsonNode node = ctxt.readTree(jp);

		Orden orden = new Orden();
		orden.setNumeroOrden(JsonUtiles.getLong(node, NUMERO_ORDEN, null));
		orden.setCodigoExterno(JsonUtiles.getString(node, CODIGO_EXTERNO, null));
		orden.setPreset(JsonUtiles.getBigDecimal(node, PRESET, null));

		// Formato ISO: 2026-11-05T08:00:00 (si no respeta el formato, parse lanza una excepción → 400)
		String fecha = JsonUtiles.getString(node, FECHA_CARGA_PREVISTA, null);
		orden.setFechaCargaPrevista(fecha != null ? LocalDateTime.parse(fecha) : null);

		// Si una entidad no viene, queda null y OrdenBusiness responde 400
		orden.setCamion(leerCamion(JsonUtiles.getNode(node, CAMION)));
		orden.setChofer(leerChofer(JsonUtiles.getNode(node, CHOFER)));
		orden.setCliente(leerCliente(JsonUtiles.getNode(node, CLIENTE)));
		orden.setProducto(leerProducto(JsonUtiles.getNode(node, PRODUCTO)));
		return orden;
	}

	private Camion leerCamion(JsonNode node) {
		if (node == null) {
			return null;
		}
		Camion camion = new Camion();
		camion.setCodigoExterno(JsonUtiles.getString(node, CODIGO_EXTERNO, null));
		camion.setPatente(JsonUtiles.getString(node, PATENTE, null));
		camion.setDescripcion(JsonUtiles.getString(node, DESCRIPCION, null));

		JsonNode cisternas = JsonUtiles.getNode(node, CISTERNAS);
		if (cisternas != null && cisternas.isArray()) {
			for (JsonNode c : cisternas) {
				Cisterna cisterna = new Cisterna();
				cisterna.setCodigoExterno(JsonUtiles.getString(c, CODIGO_EXTERNO, null));
				cisterna.setNombre(JsonUtiles.getString(c, NOMBRE, null));
				cisterna.setCapacidadLitros(JsonUtiles.getBigDecimal(c, CAPACIDAD_LITROS, null));
				// Se enlaza con su camión: es el lado que tiene la FK (id_camion)
				cisterna.setCamion(camion);
				camion.getCisternas().add(cisterna);
			}
		}
		return camion;
	}

	private Chofer leerChofer(JsonNode node) {
		if (node == null) {
			return null;
		}
		Chofer chofer = new Chofer();
		chofer.setCodigoExterno(JsonUtiles.getString(node, CODIGO_EXTERNO, null));
		chofer.setNombre(JsonUtiles.getString(node, NOMBRE, null));
		chofer.setApellido(JsonUtiles.getString(node, APELLIDO, null));
		chofer.setDocumento(JsonUtiles.getString(node, DOCUMENTO, null));
		return chofer;
	}

	private Cliente leerCliente(JsonNode node) {
		if (node == null) {
			return null;
		}
		Cliente cliente = new Cliente();
		cliente.setCodigoExterno(JsonUtiles.getString(node, CODIGO_EXTERNO, null));
		cliente.setRazonSocial(JsonUtiles.getString(node, RAZON_SOCIAL, null));
		cliente.setContacto(JsonUtiles.getString(node, CONTACTO, null));
		return cliente;
	}

	private Producto leerProducto(JsonNode node) {
		if (node == null) {
			return null;
		}
		Producto producto = new Producto();
		producto.setCodigoExterno(JsonUtiles.getString(node, CODIGO_EXTERNO, null));
		producto.setNombre(JsonUtiles.getString(node, NOMBRE, null));
		producto.setDescripcion(JsonUtiles.getString(node, DESCRIPCION, null));
		return producto;
	}
}
