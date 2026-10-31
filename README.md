# Platform Libraries

Capabilities técnicas reutilizáveis da Golden Platform, com builds e releases independentes.

A documentação humana vigente está na [Golden Platform](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/README.md).

## Desenvolvimento local

Java 25, Maven 3.9.9–3.x e Docker para testes de integração. Configure as credenciais de leitura do GitHub Packages no ambiente usado por `.github/maven-settings.xml` (`GITHUB_PACKAGES_USERNAME` e `GITHUB_PACKAGES_TOKEN`).

```sh
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

### Cobertura e análise Sonar

O `platform-parent` configura o JaCoCo para instrumentar testes unitários e de integração. A fase `verify` gera os relatórios HTML e XML em `target/site/jacoco/`; o XML usado pelo Sonar é `target/site/jacoco/jacoco.xml`. Serviços que herdam o parent recebem esse caminho automaticamente e não precisam declarar o plugin JaCoCo no próprio POM.

A análise Sonar deve ser executada pelo workflow do serviço depois de `verify`, para que o XML já exista. Configure `SONAR_HOST_URL` e `SONAR_TOKEN` no ambiente/Secrets da CI e use a chave de projeto registrada no Sonar. O token não deve ser gravado no POM nem versionado. O parent define o caminho de cobertura; o serviço continua responsável por conectar sua análise ao projeto e servidor corretos.
