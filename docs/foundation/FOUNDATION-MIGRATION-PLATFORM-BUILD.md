# Foundation Migration — platform-build

## Objetivo

Consolidar as responsabilidades antes mantidas em `BrunoBS/platform-build` no repositório oficial `BrunoBS/platform-libraries`, sem criar um substituto e sem manter dois registries no fluxo final.

## Antes

```text
platform-build
├── platform-parent
└── platform-dependencies
        ↓
platform-libraries
        ↓
consumers
```

Havia dois contextos de publicação no fluxo da Foundation.

## Depois — estrutura consolidada

```text
platform-libraries
├── platform-parent
├── platform-dependencies
├── platform-libraries-bom
└── modules/
    └── platform-*
        ↓
consumers
```

## Conteúdo migrado

Do `platform-build` foram incorporados ao `platform-libraries`:

- `platform-parent/pom.xml`;
- `platform-dependencies/pom.xml`.

A governança de build foi preservada, enquanto o parent deixou de gerenciar versões das capabilities.

Foram adicionados:

- `platform-libraries-bom/pom.xml`;
- documentação de consolidação, versionamento, publicação e migração;
- ADR-001 para a consolidação;
- ADR-002 para o namespace oficial `br.com.portalmanager.platform`.

As oito capabilities foram agrupadas fisicamente sob `modules/`.

## Namespace oficial

Durante o fechamento da Foundation, o namespace provisório:

```text
com.empresa.platform
```

foi substituído por:

```text
br.com.portalmanager.platform
```

A mudança abrange `groupId`, packages Java, imports e paths de código. Os `artifactId` permanecem `platform-*`.

## Versões próprias preparadas para publicação

```text
platform-dependencies   1.0.0
platform-parent         1.0.0
platform-libraries-bom  1.0.0
capabilities            1.0.0
```

As versões experimentais `1.0.3/1.1.0` usadas durante a investigação do cutover permanecem apenas como evidência histórica e não representam o baseline alvo.

## Registry

Registry oficial alvo:

`https://maven.pkg.github.com/brunobs/platform-libraries`

Registry legado, fora do fluxo oficial final:

`https://maven.pkg.github.com/brunobs/platform-build`

## Migração de consumidor — após publicação

Consumidores deverão:

1. usar `br.com.portalmanager.platform:platform-parent:1.0.0`;
2. importar `br.com.portalmanager.platform:platform-libraries-bom:1.0.0`;
3. declarar capabilities `br.com.portalmanager.platform:platform-*` sem versão;
4. configurar o registry de `platform-libraries` como fonte da Foundation;
5. executar `mvn clean verify` em checkout limpo, idealmente com repositório Maven local vazio ou isolado.

Exemplo:

```xml
<parent>
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-parent</artifactId>
    <version>1.0.0</version>
    <relativePath/>
</parent>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>br.com.portalmanager.platform</groupId>
            <artifactId>platform-libraries-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Estado do platform-build

`platform-build` não pertence à arquitetura alvo nem deve voltar ao fluxo oficial.

Por segurança operacional, ele não deve ser apagado ou arquivado antes de:

1. publicar o baseline definitivo no registry de `platform-libraries`;
2. provar consumo remoto por um serviço;
3. fechar documentalmente `FOUNDATION-GOLDEN-V1`.

Após o checkpoint, poderá ser marcado como legado/obsoleto e arquivado manualmente.

## Evidências da migração

A investigação intermediária passou por probes com `platform-dependencies:1.0.3`, `platform-parent:1.1.0` e release train `1.1.0`. Esses probes expuseram a associação antiga de packages Maven e retornaram HTTP 422 no cutover inicial. Eles são preservados em `FOUNDATION-PUBLISHING.md` como histórico.

Após a limpeza manual dos packages experimentais, o run `35542606756`, attempt 3, publicou com sucesso as coordenadas próprias em `1.0.0` ainda sob `com.empresa.platform`.

Em seguida, a Foundation adotou `br.com.portalmanager.platform`. O Verify #85, run `35544899150`, validou o reactor consolidado no namespace definitivo.

A etapa restante desta migração é publicar e consumir remotamente `br.com.portalmanager.platform:*:1.0.0`. Somente essa evidência fecha o cutover.
