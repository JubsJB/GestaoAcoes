## Why

As operacoes ja identificam corretoras opcionalmente, mas a posicao consolidada nao informa como a quantidade foi atribuida entre elas. Uma leitura informativa permite consultar essa distribuicao e seus deficits sem confundi-la com custodia comprovada ou alterar a contabilidade.

## What Changes

- Consultar distribuicao derivada de compras menos vendas por Carteira + Acao + Corretora, incluindo "Sem corretora informada".
- Reproduzir cronologia, detectar negativos atuais e historicos, manter avisos apos regularizacao e identificar inconsistencias de grupos zerados sem linha de quantidade zero.
- Reconciliar exatamente todas as quantidades com a quantidade consolidada no mesmo snapshot de leitura.
- Disponibilizar GET separado somente para posicoes consolidadas abertas e apresentar o resultado no detalhe da Carteira.
- Preservar operacoes opcionais/retroativas, validacao de venda consolidada, cotacoes, contratos existentes e todos os calculos financeiros.
- Sem escrita em GET, saneamento, nova tabela de posicao, transferencias ou validacao de venda por corretora.

## Capabilities

### New Capabilities

- `portfolio-broker-distribution`: leitura informativa das quantidades atribuidas, diagnosticos historicos e reconciliacao por posicao aberta.

### Modified Capabilities

- `frontend-portfolio-management`: permitir consulta separada e apresentacao da distribuicao em Posicoes abertas, preservando dados basicos, historico e posicoes consolidadas.

## Impact

Backend: leitura de Operacao/Corretora, replay quantitativo existente, novo servico/DTO/resource e OpenAPI. Frontend: servico de consulta lossless, modelos/guards e painel no detalhe da Carteira. Nenhuma mudanca em OperacaoCreateRequest, OperacaoResponse, PosicaoResponse ou nas regras de CalculadoraPosicao.

Fontes preservadas: PRD RF16/RF17/RN10/RN13-RN18; README regras financeiras; specs broker-registration, operation-registration, operation-query, portfolio-position, realized-result, frontend-operation-management, frontend-portfolio-management e frontend-dashboard-management. Apenas as duas capabilities declaradas exigem deltas: cadastro de corretoras, operacoes, financeiro e Dashboard nao recebem novo comportamento.

Esta etapa cria somente artefatos OpenSpec para revisao. Implementacao, migrations, dados, Git e archive nao autorizados.

Contrato tecnico fechado em contrato-http.md:schemas/exemplos,todos os campos obrigatorios/arrays,separacao de diagnosticos,primeira ocorrencia,ordenacao ordinal,errosHTTP e snapshots independentes. Os ajustes autorizados nao alteram as18decisoes de negocio originais nem ampliam capabilities/escopo financeiro.
