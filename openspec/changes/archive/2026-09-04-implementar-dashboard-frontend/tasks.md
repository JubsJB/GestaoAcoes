## 1. Contratos e models

- [x] 1.1 Criar os types manuais `ResumoCarteiraResponse` e `ResumoMoedaResponse` com IDs numéricos e campos `BigDecimal` textuais.
- [x] 1.2 Criar o type manual `PosicaoResponse` com todos os campos do backend, mantendo data como string e valores decimais como string.
- [x] 1.3 Criar o type manual `ResultadoRealizadoResponse` com resultado decimal textual.
- [x] 1.4 Reutilizar os enums/tipos existentes de mercado e definir moeda estritamente como `BRL | USD` sem conversão implícita.
- [x] 1.5 Adicionar testes de tipo/fixtures que representem fielmente respostas somente BRL, somente USD e mistas.

## 2. Infraestrutura lossless compartilhada

- [x] 2.1 Extrair ou criar parser JSON lossless compartilhado sem acoplar o Dashboard ao helper específico de Operações.
- [x] 2.2 Configurar allowlist mínima para todos os campos `BigDecimal` de resumo, posições e resultados realizados.
- [x] 2.3 Preservar compatibilidade da API lossless usada por Operações por meio de adaptador fino ou migração controlada.
- [x] 2.4 Implementar formatação textual monetária para BRL e USD sem converter o valor preservado para `number`.
- [x] 2.5 Implementar formatação textual de quantidade e percentual, incluindo sinais positivos e negativos.
- [x] 2.6 Testar decimal longo além da precisão segura de JavaScript, zeros de escala e casas significativas.
- [x] 2.7 Testar valores negativos, zero, payloads aninhados, coleções e JSON malformado.
- [x] 2.8 Provar por testes que os formatadores não usam `Number`, `parseFloat` nem aritmética binária financeira.
- [x] 2.9 Executar testes lossless de Operações para comprovar ausência de regressão após o compartilhamento.

## 3. Service HTTP do Dashboard

- [x] 3.1 Criar service financeiro do Dashboard usando a configuração central da API.
- [x] 3.2 Implementar `GET /carteiras/{id}/resumo` com response textual e parser lossless.
- [x] 3.3 Implementar `GET /carteiras/{id}/posicoes` com response textual e parser lossless.
- [x] 3.4 Implementar `GET /carteiras/{id}/resultados-realizados` com response textual e parser lossless.
- [x] 3.5 Garantir por teste que `/patrimonio` e `/evolucao-patrimonial` não são chamados.
- [x] 3.6 Preservar erros do interceptor/normalizador central sem tratamento HTTP duplicado e sem retry automático.
- [x] 3.7 Testar URLs, IDs codificados, métodos GET, parsing de sucesso e propagação de erro de cada método.

## 4. Rota e substituição do placeholder

- [x] 4.1 Criar o componente funcional standalone do Dashboard dentro da feature lazy existente.
- [x] 4.2 Atualizar `dashboard.routes.ts` para carregar o componente funcional sob `/dashboard`.
- [x] 4.3 Remover o placeholder somente após não existir referência ativa a ele.
- [x] 4.4 Testar carregamento lazy, renderização dentro do shell e ausência do texto/componente placeholder.
- [x] 4.5 Confirmar que `/` continua redirecionando exatamente para `/dashboard`.

## 5. Seleção de carteira

- [x] 5.1 Reutilizar `CarteirasService.listar()` para descobrir os contextos disponíveis.
- [x] 5.2 Implementar seletor com label persistente e opções identificadas por nome e ID quando necessário.
- [x] 5.3 Implementar empty state com CTA para `/carteiras/nova` quando a lista estiver vazia.
- [x] 5.4 Selecionar automaticamente quando existir exatamente uma Carteira e não houver seleção válida.
- [x] 5.5 Manter estado aguardando seleção quando existirem duas ou mais Carteiras sem contexto válido.
- [x] 5.6 Garantir por teste que a primeira Carteira não é escolhida arbitrariamente em coleção múltipla.
- [x] 5.7 Invalidar imediatamente o conteúdo financeiro anterior ao trocar a seleção.

## 6. Query parameter

- [x] 6.1 Ler `carteiraId` reativamente da query string e validá-lo como identificador positivo antes de uso.
- [x] 6.2 Validar a existência do ID contra a coleção retornada antes de iniciar consultas financeiras.
- [x] 6.3 Atualizar a URL ao selecionar ou trocar Carteira sem perder a rota do Dashboard.
- [x] 6.4 Restaurar a seleção válida por acesso direto, reload e navegação pelo histórico.
- [x] 6.5 Apresentar estado recuperável para parâmetro malformado sem realizar consulta financeira.
- [x] 6.6 Apresentar estado recuperável para ID ausente da coleção e permitir nova seleção.
- [x] 6.7 Testar parâmetro ausente com zero, uma e múltiplas Carteiras.
- [x] 6.8 Testar parâmetro válido, malformado, inexistente e Carteira removida concorrentemente.

## 7. Cards de resumo

- [x] 7.1 Renderizar um grupo de cards por `ResumoMoedaResponse` sem criar acumulador entre itens.
- [x] 7.2 Exibir patrimônio atual, custo total das posições, resultado não realizado total e rentabilidade autoritativos.
- [x] 7.3 Exibir BRL com `R$` e USD com `US$` em agrupamentos distintos.
- [x] 7.4 Exibir sinal e texto semântico para resultado/rentabilidade sem depender somente de cor.
- [x] 7.5 Exibir `posicoes.length` somente como “posições abertas”, sem associá-lo a cálculo financeiro.
- [x] 7.6 Testar resumo somente BRL, somente USD, BRL + USD e inexistência de card total combinado.
- [x] 7.7 Testar que nenhum indicador apresentado é recalculado no componente.

## 8. Posições abertas

- [x] 8.1 Criar seção de posições com ticker, empresa, mercado/moeda, quantidade, preço médio e cotação atual.
- [x] 8.2 Exibir valor atual, resultado não realizado e rentabilidade diretamente do DTO.
- [x] 8.3 Apresentar data/hora de cotação com o formatador temporal compartilhado quando incluída no layout.
- [x] 8.4 Formatar posição BRASIL em BRL e posição EUA em USD sem conversão.
- [x] 8.5 Não criar link ou ação para detalhe de posição inexistente.
- [x] 8.6 Criar empty state específico para Carteira sem posições abertas.
- [x] 8.7 Testar posição brasileira, americana, coleção mista, coleção vazia e decimais longos.

## 9. Resultados realizados

- [x] 9.1 Criar seção que exiba ticker, empresa, mercado/moeda e resultado realizado por Ação.
- [x] 9.2 Preservar cada item e sua moeda sem redução, soma ou totalização frontend.
- [x] 9.3 Criar empty state normal para ausência de resultados realizados.
- [x] 9.4 Testar lista preenchida, lista vazia, moedas simultâneas, valores positivos e negativos.
- [x] 9.5 Provar por teste a inexistência de total de resultado realizado calculado no frontend.

## 10. Estados de loading, erro e vazio

- [x] 10.1 Implementar loading anunciado para a lista de Carteiras sem simular dados financeiros.
- [x] 10.2 Implementar erro normalizado da lista com retry explícito.
- [x] 10.3 Implementar estado aguardando seleção para múltiplas Carteiras.
- [x] 10.4 Implementar loading financeiro que não mantenha dados de outro contexto como atuais.
- [x] 10.5 Tratar respostas financeiras vazias como estados normais e distintos.
- [x] 10.6 Tratar 404 financeiro como contexto inexistente/removido com recuperação pela seleção.
- [x] 10.7 Apresentar erros 409 e 422 usando mensagem/detalhes normalizados do backend.
- [x] 10.8 Apresentar erro técnico normalizado com retry explícito.
- [x] 10.9 Testar o caminho real `HttpErrorResponse → interceptor → normalização → service → Dashboard → mensagem`.
- [x] 10.10 Garantir por teste que não existe retry HTTP automático.

## 11. Reload e proteção de contexto

- [x] 11.1 Orquestrar resumo, posições e resultados realizados em paralelo após contexto válido.
- [x] 11.2 Implementar ação “Atualizar dados” que repita as três consultas para a seleção atual.
- [x] 11.3 Cancelar logicamente o contexto anterior quando carteira ou query parameter mudar.
- [x] 11.4 Impedir que respostas atrasadas de uma Carteira sobrescrevam a seleção nova.
- [x] 11.5 Não reconciliar respostas parciais ou diferenças temporais por cálculo frontend.
- [x] 11.6 Testar reload, paralelismo, troca durante loading, resposta atrasada e erro de uma consulta.

## 12. Navegação

- [x] 12.1 Adicionar ação para acessar `/carteiras/{id}` da Carteira selecionada.
- [x] 12.2 Adicionar ação para registrar Operação usando o fluxo contextual existente e a mesma Carteira.
- [x] 12.3 Garantir que ações contextuais permaneçam indisponíveis sem seleção válida.
- [x] 12.4 Testar navegação para Carteira, nova Operação, retorno e preservação do `carteiraId` no Dashboard.

## 13. Acessibilidade

- [x] 13.1 Estruturar título principal, headings de seções e agrupamentos por moeda semanticamente.
- [x] 13.2 Associar label e instrução acessível ao seletor de Carteira.
- [x] 13.3 Anunciar loading, erros e mudanças relevantes com regiões dinâmicas não invasivas.
- [x] 13.4 Garantir nomes acessíveis e foco visível em retry, reload e links contextuais.
- [x] 13.5 Associar cabeçalhos aos dados tabulares ou fornecer alternativa semântica equivalente.
- [x] 13.6 Comunicar resultados positivos e negativos por sinal/texto além de cor.
- [x] 13.7 Testar navegação por teclado, nomes acessíveis, hierarquia de headings e regiões de estado.

## 14. Responsividade e identidade visual

- [x] 14.1 Reutilizar PageHeader, feedback, cards, superfícies e tokens visuais existentes.
- [x] 14.2 Organizar cards por moeda sem sugerir soma visual entre BRL e USD.
- [x] 14.3 Adaptar posições e resultados para viewport compacto sem scroll horizontal obrigatório da página.
- [x] 14.4 Preservar todas as informações essenciais na representação compacta.
- [x] 14.5 Testar classes/estrutura responsiva e ausência de overflow nos breakpoints adotados pelo projeto.

## 15. Testes focados do Dashboard

- [x] 15.1 Cobrir lista de Carteiras em loading, sucesso, erro, vazio, uma e múltiplas opções.
- [x] 15.2 Cobrir seleção, troca de contexto e todas as variantes de query parameter.
- [x] 15.3 Cobrir cards BRL, USD e mistos sem total combinado.
- [x] 15.4 Cobrir posições BRASIL/EUA, empty state e contagem estrutural.
- [x] 15.5 Cobrir resultados realizados preenchidos/vazios e ausência de totalização.
- [x] 15.6 Cobrir decimais longos, valores negativos e preservação lossless ponta a ponta.
- [x] 15.7 Cobrir loading financeiro, 404, 409, 422, erro técnico e retry.
- [x] 15.8 Cobrir reload, paralelismo e proteção contra resposta obsoleta.
- [x] 15.9 Cobrir navegação, responsividade e acessibilidade do componente.
- [x] 15.10 Executar os testes focados de models, parser, formatadores, service, rota e página do Dashboard.

## 16. Regressão

- [x] 16.1 Executar testes do shell e confirmar rota inicial, navegação e lazy loading preservados.
- [x] 16.2 Executar testes de Carteiras e confirmar listagem, detalhe e diálogos inalterados.
- [x] 16.3 Executar testes de Operações, incluindo parser lossless, payloads e fluxo contextual.
- [x] 16.4 Executar testes de Ações e confirmar comportamento inalterado.
- [x] 16.5 Executar testes de Corretoras e confirmar comportamento inalterado.
- [x] 16.6 Executar a suíte frontend completa e registrar arquivos, testes, falhas e skips.

## 17. Build e integridade técnica

- [x] 17.1 Executar build frontend de produção sem instalar dependências.
- [x] 17.2 Registrar warnings de budget separadamente quando o build permanecer bem-sucedido.
- [x] 17.3 Confirmar que `package.json` e `package-lock.json` não foram alterados.
- [x] 17.4 Confirmar ausência de alterações em backend, migrations e contratos HTTP.
- [x] 17.5 Executar `git diff --check` e inspecionar o diff final da change.

## 18. Validação OpenSpec

- [x] 18.1 Revisar implementação contra proposal, design e todos os cenários dos deltas.
- [x] 18.2 Executar validação strict da change `implementar-dashboard-frontend`.
- [x] 18.3 Executar validação strict global e corrigir somente inconsistências desta change.
- [x] 18.4 Atualizar estas tasks conforme evidência real sem arquivar a change.

## 19. Validação manual

- [x] 19.1 Validar visualmente Dashboard somente BRL e conferir símbolos, valores e ausência de recálculo.
- [x] 19.2 Validar visualmente Dashboard somente USD e conferir separação monetária.
- [x] 19.3 Validar BRL + USD simultaneamente e confirmar inexistência de total combinado ou conversão.
- [x] 19.4 Validar troca de Carteira e confirmar invalidação imediata dos dados antigos.
- [x] 19.5 Validar acesso direto e reload com query parameter válido, ausente, inválido e inexistente.
- [x] 19.6 Validar Carteira sem posições e ausência de resultados realizados.
- [x] 19.7 Validar navegação para a Carteira e registro contextual de Operação.
- [x] 19.8 Validar reload explícito e recuperação após erro.
- [x] 19.9 Validar layout desktop sem excesso visual nem mistura de moedas.
- [x] 19.10 Validar layout responsivo sem perda de informação essencial ou overflow da página.
- [x] 19.11 Validar uso prático por teclado, foco visível, anúncios de loading/erro e semântica não dependente de cor.
