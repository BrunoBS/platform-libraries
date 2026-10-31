# platform-scheduling

POC isolada de proteção distribuída para tarefas agendadas do Spring, conforme o refinamento [`platform-scheduling-shedlock-refinamento-v1.md`](https://github.com/BrunoBS/guideline-java/blob/main/docs/refinamento/20-platform-scheduling/platform-scheduling-shedlock-refinamento-v1.md).

O lock Redis é namespaced por `spring.application.name` e pela classe, assinatura do método e tipo de agendamento. Agendamentos repetidos do mesmo método compartilham o lock, evitando execuções simultâneas quando a configuração muda entre instâncias.

## Configuração experimental

O serviço consumidor deve declarar diretamente `platform-scheduling` e `platform-redis`, além de configurar o Redis segundo o guia de `platform-redis`.

```yaml
golden:
  scheduling:
    distributed-lock:
      enabled: true
      lock-at-most-for: PT10M
      lock-at-least-for: PT0S
      keep-alive: true
```

Esses valores ainda não são defaults homologados. Com `enabled=true`, Redis precisa responder na inicialização e falhas ao obter o lock não executam a tarefa. Com `enabled=false`, as tarefas rodam sem coordenação e um WARN é emitido.

## Limites da POC

O escopo validado é `@Scheduled` síncrono: cron, `fixedRate` e `fixedDelay`, inclusive em múltiplas instâncias, com schedulers nomeados e proxies Spring. Tarefas reativas não estão suportadas: em cron e `fixedRate`, o Spring pode retornar do `Runnable` após iniciar a assinatura, antes de o publisher terminar, liberando o lock cedo demais. Tarefas registradas programaticamente só funcionam quando o Spring fornece uma identidade estável; lambdas e `Runnable`s sem classe/método identificável falham no registro, sem fallback desprotegido.

O Spring 7 encapsula as tarefas em um wrapper de acompanhamento antes de chamar o `TaskScheduler`; o decorator usa a identidade pública da tarefa, sem acessar campos privados. O router default do Spring é mantido por um bean com ciclo de vida gerenciado para encerrar seu executor junto com o contexto. As métricas são exportadas quando a aplicação fornece um `MeterRegistry`. KeepAlive não elimina o risco de concorrência após perda do Redis durante uma execução; tarefas devem ser idempotentes.

Esta branch é utilizável para avaliação da POC, mas o módulo ainda não é um artefato publicado nem uma capability homologada da Golden Platform. Aplicações de teste devem declarar explicitamente `platform-scheduling` e `platform-redis`; não se deve depender da transitividade. O pipeline completo `mvn clean verify` com Java 25, incluindo Redis via Testcontainers, é a validação de referência. Os resultados finais e os limites devem ser registrados antes de promover o módulo ou publicar o artefato.

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)
