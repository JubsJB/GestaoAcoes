## MODIFIED Requirements

### Requirement: Referência temporal da cotação
A Ação SHALL separar referenciaTipo (INSTANTE, DATA ou DESCONHECIDA), referenciaInstante, referenciaData e cotacaoObtidaEm. INSTANTE SHALL usar OffsetDateTime normalizado para UTC; DATA SHALL usar LocalDate sem inventar hora ou offset. DESCONHECIDA SHALL manter ambas as referências nulas. cotacaoObtidaEm SHALL representar o instante em que a observação de cotação atualmente armazenada foi obtida validamente pela aplicação e MAY ser nula no legado; não representa o maior instante histórico de consulta. dataHoraCotacao SHALL permanecer obrigatório como campo legado de compatibilidade, sem autoridade de comparação ou apresentação da referência real. No cadastro com preço válido e referência ausente, o estado inicial MAY ser DESCONHECIDA; isso MUST NOT fabricar referência de mercado.

A precisão temporal persistível de referenciaInstante e cotacaoObtidaEm SHALL ser de microssegundos: o instante deve ser exatamente representável com até seis casas decimais nos segundos. Casas excedentes exclusivamente zero SHALL ser aceitas sem perda; nanos significativos não representáveis SHALL ser rejeitados com 422 REFERENCIA_TEMPORAL_INVALIDA antes de disponibilizar CotacaoCandidata válida ou persistir estado/histórico. A mesma validação SHALL abranger referências externas e obtenção do Clock, em cadastro e atualização individual/lote. MUST NOT arredondar, truncar ou delegar ao banco a correção da precisão. UTC SHALL preservar o instante; LocalDate não é afetada.

#### Scenario: Precisão temporal exatamente persistível
- **WHEN** referência INSTANTE ou obtenção possui segundos inteiros, até seis casas ou somente zeros após a sexta casa
- **THEN** aceita o mesmo instante em UTC, sem perda ou arredondamento

#### Scenario: Domínio civil de nova candidata
- **WHEN** o provider fornece DATA gregoriana real com ano de exatamente quatro dígitos entre 0001 e 9999, inclusive 0001-01-01 e 9999-12-31
- **THEN** a fronteira CotacaoCandidata SHALL aceitar a referência sem piso 1970, limite futuro relativo à obtenção, Date, timezone ou calendário de mercado
- **AND** calendário impossível, ano zero, negativo ou expandido SHALL causar REFERENCIA_TEMPORAL_INVALIDA, nunca DESCONHECIDA

#### Scenario: Domínio financeiro de INSTANTE da própria candidata
- **WHEN** uma nova candidata fornece INSTANTE com calendário, hora e offset válidos e precisão exata em microssegundos
- **THEN** SHALL exigir instante real maior ou igual a 1970-01-01T00:00:00Z e menor ou igual à cotacaoObtidaEm dessa mesma candidata, com tolerância ZERO e normalização UTC sem perda
- **AND** um microssegundo abaixo do piso ou posterior à obtenção SHALL causar REFERENCIA_TEMPORAL_INVALIDA antes de escrita de Ação/histórico; igualdade e offsets equivalentes SHALL ser aceitos

#### Scenario: Obtenção e legado não herdam limites da referência
- **WHEN** cadastro, PATCH ou lote valida a candidata
- **THEN** cotacaoObtidaEm SHALL pertencer à aquisição capturada pelo Clock backend, exatamente em microssegundos, sem herdar automaticamente o piso financeiro da referência
- **AND** dataHoraCotacao SHALL permanecer legado sem novos limites financeiros ou fallback; GET/mapping/cooldown/reuso MUST NOT fabricar ou reinterpretar obtenção

#### Scenario: Registro aceito pelo contrato anterior
- **WHEN** um registro anteriormente persistido é retornado pela API
- **THEN** os novos limites de aquisição MUST NOT converter, apagar ou reconstruir sua referência, fabricar obtenção ou provocar rejeição silenciosa da leitura
- **AND** a validação estrutural de leitura SHALL ser tratada separadamente da validação financeira de nova candidata, sem saneamento automático

#### Scenario: Precisão temporal não persistível
- **WHEN** referência de mercado ou obtenção do Clock possui nanos significativos além de microssegundos, como .123456789
- **THEN** rejeita com REFERENCIA_TEMPORAL_INVALIDA antes da persistência, sem candidata válida, Ação ou histórico parcial

#### Scenario: Provider fornece timestamp confiável da cotação
- **WHEN** o cadastro recebe instante confiável do preço
- **THEN** persiste INSTANTE em UTC e obtenção separada; mantém dataHoraCotacao legado

#### Scenario: Provider não fornece timestamp utilizável
- **WHEN** o cadastro recebe somente data civil, ou nenhuma referência utilizável
- **THEN** preserva DATA sem hora quando fornecida, ou DESCONHECIDA quando ausente; o relógio preenche somente obtenção e compatibilidade legada

#### Scenario: Valor temporal na resposta
- **WHEN** o cadastro é concluído
- **THEN** o DTO inclui a referência tipada e obtenção além do campo legado ISO-8601 UTC

### Requirement: Persistência e resposta do cadastro
O sistema SHALL persistir `id`, ticker normalizado ou canônico confirmado, nome da empresa, mercado, moeda, cotação atual e data/hora da cotação. A mesma operação transacional SHALL persistir exatamente uma observação histórica inicial com a Ação, a cotação e o timestamp aceitos, reutilizando exatamente `Acao.cotacaoAtual` e `Acao.dataHoraCotacao` sem uma segunda chamada ao provider. O cadastro concluído SHALL responder `201 Created`, retornar o DTO completo persistido e incluir `Location: /acoes/{id}`.
O sistema SHALL armazenar `ticker` em `VARCHAR(30)`, `nome_empresa` em `VARCHAR(255)` e `cotacao_atual` em `NUMERIC(19,6)`. O sistema SHALL NOT persistir `origemCotacao`, pois o provider é determinado pelo mercado.
 A persistência SHALL incluir os novos campos temporais na Ação e na observação inicial com valores correspondentes, preservando a referência real disponível ou DESCONHECIDA e o instante de obtenção separado; nenhuma chamada adicional SHALL ser feita para o histórico.

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

### Requirement: Validação da nova cotação e preservação da Ação
A atualização SHALL exigir identidade compatível, preço presente, numérico, positivo e exatamente representável em NUMERIC(19,6), e referência de mercado confiável. Uma candidata aplicável SHALL modificar somente preço e metadados temporais de cotação, preservando id, ticker, nomeEmpresa, mercado e moeda. Cotação ou identidade inválida SHALL preservar o estado. Nome externo diferente MUST NOT modificar a empresa persistida.

#### Scenario: Nova cotação válida
- **WHEN** a candidata possui identidade compatível, preço válido e referência inicial confiável ou posterior do mesmo tipo
- **THEN** persiste preço e metadados temporais sem modificar os campos cadastrais

#### Scenario: Nome externo diferente
- **WHEN** o provider devolve nome da empresa diferente do nome persistido durante uma atualização válida
- **THEN** o sistema preserva `nomeEmpresa` e usa os dados externos somente para validar e obter a cotação

#### Scenario: Cotação ausente, não numérica, zero ou negativa
- **WHEN** o provider não fornece uma cotação positiva utilizável
- **THEN** o sistema responde `422 Unprocessable Entity` com código `COTACAO_INDISPONIVEL` e preserva integralmente a Ação

#### Scenario: Cotação fora da precisão suportada
- **WHEN** a nova cotação não pode ser representada exatamente em `NUMERIC(19,6)`
- **THEN** o sistema responde `422 Unprocessable Entity` com código `COTACAO_FORA_DA_PRECISAO`, sem truncar, arredondar ou persistir o valor

### Requirement: Semântica temporal e cotação numericamente igual
Atualização individual e lote SHALL compartilhar uma regra. Baseline DESCONHECIDA e candidata confiável SHALL estabelecer baseline e classificar ATUALIZADA com motivo REFERENCIA_INICIAL_ESTABELECIDA, sem afirmar ordem contra o legado desconhecido. Referência posterior do mesmo tipo SHALL classificar ATUALIZADA mesmo com preço igual. Igual e mesmo preço retornados por NOVA CONSULTA REAL válida ao provider SHALL classificar SEM_NOVA_COTACAO sem alterar cotacaoAtual, referência ou dataHoraCotacao legado e sem histórico; cotacaoObtidaEm SHALL avançar para o instante dessa obtenção somente se posterior ao persistido, ou ser preenchida se ainda nula. Na mesma observação (referência e preço iguais), instante igual ou anterior MUST NOT regredir obtenção. Baseline inicial ou referência posterior aceita SHALL substituir a observação inteira com cotacaoAtual, tipo, referência e cotacaoObtidaEm própria da candidata, mesmo que esta obtenção seja anterior à da observação substituída. MUST NOT usar max entre obtenções de observações diferentes. cotacaoObtidaEm MUST NOT ordenar referências de mercado ou desempatar tipos incompatíveis. A comparação SHALL ocorrer com estado relido sob lock, inclusive para execução atrasada. Reuso e cooldown MUST NOT atualizar cotacaoObtidaEm, preço/referência ou criar histórico: não constituem nova obtenção válida. Anterior SHALL classificar SEM_NOVA_COTACAO preservando preço, referência e obtenção associada. Igual e preço diferente SHALL classificar FALHA por conflito temporal. DATA versus INSTANTE SHALL classificar FALHA com motivo TIPO_REFERENCIA_INCOMPATIVEL sem conversões artificiais e sem alterar cotacaoObtidaEm. Referência ausente ou inválida na atualização SHALL classificar FALHA de validação temporal, inclusive para baseline desconhecida. No PATCH individual, falhas temporais SHALL usar erro padronizado 422; no lote SHALL ser resultado por ativo. dataHoraCotacao MUST NOT decidir ordenação; nas escritas aceitas SHALL manter compatibilidade com instante de mercado quando INSTANTE, ou obtenção quando DATA.

#### Scenario: Timestamp confiável do provider
- **WHEN** o provider retorna instante confiável
- **THEN** compara INSTANTE em UTC com baseline do mesmo tipo

#### Scenario: Referência posterior obtida antes da observação substituída
- **WHEN** o estado possui DATA 2026-10-01, preço 100 e obtenção 15:05Z, e chega candidata DATA 2026-10-02, preço 102 e obtenção 15:04Z
- **THEN** aceita a observação integral da candidata, com DATA 2026-10-02, preço 102 e obtenção 15:04Z; MUST NOT aplicar max entre as obtenções de observações diferentes
- **AND** a mesma política vale para INSTANTE e baseline inicial; somente referência/preço iguais exigem monotonicidade local de obtenção

#### Scenario: Fallback temporal da aplicação
- **WHEN** a candidata de atualização não fornece referência temporal confiável
- **THEN** preserva a Ação e retorna FALHA temporal; nenhum relógio substitui a referência

#### Scenario: Mesmo valor com referência temporal posterior
- **WHEN** a candidata possui mesmo preço e referência posterior do mesmo tipo
- **THEN** persiste a referência posterior e obtenção, cria histórico e retorna sucesso individual ou ATUALIZADA no lote

#### Scenario: Resultado sem referência temporal posterior
- **WHEN** a candidata possui referência igual com mesmo preço ou referência anterior
- **THEN** preserva preço/referência e retorna sucesso individual ou SEM_NOVA_COTACAO no lote, sem histórico; somente nova consulta real válida de referência/preço iguais avança obtenção posterior, enquanto referência anterior preserva também a obtenção

### Requirement: Concorrência e persistência final consistente
Providers SHALL ser chamados fora da transação de persistência. A seção final SHALL adquirir lock da Ação, reler o estado e aplicar a comparação compartilhada, inclusive lote versus lote e lote versus individual. Candidata inicial confiável ou posterior aceita SHALL persistir estado e exatamente uma observação atomicamente. Igual, anterior, falha ou tipo incompatível MUST NOT criar histórico nem substituir informação mais recente.

#### Scenario: Duas atualizações concorrentes com timestamps diferentes
- **WHEN** duas candidatas do mesmo tipo confiável concorrem e a antiga termina depois da nova
- **THEN** o estado e histórico aceito preservam a maior referência; a resposta antiga não regride o estado

#### Scenario: Falha antes da seção final
- **WHEN** a busca inicial, a chamada externa ou a validação falha
- **THEN** nenhuma transação de escrita altera a Ação ou seu histórico

#### Scenario: Persistência concluída com histórico atômico
- **WHEN** uma baseline inicial confiável ou referência posterior é aceita
- **THEN** preço, metadados temporais e observação correspondente são confirmados na mesma transação

### Requirement: Schema versionado e validado
O schema SHALL evoluir por novos changeSets Liquibase compartilhados entre PostgreSQL e H2, com ddl-auto=validate. A transição temporal SHALL adicionar campos em Ação e histórico sem editar changeSets anteriores; MUST NOT criar tabelas de posição ou estruturas fora desta evolução. Preço e campo legado SHALL ser preservados.

#### Scenario: Banco sem a tabela de Ação
- **WHEN** a aplicação inicia sobre banco compatível que já recebeu os changeSets anteriores
- **THEN** o Liquibase cria a tabela de Ação e suas constraints antes de o Hibernate validar o mapeamento

#### Scenario: Inicialização dos testes
- **WHEN** os testes de integração iniciam com H2 efêmero
- **THEN** o mesmo changelog master é executado e o Hibernate valida a estrutura sem usar `create`, `update` ou `create-drop`

## ADDED Requirements

### Requirement: Comparações temporais e primeira baseline verificáveis
O sistema SHALL decidir sobre o estado relido sob lock e preservar o estado em conflitos ou dados inválidos.

#### Scenario: Baseline legada
- **WHEN** o preço legado é 250 com referência DESCONHECIDA e chega preço 252 com DATA confiável
- **THEN** estabelece a primeira baseline, cria observação e classifica ATUALIZADA sem inferir data do legado

#### Scenario: Conflito na mesma referência
- **WHEN** a mesma DATA ou INSTANTE chega com preço diferente
- **THEN** classifica FALHA e preserva preço, referência e obtenção

#### Scenario: Tipos não comparáveis nos dois sentidos
- **WHEN** baseline DATA recebe INSTANTE ou baseline INSTANTE recebe DATA
- **THEN** classifica FALHA TIPO_REFERENCIA_INCOMPATIVEL; preserva preço, referência e obtenção; não converte nem reduz a precisão

#### Scenario: Ausência temporal
- **WHEN** uma atualização recebe preço válido sem referência confiável
- **THEN** classifica FALHA e preserva inclusive baseline desconhecida

#### Scenario: Nova obtenção da mesma observação
- **WHEN** nova consulta real válida retorna mesma DATA ou INSTANTE e mesmo preço, com obtenção posterior à persistida
- **THEN** classifica SEM_NOVA_COTACAO, avança somente cotacaoObtidaEm e preserva preço, referência, legado e histórico

#### Scenario: Obtenção igual ou anterior
- **WHEN** nova consulta real válida retorna mesma referência e preço com obtenção igual ou anterior à persistida
- **THEN** classifica SEM_NOVA_COTACAO e preserva inclusive cotacaoObtidaEm, sem histórico

#### Scenario: Obtenção concorrente atrasada
- **WHEN** duas consultas reais válidas da mesma referência/preço concorrem e a obtenção mais antiga entra na seção final depois da mais recente
- **THEN** compara sob lock e nunca regride cotacaoObtidaEm nem cria histórico ou conta ATUALIZADA

### Requirement: Transição temporal aditiva e contratos preservados
A migration SHALL adicionar campos sem modificar changeSets anteriores, preservar cotacaoAtual e dataHoraCotacao, e marcar legado sem evidência como DESCONHECIDA com referências e obtenção nulas. DTOs SHALL adicionar referenciaMercadoCotacao {tipo, instante, data} e cotacaoObtidaEm sem remover campos públicos. A cotação SHALL continuar global à Ação. Atualizar cotação MUST NOT alterar Operacao, precoUnitario, quantidade, preço médio, custo histórico, resultado realizado, replay ou histórico de operações; snapshots anteriores SHALL permanecer imutáveis. Nenhuma classificação automática de defasagem SHALL ser introduzida.

#### Scenario: Legado preservado
- **WHEN** a migration encontra timestamp de origem não comprovada
- **THEN** mantém preço e campo legado sem reinterpretar como mercado ou consulta comprovada

#### Scenario: Efeito em outra Carteira
- **WHEN** Carteira A atualiza AAPL também presente na Carteira B
- **THEN** a próxima leitura de B usa a cotação global nova com suas próprias operações

#### Scenario: Separação financeira
- **WHEN** a cotação é atualizada
- **THEN** somente avaliações dependentes da cotação e futuros snapshots manuais podem refletir a mudança
