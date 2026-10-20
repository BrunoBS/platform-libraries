# Platform Libraries

Capabilities técnicas reutilizáveis da Golden Platform, com builds e releases independentes.

A documentação humana vigente está na [Golden Platform](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/README.md).

## Desenvolvimento local

Java 25, Maven 3.9.9–3.x e Docker para testes de integração. Configure as credenciais de leitura do GitHub Packages no ambiente usado por `.github/maven-settings.xml` (`GITHUB_PACKAGES_USERNAME` e `GITHUB_PACKAGES_TOKEN`).

```sh
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```
