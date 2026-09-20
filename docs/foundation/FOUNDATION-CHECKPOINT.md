# Foundation Checkpoint — FOUNDATION-GOLDEN-V1

## Estado

A execução técnica das fases F0–F5 está concluída. A F6 possui todas as validações locais ao repositório concluídas e uma pendência de integração remota antes da declaração final do checkpoint.

## Fases concluídas

- F0 — inventário e baseline.
- F1 — estabilização do platform-build.
- F2 — starter mínimo.
- F3 — desacoplamento do platform-catalog.
- F4 — remoção física do platform-crud.
- F5 — revisão das libraries sobreviventes.

## Evidência platform-build

Head estabilizado: `6b667463f0373cac0d2bd901743c17d64b9c71f7`.

GitHub Actions run `35525112512`:

- Java 25;
- `mvn -B clean verify`;
- Maven Enforcer verde;
- dependency convergence verde;
- `BUILD SUCCESS`.

O parent não gerencia mais `platform-crud`.

## Evidência platform-libraries

Head documental da F5: `dc9209706c97737d08b0e994c7e5e633fe8a0d24`.

GitHub Actions Verify #42, run `35530259305`:

`mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify`

Resultado:

- RequireJavaVersion: sucesso;
- RequireMavenVersion: sucesso;
- DependencyConvergence: sucesso em todos os módulos;
- messaging: 49 testes verdes;
- authorization: 42 testes verdes;
- audit: 17 testes verdes;
- logging: 14 testes verdes;
- starter minimal consumer: 1 teste verde;
- test-support: 34 testes verdes;
- catalog: 17 testes verdes;
- tagging: 12 testes verdes;
- reactor completo: `BUILD SUCCESS`.

O reactor não contém `platform-crud`.

## Estado arquitetural

O starter obrigatório contém somente logging, messaging e authorization.

Audit, catalog e tagging são capabilities explícitas.

Catalog é independente de CRUD genérico.

Messaging pode iniciar sem JDBC/DataSource; JDBC e Redis são integrações opcionais.

Authorization preserva o comportamento aprovado pela Foundation.

## F6 — integração remota oficial concluída

A arquitetura de distribuição aprovada foi validada no fluxo oficial:

```text
platform-build
  -> publish
GitHub Packages
  -> resolve
platform-libraries
  -> mvn clean verify
```

As Libraries passaram a consumir `br.com.portalmanager.core:platform-parent:1.0.1` no commit `4b0a17bbc35b0d4cd6c52b9d4413b565e8d1842e`.

GitHub Actions Verify #45, run `35531158159`:

- branch `refactor/golden-foundation`;
- commit `4b0a17bbc35b0d4cd6c52b9d4413b565e8d1842e`;
- comando `mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify`;
- RequireJavaVersion: sucesso;
- RequireMavenVersion: sucesso;
- DependencyConvergence: sucesso em todos os módulos;
- reactor completo: `BUILD SUCCESS`.

Como o POM usa `<relativePath/>`, o parent 1.0.1 não é resolvido do workspace local. A execução do CI com o settings oficial e build verde constitui a evidência da resolução remota prevista para a F6.

## Checkpoint

Com F0–F6 concluídas e a integração remota oficial validada, o checkpoint `FOUNDATION-GOLDEN-V1` está tecnicamente concluído.

A próxima fase arquitetural permitida pelo roadmap é a nova Golden Reference. Nenhum trabalho dessa fase é iniciado por este documento.


## Correção pós-checkpoint — distribuição das Libraries para consumidores Golden

A G1 da Golden Reference revelou um gap de distribuição que não havia sido exercitado pela F6 original.

A F6 provou corretamente:

```text
platform-build
  -> publish
GitHub Packages / platform-build
  -> resolve
platform-libraries
  -> mvn clean verify
```

Porém o primeiro consumidor downstream da Foundation também exige:

```text
platform-libraries
  -> publish
GitHub Packages / platform-libraries
  -> resolve
Golden consumer
```

### Evidência do gap

No `BrunoBS/account-service`, após autenticação válida e configuração dos dois registries, o GitHub Actions Verify #6, run `35534805355`, attempt 1, resolveu `platform-parent:1.0.1` remotamente, mas confirmou ausência de:

- `br.com.portalmanager.core:platform-starter:1.0.0`;
- `br.com.portalmanager.core:platform-test-support:1.0.0`;

tanto no registry de `platform-build` quanto no registry correto de `platform-libraries`.

A inspeção do reactor confirmou que todas as libraries permaneciam em `1.0.0` e que não existia workflow de publicação das Libraries. O POM raiz também não possuía `distributionManagement` próprio, portanto herdava o destino do `platform-parent`, pertencente ao `platform-build`.

Como o probe autenticado confirmou que os artefatos `1.0.0` não existiam no registry correto, a versão não foi alterada.

### Correção

No commit `9bcf1e0bf6efa58dd1141e75e8e8a9f8ffd2a44c`:

- `platform-libraries` passou a declarar `distributionManagement` para `https://maven.pkg.github.com/brunobs/platform-libraries`;
- foi criado settings de publicação separado;
- leitura do parent continua autenticada por `PLATFORM_PACKAGES_TOKEN`;
- publicação no package do próprio repositório usa o `GITHUB_TOKEN` efêmero com `packages: write`, seguindo o padrão de publicação do `platform-build`;
- o workflow de publicação executa `mvn clean deploy` e não utiliza instalação local como evidência.

### Evidência da Foundation após a correção

GitHub Actions Verify #48, run `35534990434`:

- branch `refactor/golden-foundation`;
- commit `9bcf1e0bf6efa58dd1141e75e8e8a9f8ffd2a44c`;
- `mvn clean verify`;
- resultado: sucesso.

GitHub Actions Publish Maven packages #2, run `35534990418`:

- branch `refactor/golden-foundation`;
- commit `9bcf1e0bf6efa58dd1141e75e8e8a9f8ffd2a44c`;
- `mvn --settings .github/maven-publish-settings.xml --batch-mode --no-transfer-progress clean deploy`;
- reactor completo `1.0.0` publicado com sucesso;
- `platform-starter`: sucesso;
- `platform-test-support`: sucesso;
- `platform-messaging`: sucesso;
- `platform-authorization`: sucesso;
- `platform-audit`: sucesso;
- `platform-logging`: sucesso;
- `platform-catalog`: sucesso;
- `platform-tagging`: sucesso;
- resultado final: `BUILD SUCCESS`.

### Evidência do consumidor downstream

GitHub Actions Verify #7 do `BrunoBS/account-service`, run `35535402633`:

- resolve a Foundation pelos registries remotos de `platform-build` e `platform-libraries`;
- Maven Enforcer: RequireJavaVersion, RequireMavenVersion e DependencyConvergence verdes;
- `AccountServiceApplicationIT`: 1 teste, 0 falhas, 0 erros;
- contexto Spring Boot iniciado com Java 25;
- `mvn clean verify`: `BUILD SUCCESS`.

Não foi utilizado `mvn install` local entre os repositórios.

### Estado

O gap de distribuição downstream da Foundation está corrigido. A topologia remota efetivamente validada passa a ser:

```text
platform-build
  -> GitHub Packages / platform-build
  -> platform-libraries
  -> GitHub Packages / platform-libraries
  -> Golden consumer
```


## Correção pós-checkpoint — infraestrutura de teste opt-in

A G1 da Golden Reference revelou um segundo gap da Foundation: `platform-test-support:1.0.0` exportava transitivamente infraestrutura JDBC/MySQL/Kafka/Testcontainers mesmo para consumidores que não usavam essas capacidades.

No `BrunoBS/account-service`, um simples `@PlatformIntegrationTest` sem persistência passou a conter JDBC no classpath e o Spring Boot tentou ativar `DataSourceAutoConfiguration`.

O workaround temporário de excluir `DataSourceAutoConfiguration` no consumidor foi rejeitado como padrão.

### Contrato corrigido

A release `platform-test-support:1.0.1` torna opcionais:

- `spring-boot-starter-jdbc`;
- `mysql-connector-j`;
- `spring-boot-testcontainers`;
- `spring-boot-starter-kafka`;
- Testcontainers MySQL;
- Testcontainers Kafka;
- Testcontainers JUnit Jupiter.

O contrato passa a ser:

```text
infraestrutura não declarada
→ não entra transitivamente
→ não ativa auto-configuração correspondente
```

As capacidades `@WithMySql` e `@WithKafka` permanecem disponíveis e explícitas. Serviços que as utilizam devem declarar as dependências de infraestrutura correspondentes no escopo de teste.

Foi adicionado `InfrastructureDependencyOptionalityTest` para impedir regressão do contrato Maven.

### Versionamento

Como as releases anteriores já estavam publicadas, nenhuma versão foi sobrescrita:

```text
platform-parent/platform-dependencies: 1.0.2
platform-libraries release train:       1.0.1
```

Todos os módulos sobreviventes de `platform-libraries` foram publicados na release train `1.0.1`.

### Evidência

Commit funcional das Libraries:

`729f6acee1443b660680c5670fb5b995f5f1a854`

GitHub Actions Verify #50, run `35536158485`:

- `InfrastructureDependencyOptionalityTest`: sucesso;
- `platform-test-support`: 35 testes, 0 falhas, 0 erros;
- reactor `platform-libraries 1.0.1`: `BUILD SUCCESS`.

GitHub Actions Publish Maven packages #3, run `35536158484`:

- reactor completo `1.0.1`;
- `BUILD SUCCESS`;
- publicação remota no GitHub Packages.

O workflow de publicação foi restaurado para `workflow_dispatch` após o release.

### Evidência downstream

No `BrunoBS/account-service`, commit `ba2b7fcdd90fa9a35b5e0ff01a14a0b38423fbf8`:

- parent `1.0.2`;
- starter/test-support `1.0.1` via dependency management;
- nenhum JDBC/MySQL adicionado;
- nenhum exclude de `DataSourceAutoConfiguration`.

GitHub Actions Verify #11, run `35536464540`:

- `AccountServiceApplicationIT`: 1 teste, 0 falhas, 0 erros;
- Maven Enforcer e dependency convergence: sucesso;
- `mvn clean verify`: `BUILD SUCCESS`.

### Estado

O gap está resolvido. A Foundation não exige configuração negativa para impedir infraestrutura ausente; capacidades de teste pesadas são opt-in.


## Refatoração controlada pós-checkpoint — consolidação build + libraries

### Estado

A consolidação foi implementada na branch `refactor/consolidate-foundation-repositories`, mas o novo checkpoint **não está fechado**.

### Estrutura implementada

```text
platform-libraries
├── platform-dependencies
├── platform-parent
├── platform-libraries-bom
└── capabilities
```

O parent não gerencia mais versões das capabilities.

### Evidência de build

GitHub Actions Verify #54, run `35537087703`:

- Java 25;
- Maven Enforcer verde;
- dependency convergence verde;
- platform-dependencies e platform-parent resolvidos no mesmo reactor;
- platform-libraries-bom incluído;
- suites unitárias e de integração verdes;
- `BUILD SUCCESS`.

### Evidência de publicação — bloqueio encontrado

GitHub Actions Publish Maven packages #4, run `35537303423`:

- tentativa de deploy para o registry de `platform-libraries`;
- primeiro artifact: `platform-dependencies:1.0.3`;
- resposta do registry: HTTP 422;
- `BUILD FAILURE`.

A última publicação no antigo repository contém `platform-dependencies:1.0.2` e `platform-parent:1.0.2`; a versão `1.0.3` não havia sido publicada por aquele fluxo.

Nenhum package foi apagado ou alterado destrutivamente.

### Pendência de checkpoint

Antes de declarar a consolidação concluída é necessário resolver a migração dos packages Maven `platform-dependencies` e `platform-parent` para o contexto de `platform-libraries`, publicar o reactor completo e validar o `account-service` consumindo exclusivamente o novo registry.

Até isso ocorrer:

- `account-service` não deve avançar para G2;
- o antigo registry continua necessário para consumidores publicados;
- `platform-build` não deve ser arquivado.


## Atualização da consolidação — gate de publicação

A estrutura consolidada passou no GitHub Actions Verify #60, run `35537837948`, com `BUILD SUCCESS`.

A release train das capabilities `1.1.0` foi publicada com sucesso no run `35538290455`.

Entretanto o probe remoto isolado run `35538856486` confirmou que `platform-dependencies:1.0.3` não é resolvido no registry de `platform-libraries`. A tentativa de deploy desse artifact no novo repository retorna HTTP 422.

No consumidor `BrunoBS/account-service`, o Verify #13, run `35538639721`, e o probe sem cache, run `35538758316`, falham ao resolver `platform-parent:1.1.0` usando somente o registry consolidado.

### Estado do checkpoint

A refatoração estrutural está implementada e verde, mas o **novo checkpoint de consolidação permanece aberto**.

Não declarar o `platform-build` obsoleto operacionalmente e não arquivá-lo até resolver a migração dos packages Maven estruturais e obter `mvn clean verify` verde no `account-service` usando exclusivamente `platform-libraries`.

A G2 do `account-service` permanece bloqueada.
