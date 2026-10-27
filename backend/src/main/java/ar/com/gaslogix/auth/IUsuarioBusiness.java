package ar.com.gaslogix.auth;

import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.NotFoundException;

public interface IUsuarioBusiness {

	public Usuario load(String username) throws NotFoundException, BusinessException;
}
