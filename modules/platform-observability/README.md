# platform-observability

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/modules/platform-observability.md).

O formato de saída padrão é preservado. Para substituir os appenders de console pelo formato JSON da plataforma, configure:

```yaml
platform:
  observability:
    logging:
      format: JSON
```

Os valores aceitos são `DEFAULT` (padrão) e `JSON`.

## Mascaramento explícito de valores

Valores sensíveis devem ser envolvidos em `SensitiveValue.of(...)` antes de serem passados ao logger. A mesma máscara parcial é aplicada no formato `DEFAULT` e no formato `JSON`:

```java
import br.com.portalmanager.platform.library.observability.logging.SensitiveValue;

logger.info("Credencial usada: {}", SensitiveValue.of(credential));
```

O mascaramento fica habilitado por padrão. Para desativá-lo explicitamente:

```yaml
platform:
  observability:
    logging:
      masking:
        enabled: false
```

Com a máscara habilitada, valores com até dois caracteres são totalmente ocultados; de três a seis, apenas o primeiro e o último caracteres ficam visíveis; a partir de sete, ficam visíveis os três primeiros e os três últimos. Sem `SensitiveValue.of(...)`, a lib não tenta identificar ou mascarar valores automaticamente.

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.
