package ar.com.gaslogix.controllers;

public final class Constants {

	private Constants() {
	}

	// Rutas de la API
	public static final String URL_API = "/api";
	public static final String URL_API_VERSION = "/v1";
	public static final String URL_BASE = URL_API + URL_API_VERSION;

	public static final String URL_LOGIN = URL_BASE + "/login";
	public static final String URL_ORDENES = URL_BASE + "/ordenes";

	// Estados de la orden
	public static final int ESTADO_PENDIENTE_PESAJE_INICIAL = 1;
	public static final int ESTADO_CON_PESAJE_INICIAL = 2;
	public static final int ESTADO_CERRADA_PARA_CARGA = 3;
	public static final int ESTADO_FINALIZADA = 4;
}
