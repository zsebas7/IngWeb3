# 08: Cierre de orden (punto 4)

**Responsable:** Sebastián Caparroz · **Esfuerzo:** S (1)

**What to build:** un operador cierra una orden en carga cuando verifica que terminó. La orden pasa a estado 3 y desde ese momento no acepta más lecturas del caudalímetro.

**Blocked by:** 07 (Recepción de datos de carga)

**Status:** ready-for-agent

- [ ] Orden en estado 2 con al menos una lectura recibida → estado 3 y log 2 → 3 con usuario, en una transacción
- [ ] Orden en estado 2 sin ninguna lectura → 409
- [ ] Orden inexistente → 404; otro estado → 409
- [ ] Una lectura enviada a una orden cerrada es rechazada con 409
- [ ] Endpoint documentado en Swagger y request en Postman
