# Decisiones de diseño – GasLogix

Registro de las decisiones tomadas antes de implementar. Si alguna cambia, actualizar este archivo.

## Proyecto

| Tema | Decisión |
| --- | --- |
| Ubicación | Proyecto Maven en `backend/` |
| Paquete base | `ar.com.gaslogix` |
| Stack | Java 21, Spring Boot 4.1.1, Maven, Spring Data JPA (Hibernate), MySQL 8, Lombok, springdoc-openapi (Swagger), Spring Security + JWT (`com.auth0:java-jwt`) |
| Empaquetado | JAR ejecutable (`java -jar`, Tomcat embebido) |
| Esquema | `backend/db/schema.sql` es la fuente de verdad. drawSQL se actualiza a partir de él |
| Datos iniciales | `backend/db/data.sql` (roles y usuarios de prueba) |
| `ddl-auto` | `validate`: Hibernate no crea ni modifica tablas, solo verifica que las entidades coincidan con el esquema |
| Tipos | Columnas `DECIMAL` → `BigDecimal`; `estado TINYINT` → `int` con `columnDefinition = "TINYINT"` |
| Frecuencia de almacenamiento del detalle | Propiedad `carga.detalle.frecuencia-segundos` en `application.properties`. No se guarda en BD; cambiarla requiere reiniciar |

## Estructura de paquetes

```
ar.com.gaslogix
├── GaslogixApplication
├── auth/                 Usuario, Rol, repositorios, UserBusiness, filtro JWT
├── security/             SecurityConfiguration
├── config/               OpenApiConfig
├── controllers/          Constants, BaseRestController, AuthRestController,
│                         OrdenRestController   (1 recepción, 4 cierre, conciliación)
│                         PesajeRestController  (2 pesaje inicial, 5 pesaje final)
│                         CargaRestController   (3.a contraseña, 3.b detalle)
├── business/             Interfaces IXxxBusiness + implementaciones (lógica y máquina de estados)
│   └── exception/        BusinessException, NotFoundException, FoundException, ...
├── model/                Entidades JPA
│   ├── deserializer/     OrdenJsonDeserializer (JSON de SAP)
│   ├── dto/              Conciliacion y otros objetos no persistidos
│   └── persistence/      Repositorios Spring Data
└── util/                 StandartResponse, JsonUtiles
```

Se toma como referencia https://github.com/magm3333/iw3_2026 (capas, excepciones con `@Builder`, `StandartResponse`, `Constants`, deserializador con `JsonUtiles`). No se usa la herencia de entidades por cliente (`ProductCli1 extends Product`) porque cada tabla ya tiene `codigo_externo`.

## Estados de la orden

Constantes `int` en `Constants.java`: `ESTADO_PENDIENTE_PESAJE_INICIAL = 1`, `ESTADO_CON_PESAJE_INICIAL = 2`, `ESTADO_CERRADA_PARA_CARGA = 3`, `ESTADO_FINALIZADA = 4`.
Cada transición se registra en `orden_estado_log`, en la misma transacción.

## Seguridad

- Login con JWT desde el segundo parcial.
- Tablas `usuario`, `rol` y `usuario_rol` (N:M). Los flags de cuenta (`enabled`, `account_non_expired`, ...) mantienen los nombres de `UserDetails` de Spring Security.
- Roles: `ROLE_ADMIN` y `ROLE_OPERADOR`.
- Usuarios precargados: `admin` (ADMIN), `operador` (OPERADOR) y `caudalimetro` (OPERADOR, usuario técnico del sistema de carga).
- Todas las acciones las realiza un **operador logueado**: recepción de la orden de SAP, pesajes, validación de contraseña (3.a) y cierre. Su id queda en `orden_estado_log.id_usuario`.
- El caudalímetro se autentica para enviar lecturas (3.b). Como eso no cambia el estado, no aparece en el log.

## 1. Recepción de la orden

- El JSON se parsea con un `StdDeserializer` + `JsonUtiles`, como el endpoint `/b2b` del repo de referencia.
- Sincronización de entidades: se busca primero por código externo y, si no viene, por un identificador alternativo:

  | Entidad | Identificador alternativo |
  | --- | --- |
  | Camión | patente |
  | Chofer | documento |
  | Cliente | razón social |
  | Producto | nombre |

  Si faltan el código externo y el identificador alternativo → **400**.
- Si el código externo del camión existe pero la patente recibida pertenece a otro camión → **409**.
- Cisternas: se reemplaza la lista completa del camión por la recibida.
- `orden.codigo_externo` es un código opcional del cliente, distinto de `numero_orden`; queda NULL si el cliente solo usa el número.
- Orden duplicada → **409** si se repite `numero_orden`, o `codigo_externo` cuando no es NULL.
- La fecha de carga prevista debe ser futura; si no → **400**.

## 2. Pesaje inicial

- Solo en estado 1. La tara debe ser > 0 (si no → **400**).
- Genera una contraseña aleatoria de 5 dígitos (10000–99999). No necesita ser única entre órdenes, porque siempre se valida junto con el número de orden.

## 3.a Validación de contraseña

- Recibe número de orden + contraseña. La orden debe estar en estado 2.
- Responde `{"preset": n}`.
- Puede validarse más de una vez mientras la orden siga en estado 2.

## 3.b Datos de carga

- La orden se identifica solo por número de orden (no se envía la contraseña).
- El timestamp de cada lectura lo asigna el servidor al recibirla.
- Se descarta con **400** y mensaje si: caudal ≤ 0, masa acumulada ≤ 0 o masa acumulada < último valor.
- Densidad y temperatura no se validan (solo deben venir).
- Se aceptan lecturas con masa mayor al preset (el corte lo hace la bomba).
- Cada lectura válida sobrescribe los últimos valores de la orden.
- El primer dato válido se guarda siempre en `detalle_carga`; los siguientes solo si pasó la frecuencia desde el último detalle guardado.

## 4. Cierre

- Solo en estado 2. Si la orden nunca recibió datos de carga → **409**.

## 5. Pesaje final y conciliación

- Solo en estado 3. El pesaje final debe ser mayor que la tara (si no → **400**).
- La conciliación (DTO, no se persiste) se devuelve al registrar el pesaje final y puede consultarse luego, solo en estado 4.

## Códigos HTTP

| Código | Uso |
| --- | --- |
| 400 | Datos inválidos o lectura descartada |
| 401 | Contraseña de activación incorrecta / no autenticado |
| 403 | Rol sin permiso |
| 404 | Orden inexistente |
| 409 | Estado inválido o duplicado |
