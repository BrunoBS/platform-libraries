# platform-message-queue

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-message-queue.md).

A API de consumo fica em `br.com.portalmanager.platform.library.messagequeue.api`. Implementações de transporte, resolução e lifecycle ficam em pacotes internos e de provider; eles não são pontos de extensão para serviços consumidores. Os pacotes anteriores não são mantidos como aliases. As falhas usam `ApiException` e as classes padronizadas de `platform-messaging-core`; o módulo não define uma hierarquia própria de exceções.

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.
