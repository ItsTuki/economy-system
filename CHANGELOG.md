# Changelog

## 2.0.0 — 2026-11-06

### Actualización
- Compatible con **Minecraft / Paper 26.3** (antes 1.21.11). Requiere **Java 25**.
- Gradle 9.9, run-paper 3.1, Adventure 5.

### Nuevo
- Comandos con **Brigadier**: autocompletado nativo, validación de argumentos y selectores (`@a`, `@p`, `@r`…).
- `/economy take` — quitar una cantidad concreta.
- `/economy remove <jugador> [moneda]` — ahora acepta varios jugadores y filtrar por moneda.
- `/economy balance <jugador>` y `/economy info`.
- Comandos de jugador `/coins`: saldo, `exchange` (con vuelto), `condense` y `pay`.
- Campo `value` por moneda (cobre 1, plata 10, oro 100, platino 1000) para cambio y saldo.
- `item-model` (resource packs) y `glow` por moneda.
- `config.yml` con tamaño de stack, límite por comando, inventario lleno, avisos, log y distancia de pago.
- `messages.yml` traducible con MiniMessage.
- Permisos por subcomando (`economy.admin.*`, `economy.coins.*`).
- Monedas tiradas al suelo: sin despawn e invulnerables (configurable).
- Protecciones extra: yunque, afiladora, herrería, telar, hornos, aldeanos, colocar bloques y consumir.
- Las monedas existentes se actualizan solas al entrar o con `/economy reload`.
- Banner con colores en la consola al iniciar y apagar el plugin, y créditos ("Creado por ItsTuki") en la ayuda de `/economy` y `/coins`.

### Correcciones
- Cualquier moneda del plugin queda protegida aunque su id no termine en `-coin`.
- Los nombres de las monedas ya no aparecen en cursiva.
- `/economy add` usa permiso propio en lugar de exigir OP.
- Límite de cantidad por comando (antes un número enorme podía llenar el mundo de ítems).
- Las monedas por defecto ya no se vuelven a crear si las borras de `currencies.yml`.
- Un alias ya no puede pisar el id de otra moneda.
- README corregido: la carpeta de datos es `plugins/EconomyItems/`.
- Eliminada la clase de plantilla `Library.java`.

### Compatibilidad
- El nombre del plugin (`EconomyItems`) y la clave interna (`economyitems:currency-id`) no cambian: las monedas de la v1 siguen funcionando.

## 1.0.1
- Versión original (Paper 1.21.11).
