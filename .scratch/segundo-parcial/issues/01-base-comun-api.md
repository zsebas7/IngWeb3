# 01: Base común de la API

**Responsable:** Leonel Ruiz Notari · **Esfuerzo:** M (2)

**What to build:** la base compartida que usan todos los endpoints: rutas y estados de la orden centralizados como constantes, excepciones de negocio que se traducen a respuestas HTTP claras con un formato de error uniforme, y la documentación OpenAPI (Swagger) configurada con el nombre del proyecto. Sigue el estilo del repo de referencia del profesor (ver `docs/decisiones.md`).

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Existen constantes para la ruta base de la API (`/api/v1`) y para los 4 estados de la orden (1 a 4), con la nomenclatura `int ESTADO_...`
- [ ] Hay excepciones de negocio para: dato no encontrado (404), duplicado (409), estado inválido (409), dato inválido (400), contraseña incorrecta (401) y error interno (500)
- [ ] Toda respuesta de error tiene el mismo formato JSON (mensaje y código; stack trace solo si `dev.info.enabled=true`)
- [ ] La configuración de OpenAPI define título, descripción y el esquema de seguridad Bearer JWT
- [ ] El proyecto compila y levanta con `docker compose up -d --build`
