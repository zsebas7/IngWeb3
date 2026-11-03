# 10: Circuito completo en Postman

**Responsable:** Sebastián Caparroz · **Esfuerzo:** M (2)

**What to build:** una colección de Postman, versionada en el repo, que unifica los requests de todos los tickets y simula el circuito completo de una orden con tests automáticos, ejecutable de punta a punta con el Collection Runner.

**Blocked by:** 09 (Pesaje final y conciliación). La colección puede irse armando a medida que avanzan los demás tickets.

**Status:** ready-for-agent

- [ ] Un entorno de Postman con la URL base y variables (token, número de orden, contraseña)
- [ ] Circuito encadenado: login → recepción de orden → pesaje inicial → validación de contraseña → varias lecturas → cierre → pesaje final → consulta de conciliación
- [ ] Cada request tiene tests que verifican el código HTTP y los datos clave (estado, preset, valores de la conciliación)
- [ ] Incluye casos de error representativos (duplicado, estado inválido, contraseña incorrecta, lectura descartada)
- [ ] El circuito pasa completo contra un entorno recién levantado con Docker (número de orden generado dinámicamente para poder repetirlo)
