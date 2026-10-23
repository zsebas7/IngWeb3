# 02: Login JWT y roles

**Responsable:** Valentina Beas · **Esfuerzo:** L (3)

**What to build:** un usuario precargado (admin, operador o caudalimetro) hace login con usuario y contraseña y recibe un token JWT. Con ese token puede consumir los endpoints que su rol permite; sin token recibe 401 y con un rol sin permiso recibe 403. Swagger es accesible sin login. Cualquier endpoint puede obtener el usuario logueado para registrarlo en el log de estados.

**Blocked by:** 01 (Base común de la API)

**Status:** ready-for-agent

- [ ] Las entidades de usuario y rol mapean las tablas `usuario`, `rol` y `usuario_rol` y pasan la validación de Hibernate (`ddl-auto=validate`)
- [ ] El endpoint de login es público y devuelve un JWT ante credenciales correctas; 401 ante credenciales incorrectas o usuario deshabilitado
- [ ] Las peticiones con un token válido quedan autenticadas con el usuario y sus roles (`ROLE_ADMIN`, `ROLE_OPERADOR`)
- [ ] Sin token o con token inválido/vencido → 401; rol insuficiente → 403, ambos con el formato de error común
- [ ] Swagger UI y la definición OpenAPI son accesibles sin autenticación, y permiten cargar el token para probar
- [ ] Los controllers disponen de una forma común de obtener el usuario logueado
- [ ] Request de login agregado a la colección de Postman, guardando el token en una variable
