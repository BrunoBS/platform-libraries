# F4 — Remoção do platform-crud

## Objetivo

Remover fisicamente o `platform-crud` somente após o desacoplamento do `platform-catalog`, conforme o plano da Foundation.

## Pré-condição

A F3 foi concluída com o `platform-catalog` sem dependência Maven ou conceitual das abstrações genéricas do CRUD e com o reactor validado.

## Inventário e classificação

Antes da remoção foram pesquisados os termos obrigatórios:

- `platform-crud`
- `BaseCrud`
- `CrudControllerSupport`
- `CrudValidationException`
- `CrudMessage`
- `crud.`

As ocorrências remanescentes estavam concentradas no próprio módulo `platform-crud`. A busca de código remota não apontou consumidores residuais; como o GitHub sinalizou `incomplete_results=true`, essa busca foi tratada como evidência auxiliar e complementada pela remoção física e pela validação Maven completa.

## Remoção

O módulo `platform-crud` foi removido da lista `<modules>` do POM raiz e todos os arquivos sob `platform-crud/` foram excluídos.

Após a exclusão, a árvore da branch `refactor/golden-foundation` apresentou zero entradas sob `platform-crud/`.

O gerenciamento de dependência do `platform-crud` já havia sido removido do `platform-parent` na F1.

## Validação

Head validado: `3e66e6ad0426233a56d88154d7ea503ea4e02c7a`.

GitHub Actions Verify #38, run `35529729954`:

`mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify`

Resultado:

- `RequireJavaVersion`: sucesso.
- `DependencyConvergence`: sucesso em todos os módulos.
- `platform-catalog`: sucesso após a remoção física do CRUD.
- reactor completo: `BUILD SUCCESS`.
- `platform-crud` não participa mais do reactor.

## Conclusão da F4

O `platform-crud` foi removido fisicamente da Foundation. O `platform-catalog` permanece como capacidade específica e independente.

A próxima etapa é a F5 — revisão completa das libraries sobreviventes, sem introduzir grandes reescritas não relacionadas.
