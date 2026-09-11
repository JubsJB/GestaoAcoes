## MODIFIED Requirements

### Requirement: Cadastro com referências persistidas
O formulário SHALL usar Carteira existente fixa resolvida na abertura pelo contexto de entrada e permitir selecionar Ação pelo par ticker/mercado, Corretora opcional e tipo COMPRA ou VENDA. Ele MUST NOT cadastrar referências ausentes, consultar providers externos ou aceitar combinações livres de ticker e mercado.

#### Scenario: Referências disponíveis
- **WHEN** o cadastro global é aberto
- **THEN** a Carteira é resolvida pelo contexto global ou pela URL explícita validada, permanece visível e não editável, e Ações e Corretoras são carregadas pelos serviços existentes como opções identificáveis

#### Scenario: Referência obrigatória ausente
- **WHEN** não existe Carteira ou Ação selecionável
- **THEN** o formulário explica a dependência, oferece caminho à área correspondente e não envia POST

## ADDED Requirements

### Requirement: Carteira fixa da abertura ao POST
Cada abertura de cadastro SHALL capturar uma Carteira válida e mantê-la visível e não editável até cancelamento ou conclusão. Sugestão de VENDA e `carteiraId` do POST SHALL usar essa identidade capturada, nunca reler a seleção global no submit. A prévia de COMPRA SHALL preservar seu contrato independente da Carteira. Mudanças externas de contexto MUST NOT redirecionar a operação em edição a outra Carteira. Sem Carteira válida SHALL bloquear submissão e oferecer recuperação explícita.

#### Scenario: Troca global durante preenchimento
- **WHEN** o formulário abriu para A e a seleção global muda para B
- **THEN** formulário, sugestão de VENDA e POST continuam vinculados a A, identificada visivelmente, e não são reatribuídos a B

#### Scenario: Origem removida ou inválida
- **WHEN** a Carteira capturada deixa de estar disponível ou o backend rejeita sua referência
- **THEN** o formulário informa a indisponibilidade e não substitui a Carteira por um fallback nem apresenta sucesso

#### Scenario: Contratos de compra e venda preservados
- **WHEN** um formulário contextual ou global é submetido
- **THEN** reutiliza o construtor vigente: COMPRA sem preço e ordem, VENDA com preço e sem ordem, mantendo Corretora opcional, validações e bloqueio de submissão concorrente

### Requirement: Origem e retorno determinísticos de Operações
Entradas de Dashboard e detalhe de Carteira SHALL transportar Carteira e origem em URL para o fluxo em página, permitindo reconstrução após reload sem depender de history.state. Cadastro em dialog SHALL preservar origem na instância e retornar ao detalhe de origem ao fechar. URLs sem origem contextual SHALL manter retorno global para `/operacoes`. O detalhe de Operação SHALL preservar a origem contextual recebida em links e usar o DTO compatível ou GET existente após reload. Retornos SHALL usar somente destinos internos conhecidos e MUST NOT aceitar redirecionamento arbitrário.

#### Scenario: Cadastro vindo do Dashboard
- **WHEN** o usuário abre nova Operação pelo Dashboard e recarrega a página
- **THEN** a Carteira explícita validada permanece fixa e cancelar ou concluir retorna a `/dashboard?carteiraId={origemId}`

#### Scenario: Cadastro ou detalhe vindo da Carteira
- **WHEN** o usuário abre fluxo em página a partir de uma Carteira e recarrega
- **THEN** a origem válida é reconstruída e o retorno leva a `/carteiras/{origemId}`

#### Scenario: Compatibilidade global
- **WHEN** o usuário abre `/operacoes`, `/operacoes/nova` ou `/operacoes/{id}` sem origem contextual
- **THEN** a listagem continua global sem filtro implícito, o cadastro captura a Carteira válida na abertura e o retorno permanece `/operacoes`

#### Scenario: Resposta tardia da origem
- **WHEN** a operação de A conclui depois de a página ativa passar para B
- **THEN** seu DTO, erro ou atualização não contamina histórico, posições ou confirmação de B

#### Scenario: Reload de cadastro global
- **WHEN** o cadastro global captura uma Carteira válida sem origem contextual e a página é recarregada
- **THEN** o ID capturado fica representado na URL, a mesma Carteira validada é restaurada e o retorno continua `/operacoes`

#### Scenario: Origem de retorno inválida
- **WHEN** a URL contém origem desconhecida ou destino arbitrário
- **THEN** o retorno usa `/operacoes` sem redirecionamento externo; um ID contextual inválido continua exigindo recuperação explícita sem substituição silenciosa de Carteira
