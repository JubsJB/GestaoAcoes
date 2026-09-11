## Why

A rota estrutural de Corretoras ainda não permite listar, cadastrar ou consultar os registros mantidos pelo backend. Esta change introduz a primeira feature funcional do frontend sobre a foundation e o shell existentes, mantendo o backend como autoridade para CNPJ, dados cadastrais, endereço e situação cadastral.

## What Changes

- Substituir o placeholder de `/corretoras` por uma área funcional lazy-loaded com listagem, estado vazio, carregamento, erros e acesso ao cadastro e ao detalhe.
- Disponibilizar `/corretoras/nova` com Reactive Forms para solicitar somente o CNPJ na tentativa inicial de cadastro.
- Tratar exclusivamente `409/SITUACAO_CADASTRAL_NAO_ATIVA` como fluxo contextual de confirmação: informar a situação devolvida, aguardar decisão explícita e reenviar com `confirmarSituacaoCadastralNaoAtiva=true` apenas após confirmação.
- Disponibilizar `/corretoras/:id` para apresentar o contrato completo da corretora, preservando campos opcionais ausentes e a semântica de validação financeira ainda não realizada.
- Oferecer busca exata por CNPJ na listagem usando somente `GET /corretoras/por-cnpj`, conduzindo o resultado ao detalhe.
- Consumir exclusivamente `POST /corretoras`, `GET /corretoras`, `GET /corretoras/{id}` e `GET /corretoras/por-cnpj?cnpj=...` por meio da configuração central da API e do tratamento técnico de erros existente.
- Manter Material 3, responsividade e acessibilidade do shell, sem novas dependências, estado global ou integração direta com BrasilAPI/ViaCEP.
- Evoluir o requirement de destinos estruturais do shell para permitir que capabilities funcionais posteriores substituam placeholders individualmente, preservando os limites lazy e sem antecipar as demais features.

## Capabilities

### New Capabilities

- `frontend-broker-management`: Define listagem, cadastro por CNPJ, confirmação contextual de situação não ativa, busca por CNPJ, detalhe, estados visuais, responsividade e acessibilidade da área de Corretoras.

### Modified Capabilities

- `frontend-application-shell`: Permite que uma capability funcional aprovada substitua o placeholder de sua área, mantendo o shell e o limite lazy, sem tornar funcionais as demais áreas.

## Impact

- **Frontend:** feature `corretoras`, suas rotas lazy, models, serviço HTTP, páginas, componentes de apresentação estritamente necessários e testes.
- **APIs:** consumo sem alteração dos quatro endpoints vigentes de Corretoras; OpenAPI e `broker-registration` permanecem fontes de verdade.
- **Dependências:** nenhuma nova dependência planejada; Angular Material, Reactive Forms, HttpClient e signals existentes são suficientes.
- **Backend:** nenhum arquivo, endpoint, regra, provider externo ou schema será alterado.
- **OpenSpec:** nova capability `frontend-broker-management` e modificação pontual de `frontend-application-shell`; `frontend-application-foundation` permanece inalterada.
