# 03: Recepción de orden con entidades nuevas (punto 1)

**Responsable:** Mirko Bubica · **Esfuerzo:** L (3)

**What to build:** un operador logueado envía el JSON de una orden de SAP (número de orden, código externo opcional, camión con sus cisternas, chofer, cliente, producto, fecha de carga prevista y preset). Si las entidades no existen se crean; la orden queda en estado 1 con su fecha de recepción, y la transición queda registrada en el log con el usuario que la hizo. El JSON se interpreta con un deserializador tolerante a distintos nombres de atributos, como el endpoint `/b2b` del repo de referencia.

**Blocked by:** 01 (Base común de la API), 02 (Login JWT y roles)

**Status:** ready-for-agent

- [ ] Las entidades camión, cisterna, chofer, cliente, producto, orden y log de estados mapean el esquema y pasan `ddl-auto=validate`
- [ ] Una orden con entidades inexistentes las crea (camión con todas sus cisternas) y guarda la orden en estado 1 con fecha de recepción
- [ ] Se registra en el log la transición NULL → 1 con fecha y usuario logueado, en la misma transacción que la orden
- [ ] Número de orden repetido, o código externo repetido (si no es NULL) → 409
- [ ] Fecha de carga prevista pasada → 400; faltan datos obligatorios o el JSON es inválido → 400
- [ ] Solo accesible para usuarios autenticados con rol operador o admin
- [ ] Endpoint documentado en Swagger y request de ejemplo en Postman
