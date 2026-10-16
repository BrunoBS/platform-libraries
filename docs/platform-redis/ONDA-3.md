# Onda 3 — concluída

RedisPublisher separado de Store. Descoberta de N listeners via RedisSubscribe, canais e padrões PSUBSCRIBE, resolução genérica incluindo proxies; container somente com inscrições. Handlers fora do I/O com pool/fila limitados. Saturação descarta tarefa e incrementa counters/reporta redis.saturation; sem retry garantido. ErrorHandler customizável recebe somente causas sanitizadas. Reconexão usa backoff exponencial com jitter; stop/destroy e prazo de encerramento dos executores.

BUILD SUCCESS em 14/10/2026 23:58:29, 37.968s; /private/tmp/platform-redis-wave3.log. Redis: 2 unitários + 16 integrações, zero falhas/erros/skips. Inclui N canais/listeners/instâncias, padrões, separação Store/Publisher, desconexão/restart sem replay, handler falho, saturação observável e bootstrap passivo com servidor indisponível. Teste adicional de reconexão de transporte previsto na revisão de segurança final.
