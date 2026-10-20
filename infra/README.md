# Entorno Docker

Levanta MySQL 8.4 y el backend. Solo hace falta tener Docker con el plugin `compose`; Java y Maven no son necesarios.

## Primera vez

```bash
cd infra
cp .env.example .env
docker compose up -d --build
```

- Backend: http://localhost:8080
- MySQL: `localhost:3306`, base `gaslogix`, usuario y contraseña según `.env`

La primera vez, MySQL ejecuta `backend/db/schema.sql` y `backend/db/data.sql`. Los usuarios de prueba de la aplicación están en el comentario de `data.sql`.

## Puertos ocupados

Si el 3306 o el 8080 ya están en uso en tu máquina (otro MySQL, otro servicio), cambiá `DB_HOST_PORT` o `BACKEND_HOST_PORT` en `.env`. Solo cambia el puerto del host: dentro de Docker el backend siempre se conecta a `db:3306`.

## Comandos útiles

| Acción | Comando |
| --- | --- |
| Ver logs del backend | `docker compose logs -f backend` |
| Recompilar el backend tras cambiar código | `docker compose up -d --build backend` |
| Detener todo (los datos se conservan) | `docker compose down` |
| Borrar la BD y recrearla desde los scripts | `docker compose down -v` y luego `docker compose up -d --build` |
| Consola MySQL | `docker compose exec db mysql -u gaslogix -p gaslogix` |

**Importante:** los scripts SQL solo se ejecutan cuando el volumen de datos está vacío. Si se modifica `schema.sql` o `data.sql`, hay que recrear la BD con `docker compose down -v`, lo que borra todos los datos cargados.

## Correr el backend desde el IDE

Con solo la BD levantada (`docker compose up -d db`), el backend puede ejecutarse desde el IDE o con `mvn spring-boot:run` en `backend/`. Por defecto se conecta a `localhost:3306` con `gaslogix`/`gaslogix`; si cambiaste el puerto o las credenciales, definí las variables `DB_PORT`, `DB_USER` y `DB_PASS`.
