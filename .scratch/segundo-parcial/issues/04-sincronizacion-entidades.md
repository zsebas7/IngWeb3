# 04: Sincronización de entidades existentes

**Responsable:** Sebastián Caparroz · **Esfuerzo:** M (2)

**What to build:** cuando llega una orden cuyas entidades ya existen, se reutilizan y, si cambiaron, se actualizan. Cada entidad se busca primero por código externo y, si no viene, por su identificador alternativo: camión por patente, chofer por documento, cliente por razón social, producto por nombre.

**Blocked by:** 03 (Recepción de orden con entidades nuevas)

**Status:** ready-for-agent

- [ ] Una entidad existente (encontrada por código externo o identificador alternativo) se reutiliza y sus datos se actualizan con los recibidos
- [ ] Las cisternas de un camión existente se reemplazan por completo por las recibidas
- [ ] Si una entidad no trae ni código externo ni identificador alternativo → 400 y no se crea la orden
- [ ] Si el código externo de un camión existe pero la patente recibida pertenece a otro camión → 409 y no se crea la orden
- [ ] Toda la recepción (sincronización + orden + log) es una única transacción: ante cualquier error no queda nada a medio guardar
- [ ] Requests de Postman que cubren: entidad existente actualizada, sin identificador (400) y conflicto de patente (409)
