# Responsabilidades dos pacotes e classes

Este documento descreve os tipos Java de produção em `src/main/java` do módulo `platform-testing`. Cada seção corresponde a um pacote. Classes de apoio com visibilidade de pacote estão identificadas como internas.

O módulo fornece infraestrutura e contratos reutilizáveis. Os serviços consumidores implementam builders, factories, cenários, clients e regras específicas do próprio domínio.

## Fluxo entre as principais peças

- Em massa de teste, o serviço implementa `TestDataBuilder`; pode agrupar variações semânticas em uma `TestDataFactory`; e usa `TestScenario` quando precisa preparar pré-condições.
- Em HTTP, `PlatformHttpTestConfiguration` fornece `PlatformRequestSpecificationFactory`; o client do serviço compõe essa factory ou estende opcionalmente `BaseClient`; a resposta do serviço pode estender `BaseResponse`.
- Para infraestrutura, uma anotação opt-in importa uma configuração ou registrar, que cria apenas os containers, conexões e clients associados aos recursos declarados.
- As anotações de ciclo de vida conectam as extensões JUnit ao contexto do Spring e ao isolamento entre testes.

## `br.com.portalmanager.platform.library.testing.architecture`

- **PlatformArchitectureExtension** — extensão JUnit que importa as classes dos pacotes selecionados, identifica implementações concretas que sobrescrevem comportamento de tipos da plataforma e exige cobertura por teste. Usa `PlatformArchitectureTest` para definir os pacotes observados, aceita cobertura explícita por `CoversClasses` e também reconhece a convenção de nomes de testes.

## `br.com.portalmanager.platform.library.testing.architecture.annotation`

- **PlatformArchitectureTest** — anotação opt-in que ativa `PlatformArchitectureExtension`. `basePackages` limita a análise ao serviço; `observedBasePackages` define de quais pacotes da plataforma vêm as implementações base observadas.
- **CoversClasses** — anotação de classe de teste para declarar tipos cobertos explicitamente, por exemplo quando um único teste cobre vários componentes. A extensão consulta esse valor além da convenção de nomes.

## `br.com.portalmanager.platform.library.testing.authorization`

- **AuthorizationMock** — fachada de teste sobre WireMock. Configura respostas permitidas, negadas, proibidas, expiradas ou customizadas; também cria respostas condicionadas aos headers de recurso e verifica chamadas recebidas.
- **AuthorizationMockExtension** — extensão JUnit ativada por `WithMockAuthorization`. Antes de cada teste limpa os stubs e configura o resultado padrão; para sessões permitidas, aplica os beans `AuthorizationSessionCustomizer` registrados na aplicação de teste.
- **AuthorizationMockResult** — enum com os resultados padrão `ALLOWED`, `DENIED`, `FORBIDDEN` e `INTERNAL_ERROR`, usados pela anotação e pela extensão.
- **AuthorizationMockTestConfiguration** — configuração Spring importada pela anotação. Cria o servidor WireMock em porta dinâmica, o bean `AuthorizationMock` e propriedades para apontar a autorização da aplicação para esse servidor.
- **AuthorizationResourceMatcher** — builder de headers para limitar um stub a workspace, aplicação, ambiente ou headers adicionais. É consumido por `AuthorizationMock.customResource`.
- **AuthorizationSessionBuilder** — cria `UserSession` com valores de teste padrão e métodos fluentes para identidade, expiração, grupos e grupos do autorizador. É usado por `AuthorizationMock` e pelos customizadores de sessão.
- **AuthorizationSessionCustomizer** — contrato funcional para ajustar um `AuthorizationSessionBuilder`. Serviços podem implementá-lo como bean para complementar todas as sessões permitidas.

## `br.com.portalmanager.platform.library.testing.authorization.annotation`

- **WithMockAuthorization** — anotação Spring/JUnit que importa `AuthorizationMockTestConfiguration` e registra `AuthorizationMockExtension`. O atributo `defaultResult` escolhe o comportamento padrão por classe; `AuthorizationMockResult` fornece as opções.

## `br.com.portalmanager.platform.library.testing.cloud.aws`

- **AwsLocalStackConnection** — extrai do container o endpoint, a região e o provedor de credenciais de teste. É a dependência comum usada pelos factory beans dos clients AWS.
- **AwsLocalStackContainer** — especialização do LocalStack que inicia os serviços solicitados e provisiona filas, políticas de DLQ e buckets. O registrar configura os serviços e os recursos a partir das anotações.
- **AwsLocalStackImportRegistrar** — lê `WithAwsLocalStack`, valida as anotações SQS/S3 e registra o container, a conexão e os clients apenas para os serviços selecionados.
- **AwsS3ClientFactoryBean** — cria o singleton AWS SDK `S3Client` apontando para o endpoint LocalStack, com região, credenciais locais e path-style habilitado; fecha o client no encerramento do contexto.
- **AwsS3TestSupport** — apoio interno do registrar: declara o factory bean do S3 no Spring quando a anotação S3 foi solicitada.
- **AwsService** — enum que traduz os nomes dos serviços para o formato reconhecido pelo LocalStack. O registrar atual habilita SQS e S3, embora o enum também nomeie Secrets Manager, SNS e EventBridge.
- **AwsSqsClientFactoryBean** — cria o singleton AWS SDK `SqsClient` usando `AwsLocalStackConnection` e encerra o client junto com o contexto.
- **AwsSqsTestSupport** — apoio interno que registra o factory bean SQS quando a anotação SQS foi solicitada.

## `br.com.portalmanager.platform.library.testing.cloud.aws.annotation`

- **WithAwsLocalStack** — anotação agregadora que importa `AwsLocalStackImportRegistrar`. Declara os recursos SQS e/ou S3 do teste.
- **AwsSqs** — configuração aninhada de filas SQS. Cada `Queue` informa nome e opções de DLQ e número máximo de recebimentos; o registrar transforma os dados em recursos provisionados pelo container.
- **AwsS3** — configuração aninhada de buckets. Os nomes são enviados ao registrar e provisionados pelo container.

## `br.com.portalmanager.platform.library.testing.cloud.azure`

- **AzureBlobServiceClientFactoryBean** — cria o singleton SDK `BlobServiceClient` usando a connection string do container Azurite.
- **AzureBlobStorageContainer** — especialização do Azurite que cria os containers Blob declarados depois da inicialização. Se o provisionamento falhar, interrompe o container.
- **AzureBlobStorageTestSupport** — apoio interno que registra o factory bean do client Blob solicitado.
- **AzureEmulatorImportRegistrar** — lê `WithAzureEmulator`, valida se Service Bus e/ou Blob Storage foram configurados, cria os containers correspondentes e delega o registro dos clients aos apoios do pacote.
- **AzureServiceBusClientBuilderFactoryBean** — fornece um singleton `ServiceBusClientBuilder` configurado com a connection string do emulador.
- **AzureServiceBusContainer** — coordena uma rede Testcontainers, o SQL Server exigido pelo emulador e o Service Bus Emulator; inicializa e encerra os recursos juntos.
- **AzureServiceBusTestSupport** — apoio interno que registra no Spring o factory bean do builder Service Bus quando esse serviço foi solicitado.
- **AzureServiceTestSupport** — contrato interno de estratégia para registrar suporte de um serviço Azure no contexto Spring; é implementado pelo suporte Service Bus.

## `br.com.portalmanager.platform.library.testing.cloud.azure.annotation`

- **WithAzureEmulator** — anotação agregadora que importa `AzureEmulatorImportRegistrar` e seleciona Service Bus e/ou Blob Storage.
- **AzureServiceBus** — configura filas do emulador Service Bus, incluindo `maxDeliveryCount` e sessões; o registrar passa esses dados ao container.
- **AzureBlobStorage** — fornece os nomes de containers Blob que serão criados por `AzureBlobStorageContainer`.

## `br.com.portalmanager.platform.library.testing.context`

- **TestContext** — mantém o correlation ID na thread atual. `PlatformRequestSpecificationFactory` consulta esse valor para cada request; as extensões de ciclo de vida limpam o estado entre testes.

## `br.com.portalmanager.platform.library.testing.database`

- **CleanupMode** — enum que seleciona limpeza do banco antes de cada teste, depois de cada teste ou nenhuma limpeza automática.
- **DatabaseCleaner** — identifica tabelas base no schema MySQL, mantém um cache de descoberta e trunca as tabelas não excluídas. É chamado por `MySqlTestExtension`.
- **DatabaseCleanupPhase** — define se scripts de cleanup executam depois de cada teste ou depois da classe.
- **DatabaseScriptExecutor** — resolve resources com `ResourceLoader`, valida existência/leitura e executa SQL via `ResourceDatabasePopulator`. É chamado por `DatabaseScriptExtension`.
- **DatabaseScriptExtension** — extensão JUnit que lê `WithDatabaseScripts` na classe e nos métodos e chama `DatabaseScriptExecutor` nas fases configuradas. Mantém ordem de setup e executa os cleanups na ordem inversa.
- **DatabaseSetupPhase** — define se scripts de setup executam uma vez antes da classe ou antes de cada teste.
- **MySqlTestConfiguration** — configuração de teste que declara um `MySQLContainer` como service connection do Spring Boot; é importada por `WithMySql`.
- **MySqlTestExtension** — extensão JUnit que aplica o `CleanupMode` de `WithMySql` e delega a limpeza a `DatabaseCleaner`.

## `br.com.portalmanager.platform.library.testing.database.annotation`

- **WithMySql** — importa a configuração do container MySQL e registra a extensão de limpeza; define modo de limpeza e tabelas excluídas, com `flyway_schema_history` preservada por padrão.
- **WithDatabaseScripts** — declaração repetível de scripts de setup e cleanup, fases de execução e opção para continuar diante de erro; registra `DatabaseScriptExtension`.
- **DatabaseScripts** — container de anotação gerado para permitir múltiplas declarações de `WithDatabaseScripts` na mesma classe ou método.

## `br.com.portalmanager.platform.library.testing.fixture`

- **TestClock** — cria um `Clock.fixed` a partir de instante textual ou `Instant`, com UTC como fuso padrão e suporte a fuso explícito.
- **TestIds** — cria UUID determinístico a partir de uma seed. Ajuda a manter IDs estáveis e reproduzíveis em fixtures.

## `br.com.portalmanager.platform.library.testing.fixture.builder`

- **TestDataBuilder** — contrato funcional genérico cujo `build()` produz o DTO ou objeto de teste.
- **AbstractTestDataBuilder** — classe-base genérica com retorno fluente tipado por `self()`. O builder concreto do serviço herda dela e implementa `build()` pelo contrato `TestDataBuilder`.

## `br.com.portalmanager.platform.library.testing.fixture.factory`

- **TestDataFactory** — contrato funcional para produzir a massa válida padrão por `valid()`.
- **AbstractTestDataFactory** — liga uma factory ao builder concreto. `valid()` constrói a instância padrão; `create(customization)` aplica variações ao builder antes de chamar `build()`. Assim a factory do serviço expressa cenários sem duplicar a construção do builder.

## `br.com.portalmanager.platform.library.testing.fixture.scenario`

- **TestScenario** — contrato funcional cujo `setup()` prepara dependências/pré-condições e devolve o resultado da preparação. A implementação pertence ao serviço consumidor e pode reutilizar builders/factories; não é específica do HTTP.

## `br.com.portalmanager.platform.library.testing.http`

- **AuthorizationRequestData** — record de token, account, ambiente e aplicação. O builder aninhado oferece os valores padrão e permite sobrescrever apenas os campos necessários para requests autorizados.
- **BaseClient** — classe-base opcional que encapsula chamadas à factory para requests comuns/autorizados, criação de response tipada e serialização com o `JsonMapper` recebido por injeção. O client concreto continua no serviço.
- **PlatformHttpTestConfiguration** — configuração importada por `PlatformIntegrationTest`; registra `PlatformRequestSpecificationFactory` com o ambiente Spring e os customizadores ordenados.
- **PlatformRequestSpecificationCustomizer** — contrato funcional para acrescentar headers ou outras opções a um `RequestSpecBuilder`; os beans são aplicados pela factory em cada request.
- **PlatformRequestSpecificationFactory** — cria uma nova especificação RestAssured por request com porta local, JSON, correlation ID e customizadores; suas variantes adicionam os headers de autorização usando `AuthorizationRequestData`.

## `br.com.portalmanager.platform.library.testing.http.response`

- **BaseResponse** — classe-base fluente para responses específicas de cada serviço. Encapsula `ValidatableResponse`, oferece expectativas de status e de campos JSON e permite extrair o corpo tipado; o client do serviço cria a response concreta.

## `br.com.portalmanager.platform.library.testing.kafka`

- **KafkaTestConfiguration** — configuração de teste que declara o container Kafka como service connection. É importada apenas quando `WithKafka` é usado.

## `br.com.portalmanager.platform.library.testing.kafka.annotation`

- **WithKafka** — anotação opt-in que importa `KafkaTestConfiguration`; habilita o broker para o contexto Spring do teste.

## `br.com.portalmanager.platform.library.testing.lifecycle`

- **PlatformIntegrationExtension** — extensão JUnit que limpa o `TestContext` antes e depois de cada teste de integração.
- **PlatformUnitTestExtension** — extensão JUnit que limpa o `TestContext` e o MDC antes e depois de cada teste unitário.
- **TestPerformanceExtension** — mede duração de métodos e classes, registra testes que ultrapassam o limite configurado e resume tempos ao final da classe. É ativada por `PlatformIntegrationTest`.

## `br.com.portalmanager.platform.library.testing.lifecycle.annotation`

- **PlatformIntegrationTest** — anotação composta para integração: ativa `SpringBootTest` com servidor aleatório e profile `test`, importa a configuração HTTP e registra as extensões de contexto/performance.
- **PlatformUnitTest** — anotação composta para teste unitário: registra Mockito e `PlatformUnitTestExtension`, sem iniciar o contexto Spring da aplicação.

## Atualização deste inventário

Ao criar, remover ou mover uma classe de produção, atualize a seção do pacote correspondente e confira também os relacionamentos descritos aqui.
