-- Datos iniciales GasLogix: roles y usuarios de prueba.
-- Ejecutar después de schema.sql.
--
-- Credenciales de prueba (solo desarrollo; cambiar en el despliegue):
--   admin        / Admin.2026     (ROLE_ADMIN)
--   operador     / Operador.2026  (ROLE_OPERADOR)
--   caudalimetro / Caudal.2026    (ROLE_OPERADOR, usuario técnico del sistema de carga)
-- Las contraseñas se guardan con hash BCrypt.

USE gaslogix;

INSERT INTO rol (id, nombre, descripcion) VALUES
    (1, 'ROLE_ADMIN', 'Administrador del sistema'),
    (2, 'ROLE_OPERADOR', 'Operador de planta');

INSERT INTO usuario (id, username, email, password) VALUES
    (1, 'admin', 'admin@gaslogix.com.ar', '$2a$10$eetwnsUVaZYpzBgUXC/bHessphDtlbgF1T94lWXD2StdLZo1us6De'),
    (2, 'operador', 'operador@gaslogix.com.ar', '$2a$10$.AaHwNa.vwBAEKNrCJE0UOcoRNIVay7xeqcRFBu6F9Nhc6ISF.oee'),
    (3, 'caudalimetro', 'caudalimetro@gaslogix.com.ar', '$2a$10$Y9isKyHQjUIMajA/kqb2oOdYvJUW0kp602zghtn85gHJoy3I7j21u');

INSERT INTO usuario_rol (id_usuario, id_rol) VALUES
    (1, 1),
    (2, 2),
    (3, 2);
