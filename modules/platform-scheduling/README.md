# platform-scheduling

POC isolada de proteção distribuída para tarefas agendadas do Spring, conforme o refinamento [`platform-scheduling-shedlock-refinamento-v1.md`](https://github.com/BrunoBS/guideline-java/blob/20b9414b4cc6b560cbd6916adb1f38cac3ed0709/docs/refinamento/20-platform-scheduling/platform-scheduling-shedlock-refinamento-v1.md).

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

O Spring 7 encapsula as tarefas em um wrapper de acompanhamento antes de chamar o `TaskScheduler`; o decorator usa o nome público da tarefa, preservado pelo `toString()` do wrapper, para recuperar classe e método sem acessar campos privados. Tipos de `Runnable` sem nome estável e métodos cuja identidade inclua classe proxy gerada falham no registro em vez de executar sem lock. As métricas são exportadas quando a aplicação fornece um `MeterRegistry`. KeepAlive não elimina o risco de concorrência após perda do Redis durante uma execução.

O módulo não está homologado para consumo. A integração com o reactor nesta branch serve apenas à POC. A conclusão exige executar o build completo com Java 25/Maven 3.9.9+, Redis via Testcontainers e fechar a matriz de validação descrita no refinamento. Só depois registrar resultados e limites na Golden Platform, incorporar a capability ao branch principal e publicar o artefato.

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)
