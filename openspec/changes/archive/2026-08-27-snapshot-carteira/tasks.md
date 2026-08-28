## 1. Schema e modelo persistente

- [x] 1.1 Criar `006-create-snapshot-carteira.yaml` com as tabelas `snapshot_carteira` e `snapshot_carteira_moeda`, PKs, tipos, NOT NULL e rollback explícito na ordem correta das FKs.
- [x] 1.2 Adicionar ao changeSet 006 as FKs obrigatórias sem `ON DELETE CASCADE`, as uniques Carteira+timestamp e snapshot+moeda e os checks de moeda e patrimônio positivo, sem índices redundantes.
- [x] 1.3 Incluir somente o changeSet 006 após o 005 no changelog master e comprovar que os changeSets 001–005 permanecem byte a byte inalterados.
- [x] 1.4 Criar `SnapshotCarteira` exclusivamente com `id`, associação LAZY obrigatória e unidirecional para Carteira e `dataHoraSnapshot` obrigatório.
- [x] 1.5 Criar `SnapshotCarteiraMoeda` exclusivamente com `id`, associação LAZY obrigatória e unidirecional para o snapshot, moeda obrigatória e `patrimonioAtual` `BigDecimal(38,12)` positivo.
- [x] 1.6 Preservar ausência de coleções em Carteira e SnapshotCarteira, ausência de cascade JPA e ausência de estado mutável que permita atualizar snapshots persistidos.

## 2. Repositories e erros de domínio

- [x] 2.1 Criar `SnapshotCarteiraRepository` somente com persistência e verificação de existência por Carteira necessárias à criação e à proteção de DELETE.
- [x] 2.2 Criar `SnapshotCarteiraMoedaRepository` somente com persistência e leituras mínimas necessárias aos testes, sem API histórica especulativa.
- [x] 2.3 Adicionar `SNAPSHOT_CARTEIRA_DUPLICADO` e mapear exclusivamente a violação de `carteira_id + data_hora_snapshot` para `409 Conflict`, preservando rollback integral.
- [x] 2.4 Adicionar `CARTEIRA_POSSUI_SNAPSHOTS` para a proteção explícita de exclusão, sem criar outros códigos de erro.

## 3. DTOs, mapper e contrato REST

- [x] 3.1 Criar o DTO monetário do snapshot contendo exclusivamente `moeda` e `patrimonioAtual`.
- [x] 3.2 Criar `SnapshotCarteiraResponse` contendo exclusivamente `id`, `carteiraId`, `dataHoraSnapshot` e `patrimonios` imutáveis e ordenados por moeda.
- [x] 3.3 Criar mapper dedicado que projete entidades persistidas e componentes para a resposta sem realizar cálculo financeiro, consulta ou normalização numérica.
- [x] 3.4 Expor `POST /carteiras/{carteiraId}/snapshots` sem body, retornando `201 Created`, DTO completo e `Location: /carteiras/{carteiraId}/snapshots/{snapshotId}`, sem criar GET/PATCH/PUT/DELETE de snapshot.

## 4. Criação transacional do snapshot

- [x] 4.1 Criar `SnapshotCarteiraService` com transação única de escrita em `Isolation.REPEATABLE_READ`, sem lock pessimista, `@Version` ou chamada externa.
- [x] 4.2 Obter `dataHoraSnapshot` exatamente uma vez do `Clock` injetável, normalizar para UTC e fazê-lo antes da primeira leitura financeira.
- [x] 4.3 Reutilizar `PosicaoService.listarPorCarteira` exatamente uma vez, preservando 404 da Carteira inexistente, replay oficial e fetch plan sem N+1.
- [x] 4.4 Reutilizar `AgregadorPosicoesPorMoeda.agregar` exatamente uma vez sobre a mesma lista de posições, sem modificar o agregador ou duplicar cálculo monetário.
- [x] 4.5 Persistir primeiro o pai e depois zero ou mais filhos explicitamente na mesma transação, selecionando somente `moeda` e `patrimonioAtual` dos totais oficiais.
- [x] 4.6 Produzir snapshot pai sem filhos e resposta `patrimonios=[]` quando não houver posições abertas, sem criar moeda ou patrimônio zero artificial.
- [x] 4.7 Garantir rollback do pai e de todos os filhos para qualquer falha de componente, precisão, integridade ou colisão temporal, sem resposta parcial.

## 5. Preservação financeira e proteção de exclusão

- [x] 5.1 Comprovar que o snapshot persiste `patrimonioAtual` exatamente equivalente ao GET de patrimônio para o mesmo estado, usando escala 12, `UNNECESSARY` e precisão 38 já aplicadas pelo agregador.
- [x] 5.2 Manter `PatrimonioService`, `ResumoCarteiraService`, `HistoricoCotacao`, Operações, preço médio e resultado realizado fora das dependências e cálculos do snapshot.
- [x] 5.3 Estender `DELETE /carteiras/{id}` para verificar Operações primeiro e snapshots depois sob o lock curto vigente, retornando respectivamente `CARTEIRA_POSSUI_OPERACOES` ou `CARTEIRA_POSSUI_SNAPSHOTS`, sem cascade ou remoção histórica.

## 6. Testes de entidade, repository e Liquibase

- [x] 6.1 Testar `SnapshotCarteira` válido, Carteira obrigatória, timestamp obrigatório, normalização UTC, associação LAZY unidirecional e ausência de cascade/coleção.
- [x] 6.2 Testar `SnapshotCarteiraMoeda` válido, snapshot/moeda/patrimônio obrigatórios, patrimônio positivo, precisão exata `NUMERIC(38,12)` e ausência de cascade.
- [x] 6.3 Testar unique Carteira+timestamp, unique snapshot+moeda, múltiplos timestamps da mesma Carteira, mesmo timestamp entre Carteiras diferentes e snapshot pai sem filhos.
- [x] 6.4 Testar aplicação e rollback do changeSet 006 no H2 e inicialização completa com Liquibase e `hibernate.ddl-auto=validate`.

## 7. Testes funcionais da criação e exclusão

- [x] 7.1 Testar criação somente BRL, somente USD e BRL+USD, incluindo ordenação determinística e ausência de soma/conversão multimoeda.
- [x] 7.2 Testar múltiplas posições por moeda, posição encerrada omitida, venda parcial e novo ciclo usando exclusivamente a posição aberta resultante.
- [x] 7.3 Testar Carteira vazia, POST sem body, 201, DTO completo, timestamp UTC idêntico entre entidade e resposta, Location canônica e 404 para Carteira inexistente.
- [x] 7.4 Testar múltiplos snapshots no mesmo dia e patrimônio idêntico em timestamps distintos, além de `409 / SNAPSHOT_CARTEIRA_DUPLICADO` no mesmo timestamp sem alterar o snapshot existente.
- [x] 7.5 Testar DELETE de Carteira com snapshot, snapshot vazio, somente Operações, ambas as condições e nenhuma condição, comprovando precedência de `CARTEIRA_POSSUI_OPERACOES` e preservação de todo histórico.

## 8. Concorrência, arquitetura e verificação final

- [x] 8.1 Testar snapshot concorrente com registro de Operação, comprovando visão integral anterior ou posterior sem estado financeiro misto.
- [x] 8.2 Testar snapshot concorrente com PATCH de cotação, comprovando visão consistente e ausência de consulta a `HistoricoCotacao` ou provider.
- [x] 8.3 Testar dois snapshots concorrentes com timestamps distintos e colisão temporal exata, comprovando atomicidade, unicidade e ausência de registros parciais.
- [x] 8.4 Criar testes arquiteturais que comprovem uma chamada a PosicaoService, uma agregação, ausência de PatrimonioService, segundo replay, segundo cálculo monetário, provider, lock novo, N+1 e endpoints públicos não aprovados.
- [x] 8.5 Executar regressões direcionadas de Carteira, Operações, posição, patrimônio, resumo/rentabilidade, resultado realizado, stock-registration e stock-quote-history.
- [x] 8.6 Executar a suíte completa e `mvnw clean verify`, validando Liquibase/Hibernate no H2 e PostgreSQL quando o ambiente estiver disponível, documentando eventual indisponibilidade sem inventar configuração.
- [x] 8.7 Atualizar e consultar o Graphify, validar `snapshot-carteira` e o conjunto global OpenSpec em modo strict e revisar `git diff --check`, `git diff` e `git status` sem executar operação Git proibida.
