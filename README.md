# EconomyItems

Plugin de **monedas físicas** para servidores **Paper 26.3** (Minecraft 26.3, Java 25).
Las monedas son ítems reales en el inventario: se intercambian en mano, se guardan en cofres y se pierden si mueres, pero **no se pueden fabricar, fundir, colocar ni usar en recetas**. Solo los administradores las emiten.

| Moneda | ID | Material | Valor |
|---|---|---|---|
| Moneda de Cobre | `copper-coin` | Pepita de cobre | 1 |
| Moneda de Plata | `silver-coin` | Pepita de hierro | 10 |
| Moneda de Oro | `gold-coin` | Pepita de oro | 100 |
| Moneda de Platino | `platinum-coin` | Estrella del Nether | 1000 |

Todo es editable en `plugins/EconomyItems/currencies.yml` y se pueden crear monedas nuevas en el juego.

## Instalación

1. Descarga `EconomySystem-<versión>.jar` desde [Releases](../../releases).
2. Cópialo a la carpeta `plugins/` de un servidor Paper 26.3 con Java 25.
3. Inicia el servidor. Se crean `config.yml`, `currencies.yml` y `messages.yml` en `plugins/EconomyItems/`.

**¿Vienes de la v1?** Reemplaza el jar y listo. Las monedas existentes siguen siendo válidas: al entrar cada jugador (o con `/economy reload`) se actualiza su nombre y apariencia. Tu `currencies.yml` se conserva y se le agrega el campo `value`.

## Comandos

Todos tienen autocompletado nativo y aceptan selectores (`@a`, `@p`, `@r`…).

### Administración — `/economy` (alias `/economia`)

| Comando | Descripción | Permiso |
|---|---|---|
| `/economy list` | Monedas disponibles con su valor | `economy.admin.list` |
| `/economy give <jugadores> <moneda> <cantidad>` | Entrega monedas en stacks | `economy.admin.give` |
| `/economy take <jugadores> <moneda> <cantidad>` | Quita una cantidad concreta | `economy.admin.take` |
| `/economy remove <jugadores> [moneda]` | Retira todas las monedas (o solo una) | `economy.admin.remove` |
| `/economy balance <jugador>` | Monedas y valor total de un jugador | `economy.admin.balance` |
| `/economy add <id> <material> <color> <valor> <nombre>` | Crea una moneda nueva | `economy.admin.add` |
| `/economy info` | Datos de la moneda que tienes en la mano | `economy.admin.info` |
| `/economy reload` | Recarga config, mensajes y monedas | `economy.admin.reload` |

`economy.admin` (por defecto OP) incluye todos los anteriores.

Ejemplo: `/economy add rubi EMERALD "#FF0044" 50 Moneda de Rubí` crea `rubi-coin` con valor 50.

### Jugadores — `/coins` (alias `/monedas`)

| Comando | Descripción | Permiso |
|---|---|---|
| `/coins` | Tus monedas y su valor total | `economy.coins.balance` |
| `/coins exchange <de> <a> <cantidad\|all>` | Cambia monedas según su valor y devuelve el vuelto | `economy.coins.exchange` |
| `/coins condense` | Agrupa todas tus monedas en la menor cantidad posible | `economy.coins.condense` |
| `/coins pay <jugador> <moneda> <cantidad>` | Entrega monedas a otro jugador | `economy.coins.pay` |

`economy.coins` (por defecto todos) incluye los anteriores.

## Configuración

### `config.yml`

| Opción | Defecto | Qué hace |
|---|---|---|
| `coins.max-stack-size` | `99` | Tamaño de stack de las monedas (1–99) |
| `coins.refresh-on-join` | `true` | Actualiza la apariencia de las monedas al entrar |
| `commands.max-amount` | `10000` | Máximo por comando (evita spam de ítems) |
| `commands.drop-when-full` | `true` | Si el inventario está lleno, lo que sobra cae al suelo |
| `commands.notify-targets` | `true` | Avisar al jugador cuando un admin le da o quita monedas |
| `commands.log-transactions` | `true` | Registrar movimientos en la consola |
| `player-commands.exchange` / `condense` | `true` | Activar los comandos de cambio y agrupado |
| `player-commands.pay.enabled` | `true` | Activar `/coins pay` |
| `player-commands.pay.max-distance` | `0` | Distancia máxima para pagar (0 = sin límite) |
| `dropped-coins.never-despawn` | `true` | Las monedas en el suelo no desaparecen |
| `dropped-coins.invulnerable` | `true` | Ni lava, fuego, cactus ni explosiones las destruyen |
| `protection.*` | `true` | Bloqueos: `crafting`, `crafter`, `workstations` (yunque, afiladora, herrería…), `furnaces`, `villager-trades`, `placing`, `consuming` |

### `currencies.yml`

```yaml
currencies:
  gold-coin:
    id: gold-coin                 # No cambiar si ya hay monedas en circulación
    display-name: Moneda de Oro   # Nombre plano para mensajes
    styled-display-name: "<gradient:#B36F0A:#FFF07C><bold>Moneda de Oro"   # MiniMessage o &#RRGGBB
    color: '#FFFF55'
    material: GOLD_NUGGET
    value: 100                    # Valor en unidades base
    item-model: "miservidor:moneda_oro"   # Opcional: modelo de resource pack
    glow: false                   # Opcional: brillo de encantamiento
    lore:
      - "<gray>Una moneda de oro"
    aliases: [oro, gold_coin]
```

### `messages.yml`

Todos los textos usan [MiniMessage](https://docs.advntr.dev/minimessage/format) y se pueden traducir. Deja un mensaje en `""` para desactivarlo.

## Compilar

Requiere JDK 25 (Gradle lo descarga solo si no lo tienes).

```bash
./gradlew build        # jar en build/libs/
./gradlew runServer    # levanta un servidor Paper 26.3 de prueba con el plugin
```

## Licencia

Sin licencia definida todavía.
