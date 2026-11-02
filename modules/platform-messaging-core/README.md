# platform-messaging-core

Módulo de contratos e suporte compartilhado de mensagens, erros e validação usado internamente pelas bibliotecas da plataforma.

O consumidor normalmente declara a biblioteca pública que utiliza; não precisa declarar nem configurar `platform-messaging-core` diretamente. O módulo `platform-messaging` depende deste core e contém a integração web/JDBC/cache de mensagens.
