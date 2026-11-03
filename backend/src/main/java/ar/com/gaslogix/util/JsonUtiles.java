package ar.com.gaslogix.util;

import java.math.BigDecimal;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Utilidades para leer JSON de sistemas externos (SAP, TMS) tolerando distintos nombres de atributos.
 * Cada get recibe una lista de nombres posibles y devuelve el valor del primero que encuentra.
 */
public final class JsonUtiles {

	private JsonUtiles() {
	}

	/**
	 * ObjectMapper que usa el deserializador propio para la clase indicada.
	 */
	public static <T> ObjectMapper getObjectMapper(Class<T> clazz, StdDeserializer<T> deser) {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(clazz, deser);
		return JsonMapper.builder().addModule(module).build();
	}

	/**
	 * Primer atributo de la lista que existe y no es null, o null si no viene ninguno.
	 * Ejemplo: getNode(orden, {"camion", "truck"}) devuelve el objeto del camión venga con uno u otro nombre.
	 */
	public static JsonNode getNode(JsonNode node, String[] attrs) {
		if (node == null) {
			return null;
		}
		for (String attr : attrs) {
			JsonNode value = node.get(attr);
			if (value != null && !value.isNull()) {
				return value;
			}
		}
		return null;
	}

	public static String getString(JsonNode node, String[] attrs, String defaultValue) {
		JsonNode value = getNode(node, attrs);
		return value != null ? value.asString() : defaultValue;
	}

	// Solo acepta números enteros (ej. el número de orden); si no viene o no es entero, devuelve el default
	public static Long getLong(JsonNode node, String[] attrs, Long defaultValue) {
		JsonNode value = getNode(node, attrs);
		return value != null && value.isIntegralNumber() ? value.asLong() : defaultValue;
	}

	// BigDecimal para no perder precisión en kg y litros
	public static BigDecimal getBigDecimal(JsonNode node, String[] attrs, BigDecimal defaultValue) {
		JsonNode value = getNode(node, attrs);
		return value != null && value.isNumber() ? value.decimalValue() : defaultValue;
	}
}
