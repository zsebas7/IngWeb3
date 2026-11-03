# 07: Recepción de datos de carga en tiempo real (3.b)

**Responsable:** Leonel Ruiz Notari · **Esfuerzo:** L (3)

**What to build:** el caudalímetro (usuario técnico autenticado) envía lecturas de una orden identificada solo por su número: masa acumulada (kg), densidad, temperatura (°C) y caudal (kg/h). Cada lectura válida actualiza los últimos valores de la orden; además se guarda en el detalle de carga solo si pasó la frecuencia configurada desde el último detalle guardado.

**Blocked by:** 05 (Pesaje inicial)

**Status:** ready-for-agent

- [ ] Solo se aceptan lecturas con la orden en estado 2; orden inexistente → 404; otro estado → 409
- [ ] Se descarta con 400 y mensaje si: caudal ≤ 0, masa acumulada ≤ 0 o masa acumulada menor que el último valor
- [ ] Cada lectura válida sobrescribe los últimos valores de la orden con el timestamp del servidor
- [ ] La primera lectura válida fija la fecha de inicio de carga; cada lectura válida actualiza la fecha de fin de carga
- [ ] La primera lectura válida siempre se guarda en el detalle; las siguientes solo si pasaron `carga.detalle.frecuencia-segundos` desde el último detalle guardado
- [ ] Se aceptan lecturas con masa mayor al preset; densidad y temperatura solo deben venir
- [ ] Endpoint documentado en Swagger y requests en Postman (lectura válida y cada caso de descarte)
