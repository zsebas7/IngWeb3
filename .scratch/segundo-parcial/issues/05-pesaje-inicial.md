# 05: Pesaje inicial (punto 2)

**Responsable:** Mirko Bubica · **Esfuerzo:** S (1)

**What to build:** un operador registra la tara de una orden pendiente. El sistema guarda la tara y su fecha/hora, genera una contraseña de activación aleatoria de 5 dígitos, pasa la orden a estado 2 y registra la transición en el log.

**Blocked by:** 03 (Recepción de orden con entidades nuevas)

**Status:** ready-for-agent

- [ ] Con la orden en estado 1 y tara > 0: guarda tara y fecha, genera contraseña entre 10000 y 99999, estado 2, log 1 → 2 con usuario, todo en una transacción
- [ ] La respuesta incluye la contraseña de activación generada
- [ ] Orden inexistente → 404; orden en otro estado → 409; tara ≤ 0 o faltante → 400
- [ ] Endpoint documentado en Swagger y request en Postman que guarda la contraseña en una variable
