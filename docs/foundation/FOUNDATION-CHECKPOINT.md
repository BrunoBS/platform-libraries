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

As Libraries passaram a consumir `com.empresa.platform:platform-parent:1.0.1` no commit `4b0a17bbc35b0d4cd6c52b9d4413b565e8d1842e`.

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
