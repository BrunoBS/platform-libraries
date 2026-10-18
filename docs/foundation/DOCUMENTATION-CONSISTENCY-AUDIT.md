# Auditoria de consistência — Foundation e Golden Reference

Data: 2026-10-18. Estado: **em andamento**. Esta fase antecede o inventário de classes e a homologação das Engineering Skills.

## Repositórios e precedência

- `BrunoBS/platform-libraries`: Foundation consolidada; o reactor, os POMs dos módulos, o BOM, o código e os testes são evidência da implementação atual.
- `BrunoBS/account-service`: consumidor Golden Reference, artefato `workspace-service`; verificar consumo real da Foundation, arquitetura, testes e contratos.
- `BrunoBS/platform-build`: repositório legado; não usar exemplos de coordenadas `com.empresa.platform` nem `platform-crud` como padrão vigente.
- ADRs e documentação vigente descrevem decisões e intenção; quando divergem do código, registrar achado e decidir explicitamente, sem corrigir código automaticamente.

## Achados iniciais

| ID | Origem | Evidência | Estado | Ação |
| --- | --- | --- | --- | --- |
| DOC-001 | platform-libraries/README.md | Árvore de módulos não listava `modules/platform-message-queue`, presente no reactor e no BOM | Corrigido nesta branch | Acrescentado módulo à árvore |
| DOC-002 | platform-libraries/README.md | Diagrama de dependências não mostrava `platform-message-queue -> platform-messaging`, declarada no POM | Corrigido nesta branch | Acrescentada dependência |
| DOC-003 | platform-build/README.md | Coordenadas `com.empresa.platform` e `platform-crud` representam Foundation antiga | Pendente | Revisar aviso de legado no repositório de origem, sem apagar histórico |
| DOC-004 | platform-build/README.md vs platform-libraries/platform-dependencies/pom.xml | Versões documentadas na Foundation antiga não representam baseline consolidado (por exemplo JSON Schema Validator 1.5.5 vs 3.0.7) | Pendente | Marcar README legado e vincular BOM vigente |
| DOC-007 | docs/foundation/FOUNDATION-ARCHITECTURE.md | Lista parcial de módulos e testing-core único não refletem a estrutura atual com testing dividido e message-queue/redis | Contido | Aviso de vigência histórica inserido; reescrita canônica ainda pendente |
| DOC-008 | docs/foundation/FOUNDATION-VERSIONING.md | Declara groupId `br.com.portalmanager.platform` e que parent/dependencies não usam `${revision}`, enquanto POMs atuais usam `br.com.portalmanager.platform.library` e `${revision}` | Contido | Aviso de vigência histórica inserido; revisão integral pendente |
| DOC-009 | docs/foundation/FOUNDATION-CHECKPOINT.md | Gates históricos ainda desmarcados apesar de código atual com namespace novo; não comprovam estado de publicação atual | Contido | Aviso de caráter histórico; verificar CI/publicação/consumo antes de encerrar |
| DOC-010 | platform-libraries/README.md vs modules/platform-catalog/pom.xml | Diagrama omitia dependências de `platform-catalog` com `platform-authorization` e `platform-schema-validation` | Corrigido nesta branch | Diagrama alinhado às dependências Maven de runtime |
| DOC-011 | modules/platform-testing/pom.xml e README | `platform-testing-aggregator` é POM, não JAR; oito módulos de teste consumíveis | Conferido | Preservar distinção nas receitas de consumo |
| DOC-012 | account-service/src/main/resources/application.yml vs modules/platform-schema-validation/README.md | Consumidor declara `platform.schema-validation.enabled` e `platform.schema-validation.datasource.enabled` apesar de README afirmar que não existem; view está aninhada em `datasource.view-name` em vez de `platform.schema-validation.view-name` | Aberto — potencial impacto funcional | Verificar binding real de `PlatformSchemaValidationProperties`, profiles e testes (README confirma `view-name` na raiz e ausência de `enabled`); corrigir consumidor em PR próprio após prova |
| DOC-013 | account-service/src/main/resources/application.yml vs modules/platform-authorization/README.md | Fallback `PLATFORM_AUTHORIZATION_MODE:MOCK` e `spring.profiles.active: local` no YAML principal; modo REAL é o padrão seguro descrito pela library | Aberto — risco de configuração | Conferir deploy/profiles, impedir MOCK involuntário em ambientes não locais, validar testes |
| DOC-014 | account-service/README.md vs modules/platform-schema-validation/README.md | README da library documenta explicitamente tanto `@ValidateResourceSchemaRequest` (HTTP antes da desserialização) quanto `@ValidateResourceSchema` (use case); não existe contradição documental entre esses dois contratos | Parcialmente esclarecido | Conferir aplicação efetiva por fluxo no consumidor e necessidade de ambos |
| DOC-005 | platform-libraries e account-service | Comparar cada capability consumida, configuração, auto-configuração, testes, exemplos e documentação | Não auditado integralmente | Fazer confronto módulo a módulo |
| DOC-006 | account-service | Verificar README, POM, pacote, macrozonas, endpoints, schema, migrations, testes e docs de refinamento contra código atual | Não auditado integralmente | Fazer confronto por zona e módulo |

## Método e gates da fase 1

1. Mapear todos os módulos dos POMs reactors, BOM e artefatos realmente produzidos; comparar com READMEs e ADRs.
2. Confrontar contratos públicos das capabilities com documentação, testes e uso no `account-service`.
3. Revisar cada macrozona do serviço (`foundation`, `core`, `feature`, `entrypoint`) e seu fluxo com as bibliotecas.
4. Classificar cada divergência: documentação obsoleta, código divergente, contrato inconsistente ou decisão pendente; registrar arquivo, evidência, impacto e ação.
5. Corrigir apenas divergências com evidência inequívoca, em PRs isolados. Alterações de comportamento, API, migrations e arquitetura exigem revisão e testes específicos.
6. Validar checks de documentação, Maven e CI pertinentes; registrar execução real, não presumir sucesso.
7. Concluir fase 1 somente quando inconsistências conhecidas estiverem corrigidas ou justificadas e os documentos vigentes identificados.

**Fase 2 bloqueada até o fechamento da fase 1:** inventário completo classe a classe, método a método, classificação MUST/SHOULD/MAY/DOMAIN e integração com as 17 Skills.

## Escopo e limites da primeira rodada

Conferidos os READMEs e POMs raiz dos três repositórios, os documentos FOUNDATION-ARCHITECTURE, FOUNDATION-VERSIONING e FOUNDATION-CHECKPOINT, os POMs `platform-parent`, `platform-dependencies`, `platform-libraries-bom` e `modules/platform-message-queue`. Também conferidos `account-service/src/main/resources/application.yml`, READMEs de authorization/schema-validation/catalog e guia de uso de audit. Também conferidos POMs de starter, authorization, audit, catalog, schema-validation, redis, message-queue e testing, além dos READMEs de message-queue, redis, audit e testing. A pesquisa textual no índice GitHub não retornou ocorrências para as classes buscadas; isso não comprova ausência. A verificação da implementação exige navegação por caminhos reais ou checkout. Ainda não foi realizada varredura de todas as classes nem execução local de Maven/Testcontainers.
