# Onda 1 — concluída

Store, AsyncStore, codec seguro Jackson 3, namespace explícito, TTL/NX e builder single use; integração no reactor/BOM sem inclusão no starter. APIs passivas autoconfiguradas, reaproveitando conexão Spring Boot. Async usa ReactiveStringRedisTemplate/toFuture, sem executor bloqueante.

BUILD SUCCESS em 14/10/2026 23:53:49, 37.585s: mvn --offline --batch-mode --no-transfer-progress -pl modules/platform-redis -am verify. Módulo Redis: 2 unitários + 7 integrações Redis real, zero falhas/erros/skips. Reactor incluiu testes de messaging e testing-core. Log /private/tmp/platform-redis-wave1.log.

Problemas corrigidos: starter reativo ausente no cache foi baixado da Central; literal JSON do teste corrigido; coerção de número para String detectada e desabilitada explicitamente no codec. Sem divergência contratual pendente.
