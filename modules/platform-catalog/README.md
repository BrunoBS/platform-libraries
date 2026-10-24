# platform-catalog

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-catalog.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

Catálogos configurados com validação de schema validam `settings` contra o contrato publicado quando houver um schema específico ou fallback. Se nenhum schema existir, a validação dinâmica é ignorada; validações fixas do catálogo continuam ativas. Erros de payload contra um schema existente e falhas técnicas da fonte de schemas continuam sendo reportados.

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.
