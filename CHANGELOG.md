# Changelog

O que muda em cada versão. O jar de cada versão fica nas
[Releases](https://github.com/fernandoabf/cobblemon-spawn-alerts-fabric/releases).

## 1.3.0 — 2026-10-29

- **A frase de perto também aparece no alto da tela** (`nearScreen`, ligado por padrão): no modo sentido, quem
  está perto do spawn vê a frase numa barra no topo da tela por alguns segundos (`nearScreenSeconds`, 6 por padrão),
  com o som, além do chat. A cor da barra muda com o tipo: roxo para lendário e mítico, rosa para Mega, azul para
  ultracriatura, verde para paradoxo, amarelo para shiny e vermelho para Alfa. Só o servidor precisa do mod.

## 1.2.0 — 2026-10-25

- **Modo sentido** (`"mode": "sense"`, desligado por padrão): no lugar do aviso com o nome, o jogador recebe
  uma frase sorteada que não diz qual Pokémon, nem o bioma, a coordenada ou o que ele tem de raro, como se
  sentisse: "Você sente uma presença poderosa por perto…". Quem está perto (`senseRadius`, 64 blocos por
  padrão, na mesma dimensão) recebe a frase de perto; nos mais raros (`senseEveryone`: lendário, mítico,
  Mega, ultracriatura e paradoxo, por padrão), todo o resto do servidor recebe uma frase de longe ("Uma
  presença poderosa despertou em algum lugar do mundo…"). Shiny e Alfa só chegam a quem está perto.
- Um Pokémon que é várias coisas usa a mais notável: lendário, mítico, Mega, ultracriatura, paradoxo, shiny,
  Alfa.
- As frases ficam na config (`sensePhrases`, de perto e de longe para cada tipo, em português) e o estilo
  também (`senseStyle`, cinza em itálico). Quem está perto ouve um sino de ametista baixinho (`playSound`).
- O log do servidor continua com a linha completa (Pokémon, bioma e coordenadas), para a staff.
- `/spawnalerts test` mostra também as frases que os jogadores receberiam no modo sentido.

## 1.1.0 — 2026-10-18

- **Megas selvagens não são anunciados** (`announceMega`, desligado por padrão): um Pokémon numa forma Mega
  do Mega Showdown, posto no mundo por um datapack, fica de surpresa para quem achar, mesmo sendo Alfa ou
  shiny. Com `announceMega` ligado, ele é anunciado como qualquer outro.

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
