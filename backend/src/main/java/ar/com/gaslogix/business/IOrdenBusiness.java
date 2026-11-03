package ar.com.gaslogix.business;

import java.math.BigDecimal;

import ar.com.gaslogix.auth.Usuario;
import ar.com.gaslogix.business.exception.BadRequestException;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.FoundException;
import ar.com.gaslogix.business.exception.InvalidStateException;
import ar.com.gaslogix.business.exception.NotFoundException;
import ar.com.gaslogix.model.Orden;

public interface IOrdenBusiness {

	// Busca la orden por el número que manda SAP (lo usan todos los puntos a partir del 2)
	public Orden cargarPorNumeroOrden(Long numeroOrden) throws NotFoundException, BusinessException;

	// Punto 1: recibe el JSON de SAP y crea la orden en estado 1 (pendiente de pesaje inicial)
	public Orden recibirOrden(String json, Usuario usuario) throws BadRequestException, FoundException, BusinessException;

	// Punto 2: registra la tara, genera la contraseña de activación y pasa la orden a estado 2
	public Orden registrarPesajeInicial(Long numeroOrden, BigDecimal tara, Usuario usuario)
			throws BadRequestException, NotFoundException, InvalidStateException, BusinessException;
}
