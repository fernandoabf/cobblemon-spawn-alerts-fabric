# Changelog

O que muda em cada versão. O jar de cada versão fica nas
[Releases](https://github.com/fernandoabf/cobblemon-spawn-alerts-fabric/releases).

## 1.0.0 — 2026-10-18

Primeira versão, para o Cobblemon 1.8.2 no Fabric 1.21.1.

- **Aviso de spawn raro**, com um sininho: "[Spawn] Um Pikachu ✦ Shiny apareceu em Floresta de
  Bétulas!". Por padrão diz **qual Pokémon e em qual bioma, nunca as coordenadas**.
- **Cada servidor escolhe** em `config/spawnalerts.json`:
  - **o que avisar:** shiny, Alfa, lendário, mítico, ultracriatura e paradoxo (cada um liga e
    desliga), espécies a mais (`extraSpecies`), espécies que nunca avisam (`ignoredSpecies`) ou todo
    spawn (`announceAll`, que lota o chat);
  - **quanto da localização:** bioma (`showBiome`), coordenadas exatas (`showCoordinates`) e o
    jogador mais perto (`showNearestPlayer`);
  - **quem recebe:** todo o servidor, só a mesma dimensão (`sameDimensionOnly`) ou só quem está num
    raio (`radius`); no chat ou na barra de ação (`actionBar`); com ou sem som;
  - **os textos:** a mensagem e as etiquetas, com cores `&`.
- `/spawnalerts reload` recarrega a config sem reiniciar; `/spawnalerts test` mostra para você como
  fica a mensagem (só operadores).
- O nome do Pokémon e do bioma aparecem no idioma de cada jogador (traduções do Cobblemon e do
  jogo); bioma sem tradução, de datapack, aparece com o id legível.
- Spawn cancelado por outro mod não é anunciado.
- Roda só no servidor: os jogadores entram sem ter o mod.
