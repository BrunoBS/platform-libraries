# platform-parameter-client

Leitura tipada de CONFIGURATION com get e avaliação booleana de EXPRESSION V1 com matches, reutilizando RedisStore. Contrato, configuração e exemplos no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-parameter-client.md).

- [POM](pom.xml)
- [Build do reactor](../../README.md)

Na raiz das bibliotecas: `mvn --settings .github/maven-settings.xml -pl modules/platform-parameter-client -am verify`. Integrações usam Redis real via Testcontainers e exigem Docker. Não publica pacotes. Configure platform.redis com namespace-mode SHARED e namespace dynamic-parameter; o cliente não aplica outro prefixo.
