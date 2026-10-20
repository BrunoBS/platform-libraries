# Validação de contratos implementados — Foundation / Golden Reference

> Estado: auditoria baseada em evidências observadas, não homologação integral. Fonte normativa: POMs, código e testes da `platform-libraries` e consumo real em `account-service`. Última revisão: 2026-10-20.

## Política vigente de desenvolvimento

- `1.0.0` é versão deliberadamente fixa durante construção, testes e republicação das bibliotecas; incremento somente quando for definida a transição para produção.
- `platform.authorization.mode=MOCK` e perfil `local` são aceitos na validação atual. Antes do go-live produtivo, configurar obrigatoriamente `REAL`.
- Não confundir execução em infraestrutura semelhante à produtiva para testes com liberação produtiva real.

## Inventário verificado nos POMs

O reactor raiz de `platform-libraries/pom.xml` declara os módulos: `platform-dependencies`, `platform-parent`, `platform-libraries-bom`, `platform-observability`, `platform-messaging`, `platform-redis`, `platform-message-queue`, `platform-schema-validation`, `platform-testing`, `platform-authorization`, `platform-audit`, `platform-catalog`, `platform-tagging`, `platform-starter`.

`modules/platform-testing/pom.xml` é um agregador `pom`, com oito submódulos: `core`, `http`, `authorization`, `kafka`, `redis`, `database`, `cloud-aws`, `cloud-azure`. Não declarar o agregador como biblioteca JAR consumível.

O BOM controla versões de capabilities, inclusive `platform-redis`, `platform-schema-validation` e `platform-tagging`; o `account-service/pom.xml` consome parent e BOM `1.0.0`, além das três capabilities. A Foundation define `-Drevision=1.0.0` em `.mvn/maven.config`.

## Matriz de contratos e evidências

| Contrato | Evidência concreta | Conclusão | Limite da prova |
| --- | --- | --- | --- |
| Schema Validation | `platform-schema-validation/README.md` documenta `fallback-code` e `view-name`; `account-service/src/main/resources/application.yml` usa essas chaves na raiz `platform.schema-validation` | Configuração textual alinhada | Inicialização real com a view e payloads de cada endpoint não inspecionados |
| Tagging / Workspace | `WorkspaceCommandService.create` chama `saveAndFlush` antes de `tags.createInitialTags`; `update` chama `tags.reconcileAndReturnManualTags`; ambos `@Transactional` | Sequência de escrita alinhada ao contrato documentado | Implementação interna do `TagManager` não inspecionada |
| Tagging / Application | `ApplicationCommandService.create/update` segue a mesma sequência; resolve Workspace e Application antes de modificar tags | Sequência de escrita alinhada | Sem prova de autorização de todos os fluxos de consulta por tag |
| Tagging / leitura | README de `platform-tagging` atribui autorização e `ResourceVisibility` ao consumidor | Responsabilidade documentada | Conferir todos os finders e endpoints antes de declarar conformidade |
| Redis | POM de `platform-redis` inclui starter Redis reativo, Jackson 3 e módulo de testes Redis; README documenta `setAll/getAll` como MSET/MGET e operação sem fragmentação automática | Dependências e contrato documental verificados | Sem inspeção de `RedisStore`, execução do MSET/MGET ou consumo completo no serviço |
| Authorization | README separa `@AuthorizationRequired` de `@ResourceVisibility`; YAML consumidor seleciona MOCK nesta fase | Política e configuração coerentes com decisão atual | Verificar anotações/filtros nos endpoints e regra REAL para go-live |
| Arquitetura do consumidor | `GoldenArchitectureTest` possui testes para zonas, direção de dependências, controllers e autorização de facades | Guardrails codificados | A aprovação de um teste não substitui inspeção dos fluxos e regras de negócio |
| CI consumidor | Run `39247602027`, job `verify`: Java 25, preload MySQL e `mvn clean verify` em repositório Maven isolado, todos verdes | Build/testes comprovados para commit associado | Não equivale a validar todos os commits posteriores |
| CI Foundation | Run `39132515600` concluído com sucesso | Pipeline comprovada para commit associado | Não equivale a validar todos os commits posteriores |

## Fluxos reais inspecionados

### Workspace

`core/workspace/usecase/operations/WorkspaceCommandService.java`:

1. `create`: valida entrada, cria Workspace, persiste com `saveAndFlush`, cria tags iniciais e registra conclusão de onboarding quando aplicável.
2. `update`: obtém Workspace ativo para alteração, valida, atualiza atributos e aprovadores, persiste, reconcilia tags.
3. `inactivate`, `restore`, `delete`: consultam tags manuais; `delete` coloca o Workspace em quarentena, não executa remoção física nesse método.
4. Operações de criação, atualização e lifecycle inspecionadas são transacionais; as relevantes usam `@Auditable`.

### Application

`core/application/usecase/operations/ApplicationCommandService.java`:

1. `create`: resolve Workspace ativo para atualização, valida, persiste Application, cria tags e registra primeiro cadastro quando aplicável.
2. `update`: resolve Workspace, obtém Application ativa para atualização, valida, persiste e reconcilia tags.
3. `delete`: busca Application inativa elegível e coloca em quarentena; não realiza exclusão física nesse método.
4. Ações de ciclo de vida são transacionais e auditadas.

## Pendências para homologação completa

- Percorrer código-fonte interno de `TagManager`, `RedisStore` e implementações de `@ResourceVisibility`; não inferir semântica de código apenas do README.
- Identificar e verificar todos os pontos de entrada de Redis, Tagging e Authorization no consumidor, inclusive testes negativos, isolamento por Workspace e paginação.
- Conferir demais módulos da Foundation e zonas do serviço, inclusive migrations e schemas.
- Executar/verificar CI no HEAD efetivo de cada branch após as últimas alterações; distinguir pipelines de PR das de push.
- Confirmar publicação e consumo remoto dos binários `1.0.0` no ciclo de desenvolvimento, sem exigir alteração da versão.
- Validar testes de integração específicos para a view `vw_platform_resource_schemas`.

## Critério de conclusão

Só declarar `CONFORME` para um contrato quando houver evidência da implementação e, quando necessário, teste executado. Registrar `PARCIAL` para evidência incompleta e `NÃO VERIFICADO` quando houver apenas intenção documental. Não declarar auditoria ponta a ponta concluída enquanto houver itens da seção anterior.
