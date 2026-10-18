# CLAUDE.md — Spawn Alerts (Fabric 1.21.1)

Responda e escreva os textos para o usuário em **português do Brasil**. Código, ids e comentários de
código em inglês.

## O que é

Mod para **Cobblemon 1.8.2 no Fabric 1.21.1**: quando aparece um Pokémon raro, avisa qual Pokémon e
onde. Por padrão só o bioma, **sem a localização exata** (pedido do usuário); o usuário também pediu
que **cada servidor escolha tudo na config** (o que avisar, se mostra a localização, quem recebe).
Roda só no servidor (não registra blocos, itens nem pacotes).

- `SpawnAlerts`: assina `CobblemonEvents.ENTITY_SPAWN` com prioridade `LOWEST` (quem cancela o spawn
  roda antes) e filtra `PokemonEntity`. Bioma e bloco vêm do `SpawnablePosition`. Filtro padrão:
  shiny, `isAlpha`, rótulos `legendary`/`mythical`/`ultra_beast`/`paradox`, mais `extraSpecies`, menos
  `ignoredSpecies`. `{location}` junta as partes ligadas (bioma, coordenadas, jogador mais perto);
  `recipients` aplica `sameDimensionOnly`/`radius`.
- `SpawnAlertsCommand`: `/spawnalerts reload` e `/spawnalerts test` (permissão 2), pelo
  `CommandRegistrationCallback` do Fabric API.
- Só servidor ⇒ a mensagem é montada no servidor a partir do modelo da config (pt-BR por padrão);
  o nome do Pokémon (`species.translatedName`) e do bioma (`biome.<ns>.<path>`, com fallback legível)
  vão como chaves de tradução que o client já tem.
- `MessageTemplate`: modelo com códigos `&`/`§` e `{placeholders}` → `Component` com estilos.
- `config/SpawnAlertsConfig`: `config/spawnalerts.json` (Gson sem escape de HTML, para o `&` ficar
  legível).
- `tools/make_icon.py`: desenha o ícone (`py -I tools/make_icon.py <saída>`).

Irmão de [`../cobblemon-ballworks-fabric`](../cobblemon-ballworks-fabric),
[`../cobblemon-falling-apricorns-fabric`](../cobblemon-falling-apricorns-fabric) e
[`../cobblemon-trainer-xp-fabric`](../cobblemon-trainer-xp-fabric): mesmo toolchain, mesma estrutura
de gametests e de releases.

## Ambiente e comandos

Precisa de **JDK 21**. Nesta máquina o `JAVA_HOME` aponta para o JDK 17; use o Temurin 21 só nos
comandos do Gradle:

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"
./gradlew build                  # jar: build/libs/spawnalerts-<versão>.jar
./gradlew runGametest            # servidor de gametest headless com o Cobblemon real; != 0 se falhar
./gradlew runProductionGametest  # os mesmos testes num servidor Fabric de produção (nomes intermediary)
```

- O Gradle baixa o jar oficial do Cobblemon 1.8.2 do Modrinth para `run-libs/` (git-ignored, SHA-1
  conferido). O mod **compila contra ele**.
- Os gametests usam Pokémon reais (`PokemonProperties.parse(...).create()`) e chamam `announce`
  direto: montar um `SpawnablePosition` exige um spawner e uma zona de spawn inteiros.
- Para conferir assinaturas do Cobblemon: `javap -cp run-libs/Cobblemon-*.jar -p <classe>`.

## Regras do projeto

- **Antes de commitar:** `./gradlew build runGametest` tem que passar; antes de uma versão, também o
  `runProductionGametest`. Toda mecânica nova ganha gametest.
- Não commite `run-libs/`, `build/` ou jars de terceiros.
- Licença: **todos os direitos reservados ao fernandoabf** (`LICENSE`): uso do jar original liberado; cópia, modificação, reupload e modpacks só com permissão escrita.
- Commits pequenos, mensagem em inglês.

## Releases

O workflow `.github/workflows/release.yml` publica o jar nas Releases do GitHub quando o
`mod_version` ainda não tem a release `v<mod_version>`: suba o `mod_version`, escreva a seção
`## <versão> — <data>` no `CHANGELOG.md` (pt-BR) e dê push.
