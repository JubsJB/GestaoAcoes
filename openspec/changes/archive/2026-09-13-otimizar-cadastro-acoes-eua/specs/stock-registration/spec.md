## MODIFIED Requirements

### Requirement: Validação externa da existência do ticker
O sistema SHALL concluir o cadastro somente quando o provedor correspondente confirmar a existência do ticker normalizado no mercado solicitado. Resultado aproximado da busca SHALL NOT ser aceito como confirmação de um ticker diferente.

#### Scenario: Ticker brasileiro existente
- **WHEN** a BRAPI confirma o ticker solicitado e fornece seus dados obrigatórios
- **THEN** o sistema considera o ticker válido para `BRASIL`

#### Scenario: Ticker americano exato e pertencente ao mercado dos EUA
- **WHEN** a Alpha Vantage devolve correspondência exata do símbolo, identifica o mercado dos Estados Unidos e fornece os dados obrigatórios
- **THEN** o sistema considera o ticker válido para `EUA`

#### Scenario: Somente correspondência aproximada
- **WHEN** a Alpha Vantage devolve resultados de busca, mas nenhum possui símbolo normalizado exatamente igual ao solicitado e mercado dos Estados Unidos
- **THEN** o sistema responde `404 Not Found` com código de ticker inexistente e não consulta nem persiste dados de outro símbolo

#### Scenario: Ticker não encontrado
- **WHEN** o provedor correspondente não encontra o ticker no mercado solicitado
- **THEN** o sistema responde `404 Not Found` com erro padronizado e não persiste Ação

#### Scenario: BRAPI informa substituição por ticker canônico
- **WHEN** a BRAPI informa explicitamente que o ticker solicitado foi renomeado e devolve outro ticker canônico
- **THEN** o sistema usa o ticker canônico normalizado para a verificação final de duplicidade, persistência e resposta

#### Scenario: Busca americana exata com nome utilizável
- **WHEN** a Alpha Vantage confirma símbolo exato e mercado ou região compatível e a busca já fornece nome utilizável
- **THEN** o sistema consulta a última cotação sem executar consulta adicional de dados da empresa

#### Scenario: Busca americana exata sem nome utilizável
- **WHEN** a Alpha Vantage confirma símbolo exato e mercado ou região compatível, mas a busca não fornece nome utilizável
- **THEN** o sistema SHALL responder 422 com DADOS_EXTERNOS_INCOMPLETOS sem consultar OVERVIEW ou cotação e sem persistir a Ação
