# Economy Items Plugin

Plugin para Paper 1.21.x (compilado y probado en 1.21.11) que define monedas físicas visibles en el inventario (Cobre, Plata, Oro y Platino) con identificadores persistentes. Las monedas son incrafteables, se bloquean en mesas y autócraters si alguien intenta usarlas, solo pueden emitirse mediante comandos administrativos y se apilan hasta 99 unidades por stack.

Monedas disponibles:
- **Moneda de Cobre** (`copper-coin`, `cooper-coin`) — usa `Copper Nugget` para representar microtransacciones.
- **Moneda de Plata** (`silver-coin`, `iron-coin`) — utiliza `Iron Nugget` como cuerpo metálico.
- **Moneda de Oro** (`gold-coin`) — se muestra con `Gold Nugget` para las operaciones más valiosas.
- **Moneda de Platino** (`platinum-coin`, `platino`) — usa `Nether Star` como cuerpo premium.

## Requisitos
- Java 21
- Gradle 9+ (se incluye wrapper)
- API de Paper 1.21.11 (resuelta automáticamente vía Gradle Version Catalog)
- Servidor Paper 1.21.11 para la ejecución final

## Comandos
- `/economy list` — muestra las monedas disponibles y su identificador.
- `/economy give <jugador|@a> <moneda> <cantidad>` — entrega la moneda indicada al jugador o a todos los conectados (`@a`), repartiendo automáticamente múltiples stacks de hasta 99 unidades. Requiere el permiso `economy.admin`.
- `/economy remove <jugador>` — elimina todas las monedas (cualquier item con sufijo `-coin`) del inventario del jugador indicado.
- `/economy add <id> <material> <color> <nombre>` — registra una nueva moneda en `currencies.yml`, normaliza el identificador y agrega automáticamente el sufijo `-coin`.
- `/economy reload` — recarga `plugins/EconomySystem/currencies.yml` sin reiniciar el servidor.

## Compilación
```
./gradlew build
```
El artefacto listo para producción se ubica en `build/libs/EconomySystem-${version}.jar`.

## Pruebas locales
El proyecto incluye el plugin `run-paper` para iniciar un servidor de pruebas rápido en Paper 1.21.11:
```
./gradlew runServer
```
Esto descargará Paper 1.21.11 y cargará el plugin automáticamente dentro del proyecto principal. Si necesitas iniciar otro servidor externo, reutiliza el `.jar` generado desde `build/libs` dentro de tu instancia 1.21.11 favorita.

## Desarrollo
- El código fuente se encuentra en `src/main/java`.
- `plugin.yml` se genera con la versión del proyecto automáticamente durante `processResources`.
- Ajusta `plugins/EconomySystem/currencies.yml` para modificar nombres, degradados, materiales, lore y alias sin recompilar. El archivo se crea automáticamente la primera vez que se ejecuta el plugin y toma como base los valores por defecto definidos en el proyecto.
