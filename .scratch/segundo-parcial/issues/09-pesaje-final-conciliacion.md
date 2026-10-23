# 09: Pesaje final y conciliación (punto 5)

**Responsable:** Valentina Beas · **Esfuerzo:** M (2)

**What to build:** un operador registra el pesaje final de una orden cerrada; la orden pasa a estado 4 y la respuesta es la conciliación. La conciliación también puede consultarse después mediante un endpoint aparte.

**Blocked by:** 08 (Cierre de orden)

**Status:** ready-for-agent

- [ ] Orden en estado 3 y pesaje final > tara → guarda pesaje final y fecha, estado 4, log 3 → 4 con usuario, en una transacción
- [ ] Pesaje final ≤ tara o faltante → 400; orden inexistente → 404; otro estado → 409
- [ ] La conciliación incluye: pesaje inicial (tara), pesaje final, producto cargado (última masa acumulada), neto por balanza (final − tara), diferencia balanza vs. caudalímetro (neto − producto cargado) y promedios de temperatura, densidad y caudal sobre el detalle de carga
- [ ] El endpoint de consulta devuelve la conciliación solo para órdenes en estado 4 (otro estado → 409, inexistente → 404)
- [ ] Endpoints documentados en Swagger y requests en Postman
