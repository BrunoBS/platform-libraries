# Onda 2 — concluída

Hash, Set, List, Sorted Set, Atomic implementados conforme matriz da Onda 0. HLEN/SCARD + leitura completa em um Lua impedem corrida entre verificação do limite e leitura. LRANGE/ZRANGE requerem janelas limitadas. CAS atômico em Lua, compara bytes JSON e preserva TTL; expected null exige ausência. Contadores usam INCRBY/DECRBY.

BUILD SUCCESS em 14/10/2026 23:56:13, 38.278s. Comando verify focado no módulo com -am; log /private/tmp/platform-redis-wave2.log. Redis: 2 unitários + 11 integrações, zero falhas/erros/skips; inclui contadores concorrentes, CAS com um vencedor, tipos nativos, limites, ordenação e TTL. Sem divergências pendentes.
