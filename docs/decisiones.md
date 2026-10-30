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
├── auth/                 Usuario, Rol, UsuarioRepository, UsuarioBusiness, JwtService, DTOs de login,
│   ├── custom/           CustomAuthenticationManager
│   └── filters/          JWTAuthorizationFilter
├── security/             SecurityConfiguration, JsonSecurityErrorHandler (401/403)
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

## Flujo de trabajo con Git

- `main`: versión estable. Solo se actualiza desde `develop`.
- `develop`: rama de integración, creada desde `main`.
- Una rama por ticket, creada desde `develop`, con el nombre `ticket-NN-descripcion` (por ejemplo, `ticket-01-base-comun`).
- Al terminar un ticket se abre un Pull Request de la rama del ticket hacia `develop`.

## Tickets

- Los tickets del segundo parcial están en `.scratch/segundo-parcial/issues/`, uno por archivo.
- Se generaron con la skill `to-tickets`, que no se versiona.
  
  | Integrante | Tickets |
  | --- | --- |
  | Leonel Ruiz Notari | 01, 02 |
  | Valentina Beas | 07, 09 |
  | Mirko Bubica | 03, 05, 06 |
  | Sebastián Caparroz | 04, 08, 10 |

## Manejo de errores

- Cada controller captura las excepciones de negocio con `try/catch` y arma la respuesta con `StandartResponse`. No se usa un handler global (`@RestControllerAdvice`).
- Excepciones (en inglés), cada una con un único `@Builder (message, ex)`:

  | Excepción | Código HTTP | Uso |
  | --- | --- | --- |
  | `BusinessException` | 500 | Error inesperado de lógica o de acceso a la BD |
  | `NotFoundException` | 404 | Entidad inexistente |
  | `FoundException` | 409 | Entidad duplicada |
  | `InvalidStateException` | 409 | La orden no está en el estado requerido |
  | `BadRequestException` | 400 | Datos inválidos o incompletos |
  | `BadPasswordException` | 401 | Contraseña de activación incorrecta |

## Estados de la orden

Constantes `int` en `Constants.java`: `ESTADO_PENDIENTE_PESAJE_INICIAL = 1`, `ESTADO_CON_PESAJE_INICIAL = 2`, `ESTADO_CERRADA_PARA_CARGA = 3`, `ESTADO_FINALIZADA = 4`.
Cada transición se registra en `orden_estado_log`, en la misma transacción.

## Seguridad

- Login con JWT.
- Tablas `usuario`, `rol` y `usuario_rol` (N:M). Los flags de cuenta (`enabled`, `account_non_expired`, ...) mantienen los nombres de `UserDetails` de Spring Security.
- Roles: `ROLE_ADMIN` y `ROLE_OPERADOR`.
- Usuarios precargados: `admin` (ADMIN), `operador` (OPERADOR) y `caudalimetro` (OPERADOR, usuario técnico del sistema de carga).
- Todas las acciones las realiza un **operador logueado**: recepción de la orden de SAP, pesajes, validación de contraseña (3.a) y cierre. Su id queda en `orden_estado_log.id_usuario`.
- El caudalímetro se autentica para enviar lecturas (3.b). Como eso no cambia el estado, no aparece en el log.

### Login y token (ticket 02)

- `POST /api/v1/login` (público) recibe JSON `{"username", "password"}`. Solo se inicia sesión con username, no con email.
- Respuesta exitosa: `{"token", "username", "roles"}`. Credenciales faltantes → **400**; incorrectas, usuario inexistente o cuenta deshabilitada/bloqueada/vencida → **401**.
- Para no revelar qué usuarios existen, "usuario inexistente" y "contraseña incorrecta" devuelven el mismo mensaje. El estado de la cuenta se informa solo si la contraseña es correcta.
- El token (HMAC-SHA256) lleva el username (`sub`), el `id`, los `roles` y vence a las **8 horas** (`jwt.expiracion-horas`).
- La clave de firma se lee de la variable de entorno `JWT_SECRET`, con un valor de desarrollo por defecto en `application.properties`. Con Docker es obligatoria en `infra/.env`: **quien tenga un `.env` anterior debe copiar la línea `JWT_SECRET` de `.env.example`**.
- Cada request trae `Authorization: Bearer <token>`. El filtro JWT valida el token y recarga el usuario de la BD, así un usuario deshabilitado pierde el acceso en el siguiente request.
- Los 401 y 403 de Spring Security también responden con `StandartResponse`. El 401 indica el motivo (falta el token, token inválido, token vencido).
- Rutas públicas: el login, Swagger (`/swagger-ui.html`, `/v3/api-docs/**`) y `/error`. Todo lo demás requiere token.
- Los endpoints se restringen por rol con `@PreAuthorize("hasRole('OPERADOR')")` (Spring agrega el prefijo `ROLE_`).
- Los controllers heredan de `BaseRestController` y obtienen el usuario logueado con `getUserLogged()`.
- `/api/v1/prueba-auth` es un endpoint temporal para probar roles; en el ticket 10 se decide si queda.

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

---

# Decisiones por ticket

Cada ticket agrega acá las decisiones que tomó durante el desarrollo y cuáles afectan a tickets futuros.

## Ticket 03 · Recepción de orden con entidades nuevas (Mirko Bubica)

### Decisiones

- **Entidades** en `model/`: `Camion`, `Cisterna`, `Chofer`, `Cliente`, `Producto`, `Orden`, `OrdenEstadoLog`, mapeadas columna por columna contra `schema.sql` (`ddl-auto=validate`).
  - Fechas con `LocalDateTime`; montos y medidas con `BigDecimal` (`precision`/`scale` iguales al `DECIMAL` del esquema).
  - `estado`, `estado_anterior` y `estado_nuevo` con `columnDefinition = "TINYINT"`. `estadoAnterior` es `Integer` porque es NULL al crear la orden.
  - `Camion` → `Cisterna`: `@OneToMany(mappedBy = "camion", cascade = CascadeType.ALL)`; al guardar el camión se guardan sus cisternas. `Cisterna.camion` lleva `@JsonIgnore` para evitar un bucle al convertir a JSON.
  - `Orden` → camión, chofer, cliente y producto: `@ManyToOne` con `@JoinColumn` (`id_camion`, etc.).
  - `OrdenEstadoLog.usuario` es `@ManyToOne` a `auth.Usuario` (el usuario logueado de `getUserLogged()`).
- **Endpoint** `POST /api/v1/ordenes` (`Constants.URL_ORDENES`), solo `ROLE_OPERADOR` o `ROLE_ADMIN`. Recibe el JSON crudo (`HttpEntity<String>`) y responde **201** con `Location: /api/v1/ordenes/{numeroOrden}`.
- **JSON de SAP** en camelCase: `numeroOrden`, `codigoExterno`, `camion` (con `cisternas[]`), `chofer`, `cliente`, `producto`, `fechaCargaPrevista` (`yyyy-MM-ddTHH:mm:ss`, hora de Argentina) y `preset`. `OrdenJsonDeserializer` + `JsonUtiles` aceptan nombres alternativos.
  - Nombres alternativos aceptados (por ejemplo `numero_orden`/`orderNumber`, `truck`, `driver`, `customer`, `product`, `dominio`/`plate`, `tanks`): la lista completa está en las constantes de `OrdenJsonDeserializer`.
  - **Formato de fecha:** solo ISO-8601 **sin zona horaria** (`2026-11-05T08:00:00`, hora de Argentina), con `LocalDateTime.parse`. **Pendiente para cuando haya frontend:** JavaScript manda las fechas con `toISOString()` en UTC (`2026-11-05T11:00:00.000Z`), que hoy da 400. En ese momento hay que aceptar también fechas con zona (`Z` o `-03:00`) y convertirlas a la hora de Argentina; el cambio va solo en `OrdenJsonDeserializer`.
  - El deserializador **solo arma los objetos** (no consulta la BD): si falta un dato lo deja en `null` y `OrdenBusiness` decide el 400. Números con decimales se leen como `BigDecimal`; `numeroOrden` solo se acepta si es entero.
- `numeroOrden` es **obligatorio** (aclaración del profe); el `codigoExterno` de la orden es opcional.
- **Validaciones:** 400 si falta `numeroOrden`, `preset`, `fechaCargaPrevista` o alguna entidad, si el `preset` no es mayor a 0, si la fecha de carga prevista es pasada o si el JSON es inválido. 409 si se repite `numeroOrden`, o el `codigoExterno` de la orden cuando no es NULL.
  - Al **crear** una entidad nueva también es 400 si le falta un dato `NOT NULL` del esquema: patente del camión, nombre y capacidad de cada cisterna, nombre y apellido del chofer, razón social del cliente y nombre del producto. Si la entidad se reutiliza por código externo, no se exigen.
- **Entidades existentes:** si vienen con un código externo que ya está en la BD, se **reutilizan sin actualizar**; si no existen (o no traen código externo), se crean. Cada entidad se resuelve en su propio método de `OrdenBusiness` (`obtenerOCrearCamion`, `obtenerOCrearChofer`, `obtenerOCrearCliente`, `obtenerOCrearProducto`).
- **Se busca primero por código externo** porque es el identificador de integración que garantiza SAP (único y estable); razón social y nombre no son únicos en el esquema y el documento es opcional. La búsqueda por **identificador alternativo** cuando no viene el código externo (patente, documento, razón social, nombre) **queda para el ticket 04** (acordado en grupo, opción A).
- **Hueco conocido hasta el ticket 04:** si llega dos veces una entidad con un `UNIQUE` propio (por ejemplo, la patente del camión) **sin** código externo, la segunda vez se intenta crear y falla con 500.
- **Transacción única** (`@Transactional(rollbackFor = Exception.class)`): entidades + orden en estado 1 con fecha de recepción + log NULL → 1 con el usuario logueado. El `rollbackFor` es necesario porque nuestras excepciones son *checked* (`extends Exception`) y Spring, por defecto, solo hace rollback con `RuntimeException`.

### Impacto en otros tickets

- **04 (sincronización):** la sincronización completa va dentro de los métodos `obtenerOCrear...` de `OrdenBusiness` (actualizar datos, buscar por identificador alternativo, reemplazar cisternas). Para reemplazar las cisternas conviene agregar `orphanRemoval = true` en `Camion.cisternas`. Hoy un choque de `unique` (por ejemplo, una patente repetida) termina en 500; el 04 lo convierte en 409/400.
- **05, 06, 07, 08, 09:** buscar la orden con `OrdenRepository.findByNumeroOrden` (los endpoints usan el número de orden, no el id interno). Registrar cada transición en `orden_estado_log` igual que en el 03 (estado anterior, nuevo, fecha, usuario logueado) y dentro de la misma transacción, con `@Transactional(rollbackFor = Exception.class)`. La entidad `Orden` ya tiene todas las columnas de los puntos 2 a 5.
- **10 (Postman):** el request de Postman del punto 1 se arma en el 10 (acordado en grupo); el JSON de ejemplo está en Swagger (`OrdenRestController.EJEMPLO_ORDEN`). En el 10 también va la prueba de roles: un usuario sin rol OPERADOR ni ADMIN recibe 403 en `POST /api/v1/ordenes`.
