package ar.com.gaslogix.business;

import ar.com.gaslogix.business.exception.BadRequestException;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.InvalidStateException;
import ar.com.gaslogix.business.exception.NotFoundException;
import ar.com.gaslogix.model.dto.DatoCargaRequest;
import ar.com.gaslogix.model.dto.DatoCargaResponse;

public interface ICargaBusiness {

	// Punto 3.b: recibe una lectura del caudalímetro para una orden en estado 2
	public DatoCargaResponse recibirDatoCarga(Long numeroOrden, DatoCargaRequest dato)
			throws BadRequestException, NotFoundException, InvalidStateException, BusinessException;
}
