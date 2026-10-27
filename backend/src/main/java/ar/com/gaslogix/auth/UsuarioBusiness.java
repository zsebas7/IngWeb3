package ar.com.gaslogix.auth;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UsuarioBusiness implements IUsuarioBusiness {

	@Autowired
	private UsuarioRepository usuarioDAO;

	@Override
	public Usuario load(String username) throws NotFoundException, BusinessException {
		Optional<Usuario> r;
		try {
			r = usuarioDAO.findByUsername(username);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw BusinessException.builder().ex(e).build();
		}
		if (r.isEmpty()) {
			throw NotFoundException.builder().message("No se encuentra el usuario '" + username + "'").build();
		}
		return r.get();
	}
}
