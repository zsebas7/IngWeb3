# 06: Validación de contraseña de activación (3.a)

**Responsable:** Mirko Bubica · **Esfuerzo:** S (1)

**What to build:** un operador ingresa el número de orden y la contraseña de activación; si son correctos y la orden está en estado 2, recibe el preset para iniciar la carga.

**Blocked by:** 05 (Pesaje inicial)

**Status:** ready-for-agent

- [ ] Orden en estado 2 y contraseña correcta → 200 con `{"preset": n}`
- [ ] Se puede validar más de una vez mientras la orden siga en estado 2
- [ ] Orden inexistente → 404; orden en otro estado → 409; contraseña incorrecta → 401
- [ ] No modifica el estado de la orden ni escribe en el log
- [ ] Endpoint documentado en Swagger y request en Postman
