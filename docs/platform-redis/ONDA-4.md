# Onda 4 — concluída

Streams: XADD com retenção exata limitada, XREAD, grupos 0/$ com MKSTREAM e criação idempotente, XREADGROUP, XACK e recuperação XPENDING IDLE + XCLAIM em batches. RedisStreams/RedisStreamRecord não expõem drivers. Listeners anotados usam workers limitados, backoff/jitter e ACK somente após sucesso; registros falhos continuam pending. Decodificação/handler por registro impede que um JSON inválido impeça processar todo o lote. Retenção do listener é aplicada nas recuperações periódicas.

Keyspace Notifications: anotação/interface próprias com canal/body nativo; off por default. Apenas assina padrões de keyspace/keyevent. Não executa CONFIG SET em produção nem confunde notificações nativas com eventos JSON. O teste configura um Redis Testcontainers dedicado e restaura a opção ao terminar.

BUILD SUCCESS em 15/10/2026 00:01:05, 39.437s; /private/tmp/platform-redis-wave4.log. Redis: 2 unitários + 21 integrações, zero falhas/erros/skips. Cobertos grupos, latest/beginning, ACK/pending/claim, reprocessamento depois de falha, retenção e limites; bootstrap sem workers; notificações opt-in e expiração real. Sem divergências pendentes.
