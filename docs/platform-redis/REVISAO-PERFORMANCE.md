# Revisão de performance — antes de aplicar o ajuste

Em 15/10/2026 a revisão final identificou que XPENDING com IDLE tem custo O(M) sobre entradas examinadas, mesmo com COUNT pequeno. Fonte primária: https://redis.io/docs/latest/commands/xpending/. Usar apenas COUNT no filtro IDLE não satisfaz o objetivo de trabalho limitado por rodada do V3.

Decisão: XPENDING sem IDLE retorna no máximo count entradas numa janela por cursor. Filtrar idle localmente e executar XCLAIM (que verifica minIdle atomicamente). O listener mantém cursor efêmero por worker, sem mapa ilimitado/cache/outbox. Adicionar RedisStreamRecovery<T>(nextId, records) e overload recover(..., afterId, ...) ao contrato antes de implementar. recover original permanece como conveniência de primeira janela. nextId=0-0 sinaliza volta ao início; batch vazio pode ter cursor de continuação válido. Isso permite avançar por entradas antigas sem bloquear as mais novas. Não altera RedisStore nem serviços consumidores.

Onda 0/Onda 4 descrevem a implementação inicial; esta revisão substitui o filtro IDLE integral por paginação limitada e é a referência do contrato final. Será coberta por teste real de janelas/pending recentes/antigos.
