-- Esquema GasLogix (MySQL 8).
-- Fuente de verdad del modelo: Hibernate corre con ddl-auto=validate.
-- Basado en el export de drawSQL del 2026-10-20 + tablas de usuarios y usuario en el log.

CREATE DATABASE IF NOT EXISTS gaslogix CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gaslogix;

-- ---------------------------------------------------------------
-- Seguridad
-- ---------------------------------------------------------------

CREATE TABLE usuario (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username                VARCHAR(100) NOT NULL,
    email                   VARCHAR(255) NOT NULL,
    password                VARCHAR(255) NOT NULL, -- hash BCrypt
    enabled                 TINYINT(1)   NOT NULL DEFAULT 1,
    account_non_expired     TINYINT(1)   NOT NULL DEFAULT 1,
    account_non_locked      TINYINT(1)   NOT NULL DEFAULT 1,
    credentials_non_expired TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT usuario_username_unique UNIQUE (username),
    CONSTRAINT usuario_email_unique UNIQUE (email)
);

CREATE TABLE rol (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL, -- ej.: ROLE_ADMIN
    descripcion VARCHAR(100) NOT NULL,
    CONSTRAINT rol_nombre_unique UNIQUE (nombre)
);

CREATE TABLE usuario_rol (
    id_usuario BIGINT NOT NULL,
    id_rol     BIGINT NOT NULL,
    PRIMARY KEY (id_usuario, id_rol),
    CONSTRAINT usuario_rol_id_usuario_foreign FOREIGN KEY (id_usuario) REFERENCES usuario (id),
    CONSTRAINT usuario_rol_id_rol_foreign FOREIGN KEY (id_rol) REFERENCES rol (id)
);

-- ---------------------------------------------------------------
-- Entidades sincronizadas desde SAP
-- ---------------------------------------------------------------

CREATE TABLE camion (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo_externo VARCHAR(50)  NULL,
    patente        VARCHAR(20)  NOT NULL,
    descripcion    VARCHAR(255) NULL,
    CONSTRAINT camion_codigo_externo_unique UNIQUE (codigo_externo),
    CONSTRAINT camion_patente_unique UNIQUE (patente)
);

CREATE TABLE cisterna (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_camion        BIGINT        NOT NULL,
    codigo_externo   VARCHAR(50)   NULL,
    nombre           VARCHAR(50)   NOT NULL,
    capacidad_litros DECIMAL(10, 2) NOT NULL,
    CONSTRAINT cisterna_id_camion_foreign FOREIGN KEY (id_camion) REFERENCES camion (id)
);

CREATE TABLE chofer (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo_externo VARCHAR(50)  NULL,
    nombre         VARCHAR(100) NOT NULL,
    apellido       VARCHAR(100) NOT NULL,
    documento      VARCHAR(20)  NULL,
    CONSTRAINT chofer_codigo_externo_unique UNIQUE (codigo_externo)
);

CREATE TABLE cliente (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo_externo VARCHAR(50)  NULL,
    razon_social   VARCHAR(255) NOT NULL,
    contacto       VARCHAR(255) NULL,
    CONSTRAINT cliente_codigo_externo_unique UNIQUE (codigo_externo)
);

CREATE TABLE producto (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo_externo VARCHAR(50)  NULL,
    nombre         VARCHAR(100) NOT NULL,
    descripcion    VARCHAR(255) NULL,
    CONSTRAINT producto_codigo_externo_unique UNIQUE (codigo_externo)
);

-- ---------------------------------------------------------------
-- Orden y su trazabilidad
-- ---------------------------------------------------------------

CREATE TABLE orden (
    id                    BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero_orden          BIGINT         NOT NULL,
    codigo_externo        VARCHAR(50)    NULL,
    estado                TINYINT        NOT NULL DEFAULT 1,
    id_camion             BIGINT         NOT NULL,
    id_chofer             BIGINT         NOT NULL,
    id_cliente            BIGINT         NOT NULL,
    id_producto           BIGINT         NOT NULL,
    fecha_carga_prevista  DATETIME       NOT NULL,
    preset                DECIMAL(10, 2) NOT NULL,
    fecha_recepcion       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tara                  DECIMAL(10, 2) NULL,
    fecha_pesaje_inicial  DATETIME       NULL,
    password_activacion   INT            NULL,
    ultima_masa_acumulada DECIMAL(10, 2) NULL,
    ultima_densidad       DECIMAL(6, 4)  NULL,
    ultima_temperatura    DECIMAL(5, 2)  NULL,
    ultimo_caudal         DECIMAL(10, 2) NULL,
    fecha_ultimo_dato     DATETIME       NULL,
    fecha_inicio_carga    DATETIME       NULL,
    fecha_fin_carga       DATETIME       NULL,
    pesaje_final          DECIMAL(10, 2) NULL,
    fecha_pesaje_final    DATETIME       NULL,
    CONSTRAINT orden_numero_orden_unique UNIQUE (numero_orden),
    CONSTRAINT orden_codigo_externo_unique UNIQUE (codigo_externo),
    CONSTRAINT orden_id_camion_foreign FOREIGN KEY (id_camion) REFERENCES camion (id),
    CONSTRAINT orden_id_chofer_foreign FOREIGN KEY (id_chofer) REFERENCES chofer (id),
    CONSTRAINT orden_id_cliente_foreign FOREIGN KEY (id_cliente) REFERENCES cliente (id),
    CONSTRAINT orden_id_producto_foreign FOREIGN KEY (id_producto) REFERENCES producto (id)
);

CREATE TABLE detalle_carga (
    id             BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_orden       BIGINT         NOT NULL,
    masa_acumulada DECIMAL(10, 2) NOT NULL,
    densidad       DECIMAL(6, 4)  NOT NULL,
    temperatura    DECIMAL(5, 2)  NOT NULL,
    caudal         DECIMAL(10, 2) NOT NULL,
    fecha          DATETIME       NOT NULL,
    INDEX detalle_carga_id_orden_fecha_index (id_orden, fecha),
    CONSTRAINT detalle_carga_id_orden_foreign FOREIGN KEY (id_orden) REFERENCES orden (id)
);

CREATE TABLE orden_estado_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_orden        BIGINT       NOT NULL,
    estado_anterior TINYINT      NULL, -- NULL en la creación de la orden
    estado_nuevo    TINYINT      NOT NULL,
    fecha           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario      BIGINT       NOT NULL, -- operador logueado que hizo la transición
    observaciones   VARCHAR(200) NULL,
    INDEX orden_estado_log_id_orden_index (id_orden),
    CONSTRAINT orden_estado_log_id_orden_foreign FOREIGN KEY (id_orden) REFERENCES orden (id),
    CONSTRAINT orden_estado_log_id_usuario_foreign FOREIGN KEY (id_usuario) REFERENCES usuario (id)
);
