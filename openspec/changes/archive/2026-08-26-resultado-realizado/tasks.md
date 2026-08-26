## 1. Confirmar baseline e contratos preservados

- [x] 1.1 Confirmar o contrato atual de Operações, posição consolidada, DELETE de Carteira e PATCH de cotação antes de alterar código.
- [x] 1.2 Confirmar a cronologia `dataOperacao ASC, ordemNoDia ASC`, o uso técnico posterior de `id` e a constraint de ordem por Carteira+Ação.
- [x] 1.3 Confirmar o método cronológico e o `@EntityGraph` atuais de `OperacaoRepository`, registrando a baseline de queries e ausência de N+1.
- [x] 1.4 Registrar a baseline de entidades, schema, changeSets 001–004, changelog, dependências, configurações e `ddl-auto=validate`.

## 2. Evoluir o replay financeiro único

- [x] 2.1 Estender o resultado interno de `CalculadoraPosicao` com `resultadoRealizado` e `possuiVenda`, preservando `posicao`, `falha` e consumidores atuais.
- [x] 2.2 Calcular cada VENDA antes da mutação contábil por `(precoUnitarioVenda - precoMedioInterno) × quantidadeVendida`, sem gerar resultado em COMPRA.
- [x] 2.3 Acumular resultados internos sem normalização por VENDA e preservar o acumulado através de venda parcial, zeramento e novos ciclos.
- [x] 2.4 Manter `validarQuantidade` quantitativo com valores neutros nos novos campos e preservar sua tradução para `POSICAO_INSUFICIENTE`.

## 3. Aplicar precisão e falhas financeiras

- [x] 3.1 Usar o preço médio interno em escala 24, quantidade exata e somente `BigDecimal`, sem consultar o preço médio projetado de escala 12.
- [x] 3.2 Normalizar somente o acumulado final para escala 12 com `HALF_EVEN` e representar zero como `0.000000000000`.
- [x] 3.3 Validar precisão máxima 38 e produzir falha `CALCULO_FORA_DA_PRECISAO` sem truncamento ou resultado parcial.
- [x] 3.4 Preservar `HISTORICO_INCONSISTENTE` para saldo, cronologia, agrupamento, tipo, quantidade ou preço persistidos inválidos.

## 4. Implementar contrato e orquestração

- [x] 4.1 Criar `ResultadoRealizadoResponse` somente com `acaoId`, `ticker`, `nomeEmpresa`, `mercado`, `moeda` e `resultadoRealizado`.
- [x] 4.2 Criar `ResultadoRealizadoMapper` como projeção pura de Ação e acumulado já calculado.
- [x] 4.3 Criar `ResultadoRealizadoService` read-only em `REPEATABLE_READ` para validar Carteira, carregar, agrupar, reproduzir, filtrar Ações com VENDA e ordenar a resposta.
- [x] 4.4 Adicionar somente `GET /carteiras/{carteiraId}/resultados-realizados` ao `CarteiraResource`, sem body, filtros, paginação, endpoint por Ação ou `Location`.

## 5. Testar fórmula, precisão e sinais

- [x] 5.1 Cobrir COMPRA sem resultado e VENDAS com lucro `200`, prejuízo `-150` e preço igual ao médio produzindo zero não nulo.
- [x] 5.2 Cobrir múltiplas COMPRAS antes da VENDA e comprovar uso do preço médio interno vigente, inclusive periódico em escala 24.
- [x] 5.3 Cobrir múltiplas VENDAS com sinais diferentes, soma antes da normalização final e exemplo acumulado `40.000000000000`.
- [x] 5.4 Cobrir `HALF_EVEN`, escala final 12, valores de fronteira, precisão acima de 38 e `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`.

## 6. Testar vendas, ciclos, mercados e isolamento

- [x] 6.1 Cobrir VENDA parcial, custo proporcional, preço médio remanescente e COMPRA posterior sem incorporação do resultado realizado.
- [x] 6.2 Cobrir VENDA total com acumulação antes do zeramento e posição encerrada ainda presente na consulta de resultados.
- [x] 6.3 Cobrir novo ciclo e múltiplos ciclos, preservando somente o acumulado histórico e reiniciando quantidade, custo e preço médio.
- [x] 6.4 Cobrir múltiplas Ações e Carteiras, BRASIL/BRL, EUA/USD, quantidade fracionária, isolamento e ausência de conversão ou soma entre moedas.

## 7. Testar service, HTTP e efeitos colaterais

- [x] 7.1 Cobrir `200 OK` com DTO completo, ordenação por mercado/ticker/ação e ausência de campos de detalhamento por VENDA e `Location`.
- [x] 7.2 Cobrir Carteira sem Operações, somente COMPRAS e Ação sem VENDA com `[]` ou omissão, além de acumulado zero ainda presente.
- [x] 7.3 Cobrir Carteira inexistente com `404` e histórico persistido inconsistente com `409 / HISTORICO_OPERACOES_INCONSISTENTE`, sem resposta parcial.
- [x] 7.4 Verificar ausência de escrita, save, Clock, provider, PATCH interno, lock pessimista, query por VENDA/Ação e alteração de cotação.

## 8. Testar persistência, performance e concorrência

- [x] 8.1 Cobrir no repository a ordem cronológica global e o carregamento de `Operacao.acao` sem consulta adicional por Ação.
- [x] 8.2 Validar contagem de queries para múltiplas Ações/VENDAS e confirmar uma leitura do histórico sem N+1.
- [x] 8.3 Cobrir consulta concorrente com registro de Operação e confirmar snapshot integral anterior ou posterior sob `REPEATABLE_READ`.
- [x] 8.4 Confirmar em H2 e PostgreSQL quando disponível que Liquibase/Hibernate usam somente o schema vigente sem entidade, tabela ou migration de resultado.

## 9. Executar regressões e verificar entrega

- [x] 9.1 Executar regressão completa de registro/consulta de Operações, VENDA retroativa, `POSICAO_INSUFICIENTE` e DELETE protegido da Carteira.
- [x] 9.2 Executar regressão completa de posição consolidada, resultado não realizado, rentabilidade percentual e posição zerada omitida.
- [x] 9.3 Executar regressão do PATCH de cotação para BRASIL/EUA, timestamp monotônico, concorrência e ticker canônico divergente.
- [x] 9.4 Executar testes direcionados, suíte completa e `clean verify`; validar OpenSpec strict, atualizar Graphify e auditar `git diff --check`, `git diff` e `git status`.


