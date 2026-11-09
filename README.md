# GasLogix

Backend para gestionar la carga de camiones cisterna con GLP en una planta de despacho. El sistema recibe las órdenes de carga desde el sistema externo de gestión (SAP), registra los pesajes en balanza, recibe en tiempo real los datos del caudalímetro durante la carga y, al finalizar, calcula la conciliación entre lo que marcó la balanza y lo que registró el caudalímetro.

Trabajo práctico de la materia **Ingeniería Web 3**.

## Circuito de una orden

Cada orden pasa por cuatro estados. Cada cambio de estado queda registrado en `orden_estado_log`, con la fecha y el usuario que lo hizo.

| Paso | Qué ocurre | Estado resultante |
| --- | --- | --- |
| 1. Recepción | Llega la orden desde SAP con el camión, el chofer, el cliente, el producto, la fecha de carga prevista y el preset (kg a cargar). Las entidades se crean o se actualizan según su código externo. | 1 · Pendiente de pesaje inicial |
| 2. Pesaje inicial | La balanza registra la tara del camión vacío y el sistema genera una contraseña de activación de 5 dígitos. | 2 · Con pesaje inicial |
| 3.a Validación de contraseña | En el surtidor se ingresa la contraseña y el sistema devuelve el preset. | 2 |
| 3.b Datos de carga | El caudalímetro envía lecturas de masa acumulada, densidad, temperatura y caudal. Se descartan las lecturas inválidas y el resto se guarda con una frecuencia configurable. | 2 |
| 4. Cierre | Termina la carga y la orden ya no acepta lecturas. | 3 · Cerrada para carga |
| 5. Pesaje final | La balanza registra el peso del camión cargado y el sistema devuelve la conciliación. | 4 · Finalizada |

La **conciliación** compara el peso neto de balanza (pesaje final − tara) con la masa que registró el caudalímetro. También informa la diferencia entre ambos y los promedios de temperatura, densidad y caudal durante la carga.

## Stack tecnológico

| Área | Tecnología |
| --- | --- |
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 (Spring Web MVC, Tomcat embebido) |
| Persistencia | Spring Data JPA (Hibernate) sobre MySQL 8.4 |
| Seguridad | Spring Security con autenticación JWT (`com.auth0:java-jwt`) y contraseñas con hash BCrypt |
| Documentación de la API | springdoc-openapi (Swagger UI) |
| Utilidades | Lombok, Apache Commons Lang |
| Build | Maven, empaquetado como JAR ejecutable |
| Entorno | Docker y Docker Compose |
| Pruebas de la API | Postman |

## Estructura del repositorio

```
├── backend/          Proyecto Maven (Spring Boot)
│   ├── db/           schema.sql (esquema de la base) y data.sql (roles y usuarios iniciales)
│   └── src/          Código fuente, en el paquete ar.com.gaslogix
├── infra/            docker-compose.yml y .env.example para levantar la base y el backend
├── postman/          Colección y entorno para probar la API
└── docs/             Decisiones de diseño (decisiones.md)
```

El backend está organizado en capas:

- **controllers:** endpoints REST.
- **business:** lógica de negocio y máquina de estados de la orden.
- **model:** entidades JPA, repositorios y deserializador del JSON de SAP.
- **auth / security:** login, JWT y control de acceso por rol.

## Cómo levantarlo

Se necesita Docker con el plugin `compose`. Java y Maven no hacen falta.

```bash
cd infra
cp .env.example .env
docker compose up -d --build
```

- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- MySQL: `localhost:3306`, base `gaslogix`

La primera vez, MySQL ejecuta `backend/db/schema.sql` y `backend/db/data.sql`. En [infra/README.md](infra/README.md) están los comandos para cambiar los puertos, recrear la base y correr el backend desde el IDE.

## Usuarios de prueba

| Usuario | Contraseña | Rol | Uso |
| --- | --- | --- | --- |
| `admin` | `Admin.2026` | `ROLE_ADMIN` | Acceso a todos los endpoints |
| `operador` | `Operador.2026` | `ROLE_OPERADOR` | Recepción, pesajes, validación de contraseña y cierre |
| `caudalimetro` | `Caudal.2026` | `ROLE_INTEGRATION` | Envío de datos de carga |

Son credenciales de desarrollo y se cambian en el despliegue.

## Endpoints

Todos, salvo el login, requieren el header `Authorization: Bearer <token>`.

| Método | Ruta | Descripción | Roles |
| --- | --- | --- | --- |
| POST | `/api/v1/login` | Inicia sesión y devuelve el token JWT (dura 8 horas) | Público |
| POST | `/api/v1/ordenes` | Recepción de la orden | OPERADOR, ADMIN |
| POST | `/api/v1/ordenes/{numeroOrden}/pesaje-inicial` | Registra la tara y genera la contraseña | OPERADOR, ADMIN |
| POST | `/api/v1/ordenes/{numeroOrden}/validar-password` | Valida la contraseña y devuelve el preset | OPERADOR, ADMIN |
| POST | `/api/v1/ordenes/{numeroOrden}/datos-carga` | Recibe una lectura del caudalímetro | INTEGRATION, ADMIN |
| POST | `/api/v1/ordenes/{numeroOrden}/cierre` | Cierra la orden para carga | OPERADOR, ADMIN |
| POST | `/api/v1/ordenes/{numeroOrden}/pesaje-final` | Registra el pesaje final y devuelve la conciliación | OPERADOR, ADMIN |
| GET | `/api/v1/ordenes/{numeroOrden}/conciliacion` | Consulta la conciliación de una orden finalizada | OPERADOR, ADMIN |

Los ejemplos de cada request y sus respuestas están en Swagger UI.

### Códigos de respuesta

| Código | Significado |
| --- | --- |
| 400 | Datos inválidos o lectura descartada |
| 401 | Sin token, token inválido o vencido, credenciales incorrectas o contraseña de activación incorrecta |
| 403 | El rol del usuario no tiene permiso |
| 404 | Orden inexistente |
| 409 | La orden no está en el estado requerido, o hay un dato duplicado |

Todas las respuestas de error usan el mismo formato JSON, con el código y un mensaje.

## Pruebas con Postman

En `postman/` están la colección y el entorno local, y hay que importar los dos en Postman. La colección tiene un login por usuario, que guarda su token en el entorno, y un request por endpoint, en el orden del circuito.

Los valores de cada request se escriben a mano. Para probar los distintos casos se cambian los datos del body, el número de orden de la URL o el token de la pestaña Auth: `{{token}}` para el operador, `{{tokenCaudalimetro}}` para el caudalímetro y `{{tokenAdmin}}` para el admin.

## Integrantes

- Beas, Valentina
- Bubica, Mirko
- Caparroz, Sebastián
- Ruiz Notari, Leonel
