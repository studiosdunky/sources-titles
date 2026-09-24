# sync-titles-multi-version

Snapshot do sistema de títulos do hTitles que funciona igual para clientes 1.8 (ViaRewind) e modernos.

## Como funciona

Cada jogador com título ganha uma pilha **fake, só em pacotes (NMS)**, nada existe no mundo do servidor:

```
jogador → slime invisível → armor stand marker invisível com custom name (o título)
```

- A pilha é a mesma para todos os viewers (mesmos entity ids). Só muda o metadata do slime por viewer:
  - cliente `< 1.9` (legacy): slime size 2
  - cliente `>= 1.9` (moderno): slime size 1 + atributo `scale` 0.6 (1.20.5+)
- A montaria (`SetPassengers`) é feita client-side, então o título acompanha o jogador sem teleporte e sem atraso.
- `PlayerTrackEntityEvent` / `PlayerUntrackEntityEvent` mostram/escondem a pilha para cada viewer (join, alcance, troca de mundo, respawn, teleporte).
- `PlayerUseUnknownEntityEvent`: hit no slime/armor stand vira ataque no dono.
- Espectador não mostra título.

## Alturas validadas em jogo

Ver `titles.live.yml` (config usada no Lobby quando a altura foi aprovada):

| cliente | slime size | scale |
|---------|-----------|-------|
| legacy (< 1.9) | 2 | - |
| moderno (>= 1.9) | 1 (config `0` era travado em 1) | 0.6 |

## Arquivos principais

- `src/main/java/.../TitleStack.java` — pacotes da pilha fake
- `src/main/java/.../TitleDisplay.java` — ciclo de vida e eventos
- `src/main/java/.../ClientVersions.java` — detecção legacy via ViaVersion API

## Build

Depende do `hCore` em `../sources/hCore` e do paperweight-userdev (dev bundle Paper 26.2).

```
./gradlew shadowJar
```
