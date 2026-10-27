# platform-observability

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-observability.md).

O formato de saída padrão é preservado. Para substituir os appenders de console pelo formato JSON da plataforma, configure:

```yaml
platform:
  observability:
    logging:
      format: JSON
```

Os valores aceitos são `DEFAULT` (padrão) e `JSON`.

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.
