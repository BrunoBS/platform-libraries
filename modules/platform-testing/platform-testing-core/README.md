# Platform Testing Core

Infraestrutura de testes compartilhada: regras ArchUnit, ciclo de vida de testes, `TestContext`, builders, factories, cenários, relógio e IDs.

Use este artefato quando precisar das convenções e utilitários genéricos. As integrações opcionais ficam nos artefatos irmãos; veja o [mapa completo de módulos](../README.md) e o [guia de uso](../docs/GUIA_DE_USO.md).

`@PlatformUnitTest` pertence ao Core. A anotação HTTP `@PlatformIntegrationTest` pertence a `platform-testing-http`; Redis, MySQL, Kafka, autorização e cloud são selecionados nos artefatos irmãos.
