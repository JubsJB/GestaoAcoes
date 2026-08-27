## MODIFIED Requirements

### Requirement: Persistência e resposta do cadastro
O sistema SHALL persistir `id`, ticker normalizado ou canônico confirmado, nome da empresa, mercado, moeda, cotação atual e data/hora da cotação. A mesma operação transacional SHALL persistir exatamente uma observação histórica inicial com a Ação, a cotação e o timestamp aceitos, reutilizando exatamente `Acao.cotacaoAtual` e `Acao.dataHoraCotacao` sem uma segunda chamada ao provider. O cadastro concluído SHALL responder `201 Created`, retornar o DTO completo persistido e incluir `Location: /acoes/{id}`.
O sistema SHALL armazenar `ticker` em `VARCHAR(30)`, `nome_empresa` em `VARCHAR(255)` e `cotacao_atual` em `NUMERIC(19,6)`. O sistema SHALL NOT persistir `origemCotacao`, pois o provider é determinado pelo mercado.

#### Scenario: Cadastro concluído
- **WHEN** todas as validações locais e externas passam e a persistência é concluída
- **THEN** o sistema responde `201 Created` com todos os campos da Ação e `Location` baseado no identificador gerado
- **AND** existe exatamente uma observação histórica correspondente ao estado inicial
- **AND** nenhuma segunda chamada ao provider é executada para criar essa observação

#### Scenario: Falha antes da gravação
- **WHEN** qualquer validação ou integração falha antes da seção de persistência
- **THEN** nenhuma Ação ou observação histórica parcial é gravada

#### Scenario: Falha na observação inicial
- **WHEN** a Ação seria persistida, mas sua observação histórica falha
- **THEN** toda a transação é revertida e nenhuma Ação parcial permanece

### Requirement: Concorrência e persistência final consistente
O sistema SHALL manter chamadas externas fora da transação de persistência. A seção final SHALL serializar atualizações concorrentes da mesma Ação e MUST NOT substituir uma cotação com referência temporal mais nova por outra igual ou mais antiga. Quando uma candidata posterior for aplicada, estado atual e observação histórica SHALL ser persistidos atomicamente; candidatas iguais ou anteriores MUST NOT criar histórico.

#### Scenario: Duas atualizações concorrentes com timestamps diferentes
- **WHEN** duas solicitações para a mesma Ação obtêm cotações válidas com referências temporais diferentes
- **THEN** o estado final mantém a cotação de timestamp mais recente, independentemente da ordem em que as chamadas externas terminem
- **AND** o histórico não registra candidata rejeitada por não ser posterior ao estado encontrado sob lock

#### Scenario: Falha antes da seção final
- **WHEN** a busca inicial, a chamada externa ou a validação falha
- **THEN** nenhuma transação de escrita altera a Ação ou seu histórico

#### Scenario: Persistência concluída com histórico atômico
- **WHEN** uma nova cotação temporalmente aplicável é persistida
- **THEN** `cotacaoAtual`, `dataHoraCotacao` e exatamente uma observação histórica correspondente são confirmados na mesma transação
