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

## Pendência F6 — integração remota oficial

A arquitetura de distribuição aprovada exige:

```text
platform-build
  -> publish
GitHub Packages
  -> resolve
platform-libraries
  -> mvn clean verify
```

As alterações estabilizadas do `platform-build` estão na branch `refactor/golden-foundation` / PR #6 e o workflow atual dessa branch executa verify, mas não publica os artefatos estabilizados no GitHub Packages.

Por isso, o sucesso atual de `platform-libraries` prova o reactor e a resolução remota configurada, mas não prova ainda que a versão publicada de `platform-parent` / `platform-dependencies` contém exatamente as mudanças da F1.

Não usar `mvn install` local como substituto desta evidência oficial.

## Condição para declarar FOUNDATION-GOLDEN-V1

Antes do fechamento final:

1. disponibilizar no GitHub Packages a versão do `platform-parent` e `platform-dependencies` correspondente ao build estabilizado;
2. executar `platform-libraries` contra esses artefatos remotos;
3. obter `mvn clean verify` verde nesse fluxo;
4. registrar a evidência final neste documento.

Até isso ocorrer, o checkpoint permanece tecnicamente preparado, porém não declarado como concluído.
