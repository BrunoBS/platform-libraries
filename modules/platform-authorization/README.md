# platform-authorization

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-authorization.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.

## Eixos de autorização

`AuthorizationExecutionMode { REAL, MOCK }` substitui o nome Java `AuthorizationMode`. A propriedade `platform.authorization.mode`, seu default REAL e a restrição de MOCK aos perfis local/test/dev permanecem. A renomeação exige recompilação de consumidores que importam o tipo antigo.

`AuthorizationMechanism { TOKEN, POLICY }` é independente do modo de execução. `@AuthorizationRequired.mechanism` tem default TOKEN; nível e ação continuam obrigatórios. `AuthorizationPolicy` existente continua representando nível/source/path variables, sem relação com a nova allowlist de entrada.

### TOKEN

HTTP/MCP e identidades humanas/técnicas usam o mesmo AuthorizationClient. Identidade e permissão são determinadas pelo token e pelo Authorization Service. Token ausente/inválido/negado nunca seleciona POLICY. MOCK mantém a sessão de desenvolvimento e as proteções de perfil anteriores.

### POLICY

Use somente em métodos internos explicitamente anotados com mechanism=POLICY e policy=<id>. O método recebe `PolicyAuthorizationContext` com correlação e escopo solicitado; esse valor não prova origem nem concede permissão por si só.

`EntryPolicyAuthorizer` verifica, nos dois modos REAL/MOCK:

- Uma única origem de infraestrutura confiável ativa, com mensagem/timestamp/correlação presentes.
- Principal técnico cadastrado, ação/nível, Facade e listener/destino físico exatos.
- Uma tupla de escopo explicitamente permitida (conta/aplicação/ambiente), sem wildcard. null em aplicação/ambiente permite somente null, não qualquer valor.
- Ausência de requisição HTTP vinculada à thread. Uma chamada HTTP/MCP comum também não dispõe da prova privada do consumidor.

Propriedades novas: `platform.authorization.technical-principals` e `platform.authorization.entry-policies`. Cada entrada contém entry-point (`classe#metodo`), facade (`classe#metodo`), destination (alias oficial), queue-url (URL exata configurada), principal, action, level e scopes (tuplas account-identifier/application-identifier/environment-identifier). Políticas vazias são o default; método POLICY sem configuração interrompe o startup. Métodos/destinos ambíguos, principal desconhecido e configurações incoerentes também interrompem a inicialização.

A integração opcional SQS usa exclusivamente `SqsMessageQueueConsumer.currentDelivery()`. A biblioteca de fila abre o contexto privado após desserializar e o fecha antes do ACK, inclusive em erro. Não existe setter público nem abertura via payload/header. O snapshot público Delivery não abre um contexto. Prova de origem não é propagada para tarefas assíncronas; chamadas POLICY nessas tarefas são negadas. UserContext/MDC do escopo autorizado são restaurados em finally.

A aplicação POLICY precisa declarar platform-message-queue explicitamente (dependência opcional da autorização). Consumidores exclusivamente TOKEN continuam iniciando sem essa capability. A implementação usa os pacotes existentes model, config e aop.

`AuthorizationEntryOriginResolver` é um SPI de infraestrutura registrado por configuração Spring confiável. Não deve ser exposto como endpoint, escolhido por payload ou implementado com leitura de headers; um resolver incorreto é uma violação da fronteira de confiança da aplicação. O bridge oficial não aceita setters públicos de contexto.

IAM e a política da fila devem restringir produtores/origens. A evidência comprova a execução no consumidor configurado; não autentica individualmente um emissor S3 descrito no body. A autorização de domínio ainda verifica a allowlist de ações/escopos. Nenhuma dessas verificações define metadados ou um novo envelope Audit.

As sessões técnicas POLICY não recebem token fictício, grupos ou OWNER. Visibilidade de entidades permanece um controle separado. REAL continua exigindo service-url, preservando o contrato anterior para aplicações mistas.

### Limites

O bridge cobre o consumidor SQS regular; não habilita POLICY por header, Azure, dead-letter listener ou tarefa assíncrona. Notificações S3 nativas não são MessageQueueMessage<T>; não são transformadas implicitamente em eventos Audit. Este incremento não publica artefatos.
