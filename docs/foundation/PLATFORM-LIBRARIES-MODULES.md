# Platform Libraries — catálogo vigente de módulos

> Inventário baseado no reactor `pom.xml` e nos POMs dos módulos da branch `main`, revisado em 2026-10-20.

## Módulos estruturais

- `platform-parent`: parent Maven, Java 25, Maven >= 3.9.9, plugins, Enforcer, testes e qualidade.
- `platform-dependencies`: BOM tecnológico externo.
- `platform-libraries-bom`: BOM de versões das capabilities.
- `platform-libraries` (POM raiz): reactor/aggregator; não é starter de runtime.

Coordenadas Maven da Foundation: `br.com.portalmanager.platform.library`. Versão de release train configurada em `.mvn/maven.config` via `revision`, atualmente `1.0.0`.

## Capabilities de runtime

| Módulo | Responsabilidade | Incluído pelo starter? |
| --- | --- | --- |
| `platform-starter` | Agregação do baseline transversal | — |
| `platform-observability` | Logging estruturado | Sim |
| `platform-messaging` | Mensagens, i18n e exceptions | Sim |
| `platform-authorization` | Autorização, contexto e filtros de visibilidade | Sim |
| `platform-audit` | Auditoria | Não |
| `platform-catalog` | Infraestrutura de catálogos | Não |
| `platform-tagging` | Tags e reconciliação | Não |
| `platform-schema-validation` | Validação de payloads por schema | Não |
| `platform-redis` | Operações de Redis | Não |
| `platform-message-queue` | Abstrações de mensageria/filas | Não |

`platform-starter` agrega **somente** observability, messaging e authorization; outras capabilities devem ser declaradas conforme necessidade. Consulte os READMEs de cada módulo para APIs e configuração.

## Testes — agregador `modules/platform-testing`

O módulo `platform-testing` é um agregador Maven, com submódulos:

- `platform-testing-core`
- `platform-testing-http`
- `platform-testing-authorization`
- `platform-testing-database`
- `platform-testing-kafka`
- `platform-testing-redis`
- `platform-testing-cloud-aws`
- `platform-testing-cloud-azure`

Dependências de teste são opt-in; declarar apenas os módulos utilizados pelo consumidor. Não tratar todo o conteúdo como parte de `platform-testing-core`.

## Remoções e renomes

- `platform-crud`: removido, sem substituição por CRUD genérico.
- `platform-logging`: legado; usar `platform-observability`.
- `platform-test-support`: legado; usar os submódulos `platform-testing-*` apropriados.
- `platform-build` (repositório separado): linha histórica, não fonte do parent atual.

## Validação

Comparar este inventário ao reactor `pom.xml` e aos POMs de cada módulo em toda alteração estrutural. Publicação no GitHub Packages e `mvn clean verify` do consumidor são verificações separadas da leitura do código.
