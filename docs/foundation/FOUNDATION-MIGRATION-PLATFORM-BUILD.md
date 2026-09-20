# Foundation Migration — platform-build

## Antes

```text
platform-build
├── platform-parent
└── platform-dependencies
        ↓
platform-libraries
        ↓
consumers
```

Havia dois package repositories no fluxo oficial.

## Depois — estrutura alvo

```text
platform-libraries
├── platform-parent
├── platform-dependencies
├── platform-libraries-bom
└── capabilities
        ↓
consumers
```

## Arquivos migrados

Do conteúdo de `BrunoBS/platform-build` foram migrados para `BrunoBS/platform-libraries`:

- `platform-parent/pom.xml`;
- `platform-dependencies/pom.xml`.

A governança de build foi preservada e o destino de publicação alterado para o repository de `platform-libraries`.

## Arquivos adicionados

- `platform-libraries-bom/pom.xml`;
- `docs/foundation/FOUNDATION-CONSOLIDATION-INVENTORY.md`;
- `docs/foundation/FOUNDATION-VERSIONING.md`;
- `docs/foundation/FOUNDATION-PUBLISHING.md`;
- `docs/foundation/FOUNDATION-MIGRATION-PLATFORM-BUILD.md`;
- `docs/adr/ADR-001-CONSOLIDATE-FOUNDATION-REPOSITORIES.md`.

## Arquivos modificados

- root `pom.xml`;
- POMs das oito capabilities;
- `.github/maven-settings.xml`;
- `.github/maven-publish-settings.xml`;
- workflows verify/publish;
- documentação arquitetural da Foundation.

## Versões planejadas

- platform-dependencies: `1.0.3`;
- platform-parent: `1.1.0`;
- platform-libraries-bom: `1.1.0`;
- release train das capabilities: `1.1.0`.

Nenhuma dessas versões deve ser registrada como publicada enquanto o deploy remoto não concluir.

## Registry

Alvo oficial:

`https://maven.pkg.github.com/brunobs/platform-libraries`

Registry a remover do fluxo oficial após o checkpoint:

`https://maven.pkg.github.com/brunobs/platform-build`

## Migração de consumidor — após publicação

Consumidores existentes deverão:

1. usar `platform-parent:1.1.0`;
2. importar `platform-libraries-bom:1.1.0`;
3. declarar capabilities sem versão;
4. configurar apenas o registry de `platform-libraries`;
5. executar `mvn clean verify` em checkout limpo.

## Estado do platform-build

O repositório ainda não é obsoleto operacionalmente porque seus packages Maven existentes continuam necessários para consumidores atuais.

Ele não deve ser apagado nem arquivado antes da conclusão do deploy consolidado e do teste remoto do `account-service`.

Após o checkpoint, deverá ser marcado como legado/obsoleto e arquivado manualmente.


## Evidência de migração — estado atual

O reactor consolidado foi validado no Verify #60, run `35537837948`.

As capabilities `1.1.0` foram publicadas no registry alvo no run `35538290455`.

Os artifacts estruturais ainda não completaram o cutover. Um probe Maven standalone no run `35538856486` confirmou que `platform-dependencies:1.0.3` não é resolvido a partir de `platform-libraries`, enquanto a tentativa de deploy para esse destino retorna HTTP 422.

O `account-service` já possui uma branch de prova com o POM alvo, mas os runs `35538639721` e `35538758316` falham em `platform-parent:1.1.0`. Portanto essa branch não representa ainda uma migração consumível.

Nenhum package legado foi removido. `BrunoBS/platform-build` continua necessário operacionalmente até a decisão e execução do cutover dos packages Maven estruturais.
