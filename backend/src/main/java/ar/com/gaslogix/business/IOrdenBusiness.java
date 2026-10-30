package ar.com.gaslogix.business;

import ar.com.gaslogix.auth.Usuario;
import ar.com.gaslogix.business.exception.BadRequestException;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.FoundException;
import ar.com.gaslogix.model.Orden;

public interface IOrdenBusiness {

	// Punto 1: recibe el JSON de SAP y crea la orden en estado 1 (pendiente de pesaje inicial)
	public Orden recibirOrden(String json, Usuario usuario) throws BadRequestException, FoundException, BusinessException;
}
