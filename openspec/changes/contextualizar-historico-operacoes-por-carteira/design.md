## Context

Ver proposal.md para motivação e escopo. A análise dirigida pelo Graphify identificou OperacoesListPageComponent, CarteiraContextService, CarteiraNavigationService e operationReturnUrl. A lista atual chama listar() uma vez e ignora contexto; listarPorCarteira(id) já existe. Ambos os GETs retornam OperacaoResponse pelo mesmo mapper, em ordem dataOperacao/ordemNoDia/id. O parser existente preserva decimais como strings; não há conversão de DTO ou cálculo novo necessário.

CarteiraContextService publica state$/signals, valida a coleção, compartilha inicialização e resolve URL > memória > preferência > menor ID existente. CarteiraNavigationService sincroniza Dashboard e detalhe de Carteira, mas não a lista de Operações. O formulário captura Carteira fixa e normaliza sua URL; o helper de retorno reconhece dashboard/carteira. O detalhe mantém identidade por ID e pode usar extras.info com DTO compatível, mas sua origem durável depende da URL.

Baseline documental: requisitos consolidados de frontend-operation-management (Listagem cronológica e somente leitura; Origem e retorno determinísticos de Operações) e frontend-application-shell (Contexto global leve de Carteira; Precedência e preferência local de Carteira). A change anterior ainda possui deltas não sincronizados: esta evolução incorpora o acesso pelo menu e substitui explicitamente seus cenários de histórico global, sem editar a change anterior. As demais cláusulas de preservação do shell continuam vigentes; esta mudança é a exceção posterior autorizada para consulta/contexto/retorno de Operações.

## Goals / Non-Goals

**Goals:** sincronizar URL, seleção e histórico; impedir respostas obsoletas; preservar identidade capturada e retorno determinístico; reutilizar os contratos existentes.

**Non-Goals:** adicionar estado financeiro ao contexto global, store/framework, endpoint, transformação monetária, opção agregada ou alterar fluxo de dialogs de Dashboard/Carteira. Não reinterpretar a mudança anterior como defeito de implementação. Não modificar specs consolidadas, README ou PRD nesta preparação.

## Decisions

1. **Consulta contextual existente.** OperacoesListPageComponent usa listarPorCarteira(id), mantendo listar()/buscarPorId()/cadastrar() intactos. Filtrar o GET global foi descartado por transportar dados desnecessários e contradizer o contrato aprovado.
2. **URL durável.** Adicionar binding de listagem em CarteiraNavigationService seguindo o padrão do Dashboard; limitar o match à rota exata `/operacoes`. Sem parâmetro, normalizar com replaceUrl e preservar outros parâmetros. Trocas explícitas criam entrada de histórico; normalização automática não persiste fallback como escolha explícita. Comparar ID/URL evita loops e consultas duplicadas. Excluir lista contextual do reset genérico resolveUrl(null) enquanto seu binding é responsável pela URL. Não estender esse binding indiscriminadamente a `/operacoes/nova` ou `/operacoes/:id`.
3. **Uma fonte de seleção.** Reutilizar CarteiraContextService e a inicialização compartilhada; não consultar a coleção separadamente nem criar seletor local. A página usa active().nome como identificação. Renomear a mesma Carteira atualiza identificação sem recarregar o histórico; invalidar/remover a Carteira limpa os dados e segue a resolução existente, preservando o veto ao fallback de URL explícita inválida.
4. **Fluxo de leitura cancelável.** Observar o ID apenas quando o contexto estiver ready, deduplicar e combinar com gatilho explícito de retry. Usar switchMap ou proteção equivalente com identidade da requisição. Limpar dados/erro na transição, tratar erro dentro do fluxo para manter futuras seleções funcionais e impedir finalize de consulta antiga de desligar loading da atual. Estado financeiro permanece local à página. Não realizar retry automático ou chamada global.
5. **Estados distintos.** Primeiro resolver loading/erro de contexto; após sucesso distinguir ID explícito inválido de coleção vazia, depois contexto válido com loading/vazio/conteúdo/erro de operações. Sem contexto válido, bloquear ações de cadastro. Para 404 contextual posterior à validação, informar indisponibilidade e oferecer recuperação explícita, sem trocar de Carteira silenciosamente. Reutilizar feedback/status e estilos existentes, sem reformular tabela/cards.
6. **Origem durável da listagem.** Links usam carteiraId=A e origem=operacoes. Estender operationReturnUrl com destino interno conhecido `/operacoes?carteiraId=A`; não aceitar URL arbitrária. Origem operacoes sem ID válido deve bloquear captura, como origens contextuais existentes. Cancelar/concluir restaura A pela URL, mesmo se o seletor mudou para B durante preenchimento. A captura e o payload permanecem fixos em A. No retorno após sucesso, a nova instância consulta o histórico de A; não criar cache/event bus global.
7. **Detalhe imutável por seleção.** Manter GET /operacoes/{id} e reaproveitamento de DTO lossless. Query de retorno não troca operação nem reescreve sua Carteira. Validar carteiraId da origem contra DTO quando disponível; conflito/ausência de contexto usa /operacoes. Reload recupera DTO via GET e reconstrói origem da URL, sem depender de extras.info/history.state. Não sincronizar automaticamente o seletor à Carteira do DTO durante a visualização.
8. **Compatibilidade preservada.** Origens dashboard/carteira e dialogs não mudam. Acesso legado sem origem contextual conserva retorno /operacoes; essa rota agora resolve Carteira. Não criar origem implícita ou reclassificar operações antigas. A API global continua disponível, sem opção de interface nesta entrega.

## Risks / Trade-offs

- Corrida entre URL e publicação de contexto → binding único por página, deduplicação e testes de ordem de eventos; não publicar dados para ID diferente do vigente.
- Cancelamento de A encerra loading de B → estado vinculado à requisição/contexto, testes com Subjects e HTTP controlado.
- Origem operacoes incompleta cair no fallback do formulário → incluí-la na validação contextual, mantendo bloqueio sem ID válido.
- Voltar para A após preencher com seletor em B pode surpreender → nome de A visível no formulário e retorno explícito documentado; nunca reatribuir POST.
- Perda do agregado na interface → mudança deliberada; API/service preservados para uso futuro.
- Deltas anteriores sobrescreverem os novos → aplicar/sincronizar mudanças na ordem shell, depois contexto, revisando requisitos completos e cenários; nenhuma sincronização nesta etapa.
- Tests DOM não provam foco/reflow/histórico real → complementar com validação manual direcionada após implementação.

## Migration Plan

Somente planejamento agora. Após aprovação da implementação: alterar frontend e testes mínimos, executar suíte/build, comparar bundle sem alterar budgets, validar OpenSpec, atualizar Graphify após código e revisar escopo. Preservar todas as rotas existentes, dados e endpoints; não há migração de banco. Rollback da futura implementação restaura comportamento global da página e helper anterior, mantendo API/dados. Commit, publicação e arquivamento exigem instrução posterior; não pertencem a esta preparação.

## Critérios de aceitação e evidências previstas

| Critério | Evidência |
| --- | --- |
| URL A prevalece sobre memória/preferência B; sem ID usa fallback vigente | Testes de binding/contexto com armazenamento controlado |
| Normalização substitui entrada e não duplica GET | Router + HttpTestingController |
| A → B → voltar → avançar acompanha URL e seletor | Integração e navegador real |
| Respostas/erros tardios não contaminam contexto atual | HTTP cancelado e respostas controladas A/B |
| Sete estados solicitados e loading de operações distintos | Testes de DOM/HTTP; sem chamada global |
| Cadastro captura A, troca B não altera POST, cancelar/concluir volta A | Integração de formulário/rotas, COMPRA e VENDA |
| Detalhe conserva ID/DTO após troca; reload e conflito seguros | Testes de detalhe e origem |
| Contratos legados e global service preservados | Regressão de service, Dashboard/Carteira e dialogs |
| Nome da Carteira, estados, links e foco legíveis sem segundo seletor | DOM e verificação manual mobile/desktop |

Arquivos futuros previstos: operacoes-list-page.component.ts, carteira-navigation.service.ts, operation-origin.ts, ajustes mínimos em operacao-form-page.component.ts e operacao-detail-page.component.ts, e respectivos testes; app.routes.spec.ts deve substituir o cenário global da change anterior somente na futura implementação. CarteiraContextService e OperacoesService devem ser reutilizados sem mudança de contrato.
