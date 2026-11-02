# platform-messaging

Módulo de integração de mensagens da Platform Libraries. Os contratos compartilhados de erros, mensagens e validação ficam em `platform-messaging-core`; este módulo fornece resolução de mensagens, integração web/JDBC e cache Redis via `platform-redis`.

O consumidor normalmente declara `platform-messaging` e configura apenas os recursos que habilitar. Não precisa declarar `platform-messaging-core` nem o starter Spring Redis separadamente para usar o cache Redis fornecido pela plataforma.

Contratos, configuração detalhada e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/modules/platform-messaging.md).
