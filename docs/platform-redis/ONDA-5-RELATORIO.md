# Platform Redis V3 — relatório final / Onda 5

Status: **ondas 0–5 concluídas, builds e testes aprovados**. Reactor completo: 22 módulos / 549 testes. Verificação focada posterior dos últimos ajustes: 137 testes (incluindo 39 do módulo Redis). Zero falhas, erros, skips ou flakes. Branch feature/platform-redis-v3, alterações locais disponíveis para revisão, sem publicação ou merge.

## Escopo e preservação

- Repositório BrunoBS/platform-libraries, branch feature/platform-redis-v3 criada a partir da main local (que já tinha um commit à frente do remoto). Nenhum commit existente foi reescrito.
- Workspace/Account Service recebeu checkpoint documental WORKSPACE-PAUSE-PLATFORM-REDIS-V3.md. Configuration continua pausada. Comparação SHA-256 de 784 arquivos confirma preservação após a pausa. Nenhum serviço consumidor foi integrado ou alterado.
- Documento V3 baixado/lido integralmente e copiado em platform-redis-refinamento_V3.md. SHA-256 393de22a940a20584ba19314b0c15a23b8c378442ece054e1f8c7f6308f91e61.
- Onda 0 registrou inspeção e matriz de contratos antes de gerar produção. Ver ONDA-0.md; decisões posteriores de performance foram registradas antes da aplicação em REVISAO-PERFORMANCE.md.

## Funcionalidades entregues

| Área | Entrega |
| --- | --- |
| Maven | Capability independente no reactor e BOM, baseline gerenciado, sem inclusão automática no starter |
| Store | JSON UTF-8, Class/TypeReference Jackson 3, Optional apenas para ausência, TTL/expire/persist/delete/exists, NX atômico |
| API fluida | write().ttl().ifAbsent().execute(), operação single use, false apenas por NX não satisfeito |
| Async | Espelhamento completo por CompletionStage, ReactiveStringRedisTemplate/Lettuce, sem supplyAsync bloqueante |
| Estruturas | Hash, Set, List, Sorted Set, Atomic; matriz completa, leituras limitadas, CAS Lua com TTL preservado |
| Pub/Sub | Publisher independente, N listeners/canais/padrões, herança genérica, lifecycle, reconexão, executor/fila limitados |
| Streams | Append/read/grupos 0/$/ACK/retention, listeners concorrentes, recuperação por janela/cursor e XCLAIM com minIdle |
| Keyspace | Inscrição nativa opcional, desligada por default, sem CONFIG SET em produção |
| Segurança | Namespace ISOLATED/SHARED explícito; limites; TLS/credenciais/timeouts Boot; JSON sem default typing inseguro |
| Exceções | Classes existentes de messaging e bundles PT-BR/EN PLT-REDIS-001..011; causas sanitizadas |
| Diagnóstico | ErrorHandler customizável e contadores sem labels/payload; falhas e saturação observáveis |
| Spring | AutoConfiguration.imports, ConditionalOnMissingBean, reutiliza infraestrutura, sem consumidores sem anotações |
| Organização | Pacotes obrigatórios, package-info, interfaces coesas; sem dependência Message Queue/domínios consumidores |
| CI | Workflow verify inclui feature/**; passos locais equivalentes executados, sem disparar publicação |

## Evidências por onda

| Onda | Resultado do build/testes | Log |
| --- | --- | --- |
| 0 | BUILD SUCCESS; messaging/testing-core, 98 testes | /private/tmp/platform-redis-wave0.log |
| 1 | BUILD SUCCESS; 2 unitários + 7 IT Redis; 107 testes no reactor focado | /private/tmp/platform-redis-wave1.log |
| 2 | BUILD SUCCESS; 2 unitários + 11 IT Redis; 111 no reactor focado | /private/tmp/platform-redis-wave2.log |
| 3 | BUILD SUCCESS; 2 unitários + 16 IT Redis; 116 no reactor focado | /private/tmp/platform-redis-wave3.log |
| 4 | BUILD SUCCESS; 2 unitários + 21 IT Redis; 121 no reactor focado | /private/tmp/platform-redis-wave4.log |
| 5, revisão inicial | BUILD SUCCESS; segurança/reconexão/tipos herdados, depois 4 unitários + 32 IT | /private/tmp/platform-redis-wave5-focused.log; /private/tmp/platform-redis-wave5-focused-final.log |
| Reactor completo inicial | BUILD SUCCESS nos 22 módulos; 546 testes, zero falhas/erros/skips, 6min11, 15/10/2026 00:26:41 | /private/tmp/platform-redis-reactor.log |
| Reactor completo final | BUILD SUCCESS nos 22 módulos; **549 testes**, zero falhas/erros/skips/flakes, 6min09, 15/10/2026 00:35:51; inclui cursor Streams | /private/tmp/platform-redis-final-reactor.log |
| Focado final posterior | BUILD SUCCESS; **137 testes** (98 messaging/testing-core + 5 unitários/arquiteturais e 34 IT Redis), 17.104s, 15/10/2026 00:38:34; valida lifecycle/ACL/metadados finais | /private/tmp/platform-redis-final-module.log |

Integração real via Testcontainers Redis fixado por digest e platform-testing-core/PinnedDockerImage. TLS usa Redis com certificado confiado por SSL bundle Boot e senha, incluindo rejeição de senha inválida. Timeout real por CLIENT PAUSE. Reconexão real por corte de conexão Pub/Sub. Ausência de dados sensíveis verificada em causa sanitizada e em eventos de log capturados. Bootstrapping testado com EnableAutoConfiguration, dependência e propriedades, sem conexão/template/API manual.

Condições concorrentes: um vencedor NX/CAS, contadores sem perda; lote/limites de estruturas, DTO/lista/mapa/genéricos, null/JSON inválido/tipo errado, TTL overwrite e API fluida single use. Streams verifica ACK pós-sucesso, pending após erro/JSON inválido, outro registro do lote continua, claim/reprocessamento, grupos latest/beginning, retenção e avanço do cursor por pending recente/antigo. Lifecycle sem consumidores passivos, stop/restart, fila saturada, múltiplas instâncias e padrões Pub/Sub.

## Problemas encontrados e corrigidos

- Starter reativo não estava no cache: download de dependência gerenciada, sem mudar versão/baseline.
- Coerção Jackson número→String: configuração explícita do codec bloqueia; mapa/DTO mantém tipo solicitado.
- Tipo genérico herdado perdia argumento interno: resolução recursiva de ResolvableType para JavaType, coberta por List<Event> herdado.
- Reexecução do script de edição duplicou dois testes; duplicatas removidas. Literais/imports de testes corrigidos antes do aceite.
- Comando genérico Spring CLIENT KILL escolhia output incompatível: teste usa redis-cli no container para cortar conexão de verdade.
- Espera de recuperação após timeout passou a tolerar o erro transitório durante CLIENT PAUSE, sem ocultar falha definitiva.
- GET externo acima do limite agora é recusado em Lua antes de transmitir valor ao cliente; null JSON escrito como JsonNode também é recusado.
- Namespace ISOLATED com namespace explícito continua incluindo identidade do serviço; compartilhamento exige modo SHARED.
- Geração de workers impede execução antiga de voltar a fazer ACK após stop/start.
- Recuperação XPENDING IDLE poderia varrer backlog integral: substituída por janela/cursor limitada + filtro local + XCLAIM revalidando minIdle. Documentação primária e decisão em REVISAO-PERFORMANCE.md.

- PTTL reativo: Spring getExpire converte persistente em Duration.ZERO; leitura direta de PTTL reativo preserva a semântica Optional (ausente/persistente) sem perder zero legítimo de chave prestes a expirar.
- XPENDING/Lettuce: XPendingArgs ignora flag exclusiva da Range. Usar limite textual exclusivo `(id` aceito pelo protocolo Redis 6.2+, com teste real que atravessa todas as janelas sem repetir limite.

- Declarações de consumer são validadas integralmente antes de alocar/iniciar workers, evitando recursos ativos em bootstrap inválido. NOPERM é classificado junto à falha de autenticação/permissão, sem expor mensagem do servidor.

- Java 25 não executa processadores por descoberta implícita: configuration-processor foi declarado explicitamente no compiler do módulo, com versão do baseline, para gerar metadados IDE. A flag enabled também possui metadata adicional.

## Verificações finais

- XML Surefire/Failsafe e resumos do Maven conferidos: 549 casos no reactor completo; módulo Redis final 39 (5 unitários/arquiteturais, 34 integrações), sem resultados flaky.
- Maven Enforcer / convergence, compilação Java 25, empacotamento e relatórios JaCoCo aprovados. Não executados install/deploy.
- python3 scripts/verify-library-namespace.py aprovado, incluindo arquivos novos; git diff --check e whitespace de arquivos novos aprovados.
- Release train: 20 flattened POMs sem revision não resolvida. Metadata Spring gerado com 15 propriedades, incluindo enabled/namespace/limites; imports de AutoConfiguration e bundles presentes no JAR.
- Workspace/Account Service: SHA-256 dos 784 arquivos de origem/inventário permanecem idênticos após o checkpoint. Branch/HEAD existentes preservados; HEAD da biblioteca e main local continuam em 917aa7b005fe82915b0c366cb4de8882e643beed.

## Decisões e divergências

Não há requisito obrigatório do V3 conscientemente omitido. Baseline de servidor adotado: Redis 6.2+ para limites exclusivos de cursor; testes reais com Redis 7 fixado por digest. Assinaturas avançadas e defaults não fixados pelo refinamento foram definidos na Onda 0. O overload de cursor/RedisStreamRecovery é ampliação aditiva justificada pelo limite de trabalho; não altera consumidor existente, pois este módulo ainda não era publicado. ttl empty mantém ambiguidade permitida (ausente ou persistente). Async não inclui builder, conforme operações diretas do V3. Não foi criada configuração declarativa alternativa de listeners, que era opcional. Keyspace só assina; não administra servidor.

Retenção é exata e configurável. Pub/Sub permanece transitório, sem ACK/retry garantido; saturação descarta tarefa e sinaliza falha. Streams permite reprocessamento: falha/queda entre efeito do handler e ACK exige idempotência da aplicação. minIdle deve superar duração normal do handler; retenção pode remover payload pending. Não há DLQ/exactly-once, transação distribuída, cache local, outbox Redis ou hierarquia de exceções própria.

## Pendências e limites da entrega

- Nenhuma integração de Configuration ou mudança de domínios consumidores nesta tarefa; retomada depende de trabalho separado.
- CI remoto aguarda envio da branch pelo usuário; não afirmar execução de GitHub Actions sem push. O workflow de verificação está preparado e seus checks foram executados localmente. Nenhum artefato publicado, merge automático ou push realizado.
- Um arquivo não rastreado modules/platform-messaging/.flattened-pom 2.xml apareceu durante o trabalho e foi preservado como artefato alheio à feature; não faz parte do módulo nem da entrega proposta.
- Java 25/Docker são necessários para executar a suíte; arquivos TLS em src/test/resources são exclusivamente fixtures públicas de teste.

Consulte modules/platform-redis/README.md para configuração, assinaturas, exemplos, limites e operação.
