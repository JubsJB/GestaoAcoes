## 1. Contratos e models lossless

- [x] 1.1 Criar models manuais para `EvolucaoPatrimonialResponse`, ponto temporal e patrimônio por moeda.
- [x] 1.2 Representar `patrimonioAtual` como string, `dataHoraSnapshot` como string e moeda como `BRL | USD`.
- [x] 1.3 Representar `carteiraId`, `snapshotId` e o `id` retornado pelo POST conforme a convenção de IDs numéricos seguros.
- [x] 1.4 Criar model para `SnapshotCarteiraResponse` e seus componentes monetários.
- [x] 1.5 Criar fixtures de zero, um e múltiplos snapshots, incluindo snapshot vazio e histórico multimoeda esparso.
- [x] 1.6 Testar rejeição de IDs, timestamps, moedas, listas e estruturas aninhadas inválidas.

## 2. Parser e precisão financeira

- [x] 2.1 Reutilizar `parseLosslessJson` com allowlist mínima que proteja `patrimonioAtual` em GET e POST.
- [x] 2.2 Preservar decimal longo além da precisão segura de JavaScript sem alteração de dígitos.
- [x] 2.3 Preservar escala 12, zeros significativos e notação científica válida.
- [x] 2.4 Testar parsing de coleções aninhadas, múltiplos pontos e múltiplas moedas.
- [x] 2.5 Testar snapshot vazio, evolução vazia e payload JSON malformado.
- [x] 2.6 Testar campo decimal malformado, nulo ou de tipo inesperado.
- [x] 2.7 Provar que parsing, models, labels, tooltip e histórico textual não usam `Number`, `parseFloat` ou aritmética financeira binária.
- [x] 2.8 Executar testes existentes do parser compartilhado e de Operações para proteger compatibilidade.

## 3. Service HTTP da evolução

- [x] 3.1 Criar service dedicado usando a configuração central da API.
- [x] 3.2 Implementar `GET /carteiras/{id}/evolucao-patrimonial` com response textual, parser lossless e guard estrutural.
- [x] 3.3 Implementar `POST /carteiras/{id}/snapshots` sem body, com response textual, parser lossless e guard estrutural.
- [x] 3.4 Codificar o ID no path e não enviar filtros, paginação ou query parameters especulativos.
- [x] 3.5 Preservar a normalização HTTP central, inclusive `StandardError` recebido como texto.
- [x] 3.6 Não adicionar retry HTTP automático aos métodos GET ou POST.
- [x] 3.7 Testar URL, método, ausência de body no POST, status de sucesso e parsing de cada contrato.
- [x] 3.8 Testar propagação de 404, 409, 422, códigos herdados e erro técnico.

## 4. Modelo temporal e segmentação por moeda

- [x] 4.1 Transformar a resposta em séries BRL e USD sem reordenar, agregar ou remover snapshots.
- [x] 4.2 Preservar a ordem `dataHoraSnapshot ASC` com `snapshotId ASC` como estabilizador defensivo.
- [x] 4.3 Criar segmentos independentes por moeda e interrompê-los quando a moeda estiver ausente em um snapshot.
- [x] 4.4 Garantir que lacunas não gerem zero, carry-forward, interpolação ou ligação visual indevida.
- [x] 4.5 Preservar snapshots vazios no histórico textual sem criar pontos monetários.
- [x] 4.6 Tratar uma única observação monetária como marcador isolado sem linha de tendência.
- [x] 4.7 Testar somente BRL, somente USD, BRL + USD e moeda ausente em pontos intermediários.
- [x] 4.8 Testar ausência de zero artificial, interpolação, média, agregação e métricas derivadas.

## 5. Geometria SVG controlada

- [x] 5.1 Criar helper isolado que converta uma cópia decimal somente para geometria aproximada.
- [x] 5.2 Manter no modelo geométrico referência à string autoritativa e à identidade temporal originais.
- [x] 5.3 Calcular coordenada X por instante real sem assumir granularidade diária.
- [x] 5.4 Calcular domínio e coordenada Y separadamente para cada moeda.
- [x] 5.5 Tratar domínio com valor único sem divisão por zero ou tendência artificial.
- [x] 5.6 Gerar segmentos, linhas e marcadores sem conectar gaps.
- [x] 5.7 Selecionar labels adaptativos do eixo sem remover pontos do dataset.
- [x] 5.8 Testar que o valor aproximado não alcança labels, tooltip, histórico, persistência ou requisição HTTP.
- [x] 5.9 Testar valores extremos, diferenças subpixel, timestamps repetidos defensivos e série longa.

## 6. Componente de gráfico SVG

- [x] 6.1 Criar componente SVG standalone, sem Canvas ou biblioteca gráfica.
- [x] 6.2 Renderizar gráfico “Evolução patrimonial — BRL” somente quando existir série BRL.
- [x] 6.3 Renderizar gráfico “Evolução patrimonial — USD” somente quando existir série USD.
- [x] 6.4 Usar escalas independentes e símbolos `R$`/`US$` sem total ou eixo compartilhado.
- [x] 6.5 Renderizar marcador isolado para série com um ponto e linhas somente em segmentos com múltiplos pontos.
- [x] 6.6 Aplicar estilos não cromáticos distintos e foco visível aos pontos interativos.
- [x] 6.7 Implementar tooltip usando exclusivamente timestamp e valor autoritativos.
- [x] 6.8 Tornar tooltip acionável por mouse, teclado, foco e toque quando houver interação.
- [x] 6.9 Testar DOM SVG, segmentos, marcadores, labels e inexistência de elementos artificiais.

## 7. Alternativa textual e datas

- [x] 7.1 Criar histórico textual responsivo contendo todos os snapshots em ordem.
- [x] 7.2 Exibir data e hora local suficientemente completas e indicar explicitamente o uso do horário local.
- [x] 7.3 Exibir cada moeda e `patrimonioAtual` com o formatador financeiro compartilhado.
- [x] 7.4 Identificar snapshots com `patrimonios=[]` como observações sem patrimônio.
- [x] 7.5 Manter a alternativa sempre disponível ou revelável por controle acessível com estado comunicado.
- [x] 7.6 Testar snapshots no mesmo dia, minuto e segundo, além de timestamp ISO inválido.
- [x] 7.7 Testar que o histórico textual não depende do modelo geométrico nem perde snapshots.

## 8. Seção modular de evolução

- [x] 8.1 Criar componente standalone da seção com heading “Evolução patrimonial” e descrição clara.
- [x] 8.2 Receber o contexto válido de Carteira sem duplicar seletor, query parameter ou navegação.
- [x] 8.3 Orquestrar o GET em pipeline próprio, sem acoplar sua falha ao `forkJoin` financeiro atual.
- [x] 8.4 Invalidar imediatamente dados da evolução ao mudar ou remover o contexto.
- [x] 8.5 Cancelar logicamente consultas anteriores e ignorar respostas ou erros stale.
- [x] 8.6 Compor gráficos, histórico textual, ações e estados sem ampliar responsabilidades do componente principal.
- [x] 8.7 Garantir que nenhuma consulta seja feita sem Carteira válida.

## 9. Estados da consulta

- [x] 9.1 Implementar loading acessível da evolução sem simular dados.
- [x] 9.2 Implementar empty state para `pontos=[]` com orientação para registrar o primeiro snapshot.
- [x] 9.3 Implementar estado textual de uma única observação sem sugerir tendência.
- [x] 9.4 Implementar conteúdo para múltiplos snapshots e moedas independentes.
- [x] 9.5 Representar snapshots vazios sem tratá-los como erro.
- [x] 9.6 Implementar 404 como contexto de Carteira inexistente/removida conforme padrão do Dashboard.
- [x] 9.7 Implementar erro técnico normalizado com retry explícito apenas do GET.
- [x] 9.8 Garantir que falha da evolução não apague resumo, posições ou resultados existentes.
- [x] 9.9 Testar loading, empty, single point, content, snapshot vazio, 404, erro técnico e retry.

## 10. Criação manual de snapshot

- [x] 10.1 Adicionar ação acessível “Registrar snapshot” somente com Carteira válida.
- [x] 10.2 Executar POST exclusivamente após acionamento explícito da ação.
- [x] 10.3 Manter estado de criação independente sem bloquear desnecessariamente o restante do Dashboard.
- [x] 10.4 Impedir double-submit enquanto o POST estiver pendente.
- [x] 10.5 Anunciar progresso da criação e apresentar confirmação após sucesso.
- [x] 10.6 Após 201, recarregar somente a evolução da Carteira de origem ainda selecionada.
- [x] 10.7 Tratar 404, `SNAPSHOT_CARTEIRA_DUPLICADO`, `INTEGRIDADE_DADOS_VIOLADA`, `CALCULO_POSICAO_FORA_DA_PRECISAO`, códigos herdados e erro técnico.
- [x] 10.8 Reabilitar a ação após falha e não recarregar a evolução como se houvesse sucesso.
- [x] 10.9 Provar que abrir, carregar, trocar Carteira, atualizar dados e registrar Operação nunca executam POST.

## 11. Integração com o Dashboard

- [x] 11.1 Renderizar a seção somente dentro da rota `/dashboard` e preservar `carteiraId` atual.
- [x] 11.2 Integrar a seção sem alterar cards, posições, resultados, navegação ou estados já implementados.
- [x] 11.3 Fazer “Atualizar dados” recarregar também a evolução da Carteira selecionada.
- [x] 11.4 Garantir que “Atualizar dados” não crie snapshot.
- [x] 11.5 Preservar retry local da evolução independente do reload financeiro restante.
- [x] 11.6 Testar nenhuma Carteira, seleção pendente, parâmetro inválido, seleção válida e troca de contexto.
- [x] 11.7 Testar troca durante GET, durante POST e durante reload posterior à criação.
- [x] 11.8 Testar que confirmação e respostas da Carteira anterior não contaminam a nova seleção.

## 12. Acessibilidade

- [x] 12.1 Estruturar heading, descrição, gráficos e histórico textual com semântica coerente.
- [x] 12.2 Tratar SVG como complementar e histórico textual como fonte acessível principal.
- [x] 12.3 Identificar BRL e USD por título, símbolo e estilo além de cor.
- [x] 12.4 Anunciar loading e criação com `role="status"` e/ou `aria-live="polite"`.
- [x] 12.5 Anunciar erros adequadamente sem deslocar foco de forma indevida.
- [x] 12.6 Garantir nomes acessíveis e foco visível em registrar, retry, histórico e pontos interativos.
- [x] 12.7 Garantir tooltip acessível por foco/toque quando aplicável.
- [x] 12.8 Testar teclado, ordem de foco, nomes, live regions, semântica não cromática e alternativa textual completa.

## 13. Responsividade

- [x] 13.1 Implementar SVG fluido com `viewBox` e dimensões CSS sem overflow horizontal da página.
- [x] 13.2 Organizar gráficos e detalhes completos no desktop.
- [x] 13.3 Reduzir somente labels do eixo em tablet, preservando todos os pontos.
- [x] 13.4 Empilhar gráficos, legenda e histórico adequadamente no mobile.
- [x] 13.5 Manter tooltip dentro da viewport em mouse, teclado e toque.
- [x] 13.6 Preservar todos os valores e observações na representação compacta.
- [x] 13.7 Testar estrutura desktop, tablet e mobile nos breakpoints existentes.

## 14. Performance e limites

- [x] 14.1 Manter todos os snapshots retornados no dataset, sem paginação ou filtros fictícios.
- [x] 14.2 Evitar um nó decorativo por label/gridline quando a densidade puder ser reduzida sem alterar dados.
- [x] 14.3 Não implementar downsampling financeiro, agregação temporal ou remoção de observações.
- [x] 14.4 Testar centenas e milhares de pontos em transformação/renderização proporcional ao risco.
- [x] 14.5 Registrar em documentação/testes a limitação de crescimento indefinido do histórico completo.

## 15. Testes focados

- [x] 15.1 Cobrir models, parser, guards e precisão lossless ponta a ponta.
- [x] 15.2 Cobrir service GET e POST, URLs, ausência de body, erros e ausência de retry automático.
- [x] 15.3 Cobrir transformação temporal, séries BRL/USD, gaps e snapshots vazios.
- [x] 15.4 Cobrir helper geométrico e sua fronteira de aproximação autorizada.
- [x] 15.5 Cobrir SVG com zero, um e múltiplos pontos/segmentos.
- [x] 15.6 Cobrir histórico textual, datas locais e valores autoritativos.
- [x] 15.7 Cobrir criação manual, double-submit, sucesso, reload e falhas.
- [x] 15.8 Cobrir proteção stale em GET, POST e reload após criação.
- [x] 15.9 Cobrir estados, acessibilidade e responsividade estrutural.
- [x] 15.10 Executar todos os testes focados da nova capability.

## 16. Regressão e integridade

- [x] 16.1 Executar testes completos do Dashboard e confirmar comportamento anterior preservado.
- [x] 16.2 Executar testes de rota e shell e confirmar ausência de nova rota.
- [x] 16.3 Executar testes de Operações e parser lossless compartilhado.
- [x] 16.4 Executar a suíte frontend completa e registrar testes, falhas e skips.
- [x] 16.5 Executar build frontend de produção sem instalar dependências.
- [x] 16.6 Confirmar que `package.json` e `package-lock.json` não foram alterados.
- [x] 16.7 Confirmar ausência de alterações backend, migrations e contratos HTTP.
- [x] 16.8 Executar `git diff --check` e inspecionar o diff final.
- [ ] 16.9 Atualizar Graphify após alterações de código, conforme instruções do repositório.

  Nota de aceite: DISPENSADA por decisão explícita de revisão humana. Graphify não executado e proibido nesta etapa. Trata-se de manutenção auxiliar do grafo, não de requisito funcional da proposal, design ou specs; execuções anteriores coincidiram com efeitos indesejados fora do escopo. Task encerrada para fins de aceite por dispensa, não por execução. Checkbox preservado desmarcado para distinguir dispensa de conclusão; não representa pendência de aceite.


## 17. Validação OpenSpec

- [x] 17.1 Revisar implementação contra proposal, design e todos os cenários dos deltas.
- [x] 17.2 Executar validação strict da change `implementar-evolucao-patrimonial-frontend`.
- [x] 17.3 Executar validação strict global e corrigir somente inconsistências desta change.
- [x] 17.4 Atualizar estas tasks conforme evidência real sem arquivar a change.

## 18. Validação manual

- [x] 18.1 Validar evolução somente BRL, símbolos, valores e correspondência com o payload autoritativo.
- [x] 18.2 Validar evolução somente USD e gráfico independente em `US$`.

  Nota de aceite: COMPROVADA POR EVIDÊNCIA COMBINADA. Validação manual em carteira BRL + USD comprovou gráfico USD, símbolo US$, estilo e escala independentes, histórico textual e ausência de conversão ou total combinado. Teste automatizado específico cobre somente USD e ausência de gráfico BRL. Não houve teste manual de carteira exclusivamente USD: cenário indisponível por ausência de carteira exclusiva e rate limit da Alpha Vantage.

- [x] 18.3 Validar BRL + USD com dois gráficos, sem total, conversão ou eixo compartilhado.
- [x] 18.4 Validar gaps de moeda e confirmar ausência de zero, carry-forward, interpolação e ligação visual indevida.

  Nota de aceite: COMPROVADA POR EVIDÊNCIA COMBINADA. Testes automatizados específicos verificam moeda ausente em ponto intermediário, interrupção de segmento, ausência de zero artificial, carry-forward, interpolação e conexão através do gap, preservando o snapshot temporal. Validações manuais gerais comprovaram gráficos BRL/USD independentes e apresentação normal sem conversão ou totalização. O gap não foi reproduzido manualmente; recriar a posição USD depende da Alpha Vantage limitada por rate limit. Não foram fabricados dados para este aceite.

- [x] 18.5 Validar zero snapshots, um snapshot, múltiplos snapshots e snapshot vazio.
- [x] 18.6 Validar histórico textual completo, horário local e observações próximas distinguíveis.
- [x] 18.7 Validar “Registrar snapshot”, progresso, double-submit, confirmação e reload da evolução.
- [x] 18.8 Validar erros 404, 409, 422 e técnico, além de retry explícito sem retry automático.
- [x] 18.9 Validar “Atualizar dados” recarregando evolução sem criar snapshot.
- [x] 18.10 Validar troca de Carteira durante GET e durante POST/reload sem conteúdo stale.
- [x] 18.11 Validar mouse, teclado, toque, foco, tooltip, anúncios e alternativa textual.
- [x] 18.12 Validar desktop, tablet e mobile sem perda de dados ou overflow horizontal da página.
