# platform-parameter-client

Dois contratos públicos independentes e somente leitura:

- `ConfigurationProvider.get`: leitura tipada de envelopes CONFIGURATION. Três overloads (default inferível, Class e TypeReference Jackson 3). Default por identidade somente na ausência; erros de JSON/conversão não retornam default. EXPRESSION é incompatível.
- `ExpressionProvider.matches`: avaliação booleana dos oito operadores EXPRESSION V1. Overload estrito propaga falhas controladas; default explícito registra somente chave técnica do erro e retorna o booleano informado. CONFIGURATION é incompatível. Precisão preservada com BigInteger/BigDecimal, sem coerção de texto ou passagem por double.

Injete somente o contrato necessário ao componente:

```java
@Service
public class OrderConfiguration {
    private final ConfigurationProvider configurations;

    public OrderConfiguration(ConfigurationProvider configurations) {
        this.configurations = configurations;
    }

    public int quarantineDays() {
        return configurations.get("key-service.quarantine-days", 30);
    }
}

@Service
public class AgePolicy {
    private final ExpressionProvider expressions;

    public AgePolicy(ExpressionProvider expressions) {
        this.expressions = expressions;
    }

    public boolean accepts(int age) {
        return expressions.matches("rules.minimum-age", age, false);
    }
}
```

As classes demonstrativas ficam em arquivos separados; importe `org.springframework.stereotype.Service` e o provider do pacote `br.com.portalmanager.platform.library.parameterclient` correspondente.

Ambos compartilham o leitor interno RedisParameterReader e o RedisStore resolvido por `@RedisNamespace(value="dynamic-parameter", mode=SHARED)`. A chave física continua `dynamic-parameter:<chave-logica>`, envelope `{type,value}`; não há prefixação manual nem outro pool, conexão ou codec. O RedisStore padrão pode usar outro namespace global, inclusive ISOLATED. Não é necessário configurar `platform.redis.namespace=dynamic-parameter` nem `namespace-mode=SHARED` globalmente.

A chave recebida é literal, sem regex, trim ou inferência de feature: o cliente rejeita somente null/em branco; RedisStore valida controles e limites de bytes. Validação de cadastro pertence ao produtor. CONFIGURATION mantém o mapper/conversão estrita existentes; EXPRESSION usa desserialização numérica exata. Cada bean pode ser substituído independentemente. `platform.parameter-client.enabled=false` desativa ambos; sem infraestrutura Redis não são criados.

Contrato, erros, matriz de compatibilidade e exemplos no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-parameter-client.md).

- [POM](pom.xml)
- [Build do reactor](../../README.md)

Na raiz das bibliotecas: `mvn --settings .github/maven-settings.xml -pl modules/platform-parameter-client -am clean verify`. Integrações usam Redis real via Testcontainers e exigem Docker. Não publica pacotes. Cobertura inclui interfaces independentes/ausência da API removida, overrides separados, os oito operadores/precisão/fallback seguro, defaults, RedisStore padrão ISOLATED, outro store anotado, envelopes trocados, fixture do produtor, prefixo único, lotes, sobrescrita, delete, TTL e falhas reais de conexão/timeout.
