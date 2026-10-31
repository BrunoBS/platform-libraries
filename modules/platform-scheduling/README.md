# platform-scheduling

Capability de proteção distribuída para tarefas agendadas do Spring, conforme o refinamento [`platform-scheduling-shedlock-refinamento-v1.md`](https://github.com/BrunoBS/guideline-java/blob/main/docs/refinamento/20-platform-scheduling/platform-scheduling-shedlock-refinamento-v1.md).

O lock Redis é namespaced por `spring.application.name` e pela classe e assinatura do método. Agendamentos repetidos do mesmo método compartilham o lock, evitando execuções simultâneas quando a configuração muda entre instâncias.

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

Com o locking habilitado, toda tarefa Spring `@Scheduled` é protegida por padrão. Para excluir uma tarefa específica, use a anotação da lib:

```java
import br.com.portalmanager.platform.library.scheduling.annotation.DisableScheduledLock;
import org.springframework.scheduling.annotation.Scheduled;

@Scheduled(fixedDelay = 60_000)
@DisableScheduledLock
void refreshLocalCache() {
    // Esta tarefa roda sem lock distribuído.
}
```

A anotação vale somente para o método marcado; as demais tarefas continuam protegidas.

## Tarefas programáticas

Tarefas registradas por código devem usar `PlatformTaskScheduler` e informar um nome estável. Todas as instâncias que executam a mesma tarefa devem usar o mesmo nome:

```java
import br.com.portalmanager.platform.library.scheduling.task.PlatformTaskScheduler;
import java.time.Duration;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
class ParameterReconciliationJob {
    private final PlatformTaskScheduler scheduler;

    ParameterReconciliationJob(PlatformTaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostConstruct
    void register() {
        scheduler.scheduleWithFixedDelay(
                "parameter-reconciliation", this::reconcileParameters, Duration.ofMinutes(1));
    }

    private void reconcileParameters() {
        // Executar uma reconciliação síncrona.
    }
}
```

O ID identifica a tarefa lógica e deve ser igual em todas as instâncias. Lambdas e `Runnable`s só podem ser registrados por essa fachada, que associa o ID ao lock; o acesso direto a `TaskScheduler` não é uma API de coordenação de tarefas programáticas.

## Cobertura e limites

O contrato de execução automática cobre tarefas síncronas e reativas `@Scheduled` com cron, `fixedRate` e `fixedDelay`, inclusive em múltiplas instâncias, `@Schedules` repetidos, schedulers nomeados e proxies Spring. Para métodos reativos, a lib bloqueia a thread do scheduler até o `Publisher` completar ou falhar; assim, o lock cobre o processamento inteiro. `@DisableScheduledLock` deixa a tarefa marcada fora desse comportamento e ela segue a assinatura reativa nativa do Spring.

Como a thread do scheduler fica ocupada durante o fluxo, configure um pool com capacidade suficiente para as demais tarefas. O tempo máximo do lock deve superar a duração máxima esperada; `keep-alive` pode renovar o lease enquanto a tarefa permanece ativa.

Os testes de integração também exercitam a indisponibilidade real e temporária do Redis enquanto um lock está ativo, o comportamento fail-closed e a retomada após a recuperação do Redis, além da expiração de um lease abandonado para simular a queda do detentor. Redis indisponível na inicialização impede o contexto de subir; `enabled=false` mantém a execução sem coordenação.

O Spring 7 encapsula as tarefas em um wrapper de acompanhamento antes de chamar o `TaskScheduler`; o decorator usa a identidade pública da tarefa, sem acessar campos privados. O router default do Spring é mantido por um bean com ciclo de vida gerenciado. No shutdown ele para de aceitar novas execuções, interrompe o scheduler default e aguarda as execuções ativas liberarem seus locks antes de o provider Redis ser destruído. As métricas são exportadas quando a aplicação fornece um `MeterRegistry`. KeepAlive não elimina o risco de concorrência após perda de conectividade ou perda dos dados do Redis durante uma execução; tarefas devem ser idempotentes.

Aplicações consumidoras devem declarar explicitamente `platform-scheduling` e `platform-redis`; não dependa da transitividade. O pipeline completo `mvn clean verify` com Java 25, incluindo Redis via Testcontainers, é a validação de referência. A publicação do artefato ocorre no fluxo de release após o merge aprovado; `clean verify` não publica.

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)
