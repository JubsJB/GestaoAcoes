## MODIFIED Requirements

### Requirement: Atualização manual de cotação somente no detalhe
O detalhe SHALL oferecer uma ação explícita para solicitar `PATCH /acoes/{id}/cotacao` sem dados de cotação fornecidos pelo usuário. A aplicação MUST NOT atualizar automaticamente, repetir ou agendar essa operação e MUST NOT disponibilizá-la na listagem.
 A restrição somente no detalhe SHALL limitar a atualização individual na área de Ações; o lote manual orientado à Carteira no Dashboard SHALL permanecer disponível. Ambas SHALL usar a mesma regra backend. Sucesso HTTP MUST NOT prometer cotação nova ou consulta externa efetiva; feedback SHALL informar processamento e estado retornado.

#### Scenario: Solicitação manual
- **WHEN** o usuário aciona a atualização no detalhe
- **THEN** a aplicação envia exatamente um PATCH sem payload funcional de cotação, timestamp, ticker, mercado, moeda ou nome

#### Scenario: Operação em andamento
- **WHEN** o PATCH está em andamento
- **THEN** a ação permanece ocupada, anunciada semanticamente e protegida contra outra solicitação concorrente

#### Scenario: Atualização concluída
- **WHEN** o PATCH responde com `AcaoResponse`
- **THEN** a aplicação substitui o DTO local pela resposta, atualiza cotação e data/hora exibidas e comunica sucesso sem GET adicional

#### Scenario: Falha preserva estado anterior
- **WHEN** o PATCH falha
- **THEN** ticker, cotação, moeda e data/hora anteriormente exibidos são preservados, o erro é apresentado e uma nova tentativa só pode ocorrer por ação manual posterior

#### Scenario: Limite de requisições
- **WHEN** o PATCH responde `429` com `code=LIMITE_REQUISICOES_EXCEDIDO`
- **THEN** a aplicação informa o limite no contexto da atualização sem retry automático ou repetição temporizada

#### Scenario: Ticker canônico divergente
- **WHEN** o PATCH responde `409` com `code=TICKER_CANONICO_DIVERGENTE`
- **THEN** a aplicação informa que a atualização não foi aplicada, preserva o ticker e o DTO locais e não tenta aceitar a substituição

#### Scenario: Indisponibilidade ou timeout
- **WHEN** o PATCH responde com indisponibilidade ou timeout do provider
- **THEN** a aplicação preserva a última cotação exibida, mantém `message` e `details` do erro e libera a ação ao finalizar

### Requirement: Apresentação responsiva sem cálculo financeiro
A área SHALL preservar ticker, empresa, mercado, moeda e cotação em tabela/cards e detalhe responsivos, sem cálculo financeiro ou FX. Referência real SHALL usar tipo: DATA somente dd/MM/yyyy sem deslocamento por timezone; INSTANTE data/hora em pt-BR no timezone do navegador; DESCONHECIDA mensagem explícita. Obtenção MAY ser apresentada separadamente no detalhe quando disponível. dataHoraCotacao legado MUST NOT ser apresentado como referência real. A cotação SHALL ser última conhecida, sem promessa de tempo real ou classificação automática de defasagem. MUST NOT introduzir polling, coleta automática ou mudanças nos campos cadastrais. Hierarquia, nome legível, moeda, reflow e atualização manual SHALL ser preservados.

#### Scenario: Mercado e moeda apresentados
- **WHEN** uma Ação é exibida
- **THEN** mercado aparece amigavelmente e cotação é formatada conforme BRL ou USD recebido

#### Scenario: Data e cotação
- **WHEN** cotação e referência tipada são exibidas
- **THEN** apresenta a precisão disponível ou referência desconhecida; não usa legado para inventar data de mercado

#### Scenario: Domínio aprovado e precisão na apresentação temporal
- **WHEN** uma nova referência válida do backend é apresentada em listagem/detalhe de Ações, posições ou desempenho por ativo
- **THEN** DATA SHALL preservar calendário gregoriano real de 0001 a 9999, ano de quatro dígitos e apresentação civil sem Date ou deslocamento de dia
- **AND** INSTANTE SHALL ser apresentado no timezone do navegador preservando o DTO original, offsets com segundos e frações sem arredondamento que altere minuto; DESCONHECIDA SHALL ter mensagem explícita sem fallback
- **AND** cotacaoObtidaEm SHALL ser apresentada separadamente, com nulo como "Não informada", sem relógio do navegador para validação

#### Scenario: Validação de aquisição distinta da leitura persistida
- **WHEN** valida uma nova aquisição ou um EstadoCotacao completo dessa aquisição
- **THEN** SHALL respeitar calendário/hora/offset, precisão exata em microssegundos, piso inclusivo 1970 de INSTANTE e referenciaInstante <= cotacaoObtidaEm própria, com tolerância ZERO, sem Date para decidir validade financeira ou precisão
- **AND** guards de leitura de DTOs persistidos MUST NOT rejeitar silenciosamente registros do contrato anterior mediante aplicação indiscriminada desses novos limites; textos originais e parsing lossless SHALL permanecer intactos

#### Scenario: Nome empresarial longo
- **WHEN** o nome da empresa é extenso
- **THEN** ele quebra de forma legível sem truncamento obrigatório ou overflow horizontal

#### Scenario: Viewport compacto
- **WHEN** listagem ou detalhe é exibido em tela compacta
- **THEN** cards, feedbacks e ações permanecem legíveis e operáveis em coluna

#### Scenario: Listagem comparável
- **WHEN** Ações são exibidas no desktop
- **THEN** ticker, empresa, mercado, moeda, cotação e referência temporal permanecem comparáveis com as ações existentes

#### Scenario: Uma representação ativa
- **WHEN** a listagem é exibida em largura compacta
- **THEN** somente os cards participam da árvore acessível e do foco, preservando todos os campos e ações
