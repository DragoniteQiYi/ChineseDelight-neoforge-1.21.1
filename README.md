中华乐事 (Chinese Delight)
=======

A Minecraft mod about Chinese cuisine for **NeoForge 1.21.1**.

Adds Chinese-style ingredients, cookware and dishes: crops, a stone mill, an iron pot,
a fermenting jar, an iron pan, a kitchen knife and a chef hat.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.251 |
| Java | 21 |

## Building

```bash
./gradlew build
```

The output jar is written to `build/libs/`.

## Running in development

```bash
./gradlew runClient
./gradlew runServer
```

If Gradle cannot find Java, set `JAVA_HOME` to a JDK 21 installation first.

## Mapping names

By default the MDK is configured to use the official mapping names from Mojang for methods
and fields in the Minecraft codebase. These names are covered by a specific license. All
modders should be aware of this license. For the latest license text, refer to the mapping
file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## Additional resources

Community documentation: https://docs.neoforged.net/
NeoForged Discord: https://discord.neoforged.net/
