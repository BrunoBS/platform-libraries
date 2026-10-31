# platform-testing-core

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/modules/platform-testing-core.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../../README.md)

O módulo disponibiliza Datafaker como dependência transitiva para builders e factories
de dados de teste dos serviços. A versão é gerenciada pelo `platform-dependencies`.

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.
