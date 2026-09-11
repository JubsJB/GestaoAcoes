## Context

Retomada a partir de `.openspec.yaml` e `proposal.md` existentes, preservados sem reescrita. A motivação e os limites de produto estão na proposta; este documento completa as decisões de implementação futura. PRD: seção 9.3, RF13–RF26 e seções 17–18. Nenhuma regra financeira é redefinida.

Somente quatro capabilities recebem deltas: frontend-application-shell, frontend-dashboard-management, frontend-portfolio-management e frontend-operation-management. O estado inicial contém trabalho visual anterior ainda não commitado; ele não pertence a esta execução de planejamento.

## Goals / Non-Goals

**Goals:** estabelecer uma fonte compartilhada de identidade da Carteira, sincronizar navegação e mutações confirmadas, eliminar a seleção duplicada do Dashboard, apresentar posições existentes no detalhe e impedir que uma operação mude de Carteira durante o preenchimento.

**Non-Goals:** armazenar finanças no contexto, alterar backend, DTO, endpoint, banco, cálculo financeiro, parsing lossless, formatadores, dependências, budgets ou Graphify. Não retomar redesign, autenticação, FX ou sincronização live entre abas. Esta execução cria somente artefatos; não implementa, testa código, faz commit, push, merge ou archive.

## Decisions

### 1. Store leve e coordenação de navegação

Usar serviço compartilhado em escopo da aplicação com signals/computed e RxJS já disponíveis. Estado: coleção de CarteiraResponse, ID ativo, status de inicialização e erro normalizado. Identificação e nome são derivados da coleção. Não incluir posições, operações, resumos, resultados, evolução, snapshots ou cache financeiro.

O store administra estado e mutações confirmadas; um coordenador traduz intenções de seleção e eventos do Router. As páginas continuam responsáveis por suas consultas financeiras. Evitar store com Router embutido e efeitos bidirecionais dispersos nas páginas, pois favorecem loops. Comparar ID e URL antes de navegar ou emitir alteração. Compartilhar a carga inicial em andamento entre shell e Dashboard; o Dashboard não lista Carteiras por conta própria. Uma resposta antiga da coleção não pode desfazer uma mutação confirmada mais recente: invalidar a geração pendente ou reconciliar pelos eventos confirmados antes de publicá-la.

Manter OnPush e os limites lazy. Reutilizar os serviços existentes sem importar componente de página do Dashboard para o shell ou detalhe. Uma eventual extração de apresentação de posições deve ser mínima e não copiar parsing ou cálculos.

### 2. URL, memória e localStorage

Ordem: URL explícita válida > seleção atual válida em memória > preferência local validada > primeira Carteira por id ASC > vazio. Validar IDs segundo a representação segura já adotada e contra a coleção disponível; erro de GET não é ausência. O fallback ordena apenas a escolha do ID e não reordena históricos financeiros nem a listagem de Carteiras.

Usar chave isolada e versionada `gestaoacoes.carteira-ativa.v1`, contendo somente o ID da última seleção explícita. Ler defensivamente; falhas de leitura/escrita/remoção não impedem uso em memória. Uma URL válida aberta deliberadamente e a escolha no seletor são seleções explícitas; normalização automática da URL e fallback não são. Abrir detalhe válido seleciona e persiste essa Carteira. Preferência inválida é ignorada e pode ser removida, sem gravar fallback. Não ouvir `storage` para sincronização live.

URL inválida permanece um estado explícito sem consulta financeira nem substituição silenciosa por preferência. A escolha de outra Carteira no seletor ou o retorno a um destino válido recupera o fluxo. A URL só é normalizada depois de contexto validado; a normalização usa substituição da entrada do histórico, enquanto a troca deliberada permite voltar/avançar. Preservar parâmetros não relacionados. O coordenador distingue origem Router, seleção explícita e fallback para evitar persistência indevida e ciclos.

| Destino | Identidade e comportamento |
| --- | --- |
| `/dashboard` | `carteiraId` na query; ausência resolve contexto e normaliza a URL |
| `/carteiras/{id}` | ID do path é autoritativo; trocar no seletor navega para novo detalhe; query conflitante não substitui o path |
| `/carteiras/{id}/editar` | ID da entidade editada permanece fixo; troca global não reatribui o PATCH |
| Listagens, Ações e Corretoras | Seleção global permanece disponível sem filtrar implicitamente essas coleções |
| `/operacoes` | Histórico global preservado, sem filtro pela Carteira ativa |
| `/operacoes/nova` | Captura ID validado da URL ou do contexto na abertura; identidade fixa durante a instância |
| `/operacoes/{id}` | ID do path identifica a Operação; contexto de retorno não muda o recurso consultado |

### 3. Criação, edição e exclusão

Aplicar alterações à coleção somente após POST/PATCH bem-sucedido ou DELETE 204. Incorporar o DTO retornado sem GET obrigatório adicional. Criação em dialog mantém seleção válida; se não havia seleção, resolve fallback. Criação direta mantém a navegação existente ao detalhe, que seleciona a nova Carteira. PATCH atualiza o nome no seletor e nas representações locais mantendo o ID.

DELETE remove o item e qualquer preferência que o referencie. Se era a ativa, limpar sua identidade e dados visuais antigos e escolher a primeira restante por id ASC; se era a última, entrar em vazio. Preservar retorno existente a `/carteiras`. Remover Carteira não ativa não muda seleção válida. Cancelamento, 409 ou erro técnico preservam estado e feedback vigente. Não inferir elegibilidade de exclusão nem resolver conflitos financeiros no frontend.

### 4. Dashboard e evolução

O Dashboard perde seletor e carga local de Carteiras. Recebe identidade e estados globais, mantém `carteiraId` na URL e seus próprios estados financeiros. Mudança de contexto invalida imediatamente dados antigos e cancela logicamente requisições com switchMap ou geração equivalente. Respostas tardias não produzem conteúdo, erro ou sucesso no novo contexto.

Continuam os contratos de resumo, posições, resultados realizados e evolução; Atualizar dados continua sem POST. A evolução recebe o mesmo ID validado por seu contrato atual, mantém isolamento, histórico completo e snapshots exclusivamente manuais. Não há alteração observável própria em frontend-portfolio-evolution: sua seleção já depende das regras vigentes do Dashboard e sua proteção concorrente já está especificada. Portanto não criar delta para essa capability. Qualquer futura mudança própria de evolução exigirá nova avaliação de escopo antes de editar sua spec.

### 5. Detalhe da Carteira

Observar paramMap reativamente, deduplicar IDs e aceitar estado transitório somente se compatível. A abertura válida alinha contexto global. Consultar posições via DashboardService/contrato existente `GET /carteiras/{id}/posicoes`; não criar DTO, endpoint ou novo parser. Exibir campos já aprovados para posições, moedas e formatação existentes, sem resumo ou evolução adicionais.

Dados básicos, posições e histórico têm estados independentes; o erro de uma seção não oculta as demais. Carteira inexistente impede consultas dependentes. Após operação contextual 201, incorporar o DTO no histórico conforme ordenação existente e consultar posições novamente somente para a origem ainda ativa. Não derivar posições do histórico. Uma troca de paramMap invalida respostas, DTO transitório e estados do ID anterior.

### 6. Operações com identidade fixa e retorno reproduzível

Reutilizar formulário, pipeline consultivo e construtor discriminado existentes em dialog e página. Capturar `carteiraId` validado na abertura em um contexto imutável da instância. A sugestão de VENDA e o POST usam essa captura; a prévia de COMPRA mantém o contrato independente da Carteira. Nunca consultar o store para decidir a Carteira no submit. A Carteira é visível e não editável também na entrada global. Sem identidade válida, o usuário recupera o contexto e reabre o fluxo; não submeter com fallback tardio.

Para página, usar `/operacoes/nova?carteiraId={id}&origem=dashboard` ou `origem=carteira`; transportar os mesmos parâmetros ao detalhe de Operação quando aplicável. `origem` é enum interno, não URL livre. Sem origem, retorno é `/operacoes`; origem desconhecida não autoriza redirecionamento e usa retorno global. Origem contextual sem ID válido exige recuperação explícita. Em entrada global sem query, representar o ID capturado na URL por replaceUrl para que reload mantenha a mesma Carteira, sem converter a origem global em contextual.

Cancelar ou concluir a página retorna a `/dashboard?carteiraId={id}` ou `/carteiras/{id}` conforme a origem; dialog fecha no detalhe de origem. Reload de um dialog não restaura formulário não salvo: permanece a rota do detalhe. history.state pode otimizar leitura do DTO, nunca ser a única fonte de retorno. Acesso direto ao detalhe consulta o endpoint existente e sem origem retorna à lista global.

Troca do seletor durante formulário não modifica a identidade capturada nem a query de origem da instância. Se a troca no detalhe causa navegação com dialog aberto, fechar/cancelar o dialog da rota anterior; qualquer POST já enviado continua associado à origem e sua resposta não atualiza a nova página. Mudança explícita da URL de cadastro encerra a instância anterior e inicia outro contexto validado, em vez de modificar silenciosamente seus campos. Não criar retry automático nem prometer cancelamento de POST já aceito pelo backend.

### 7. Relação com a change visual pausada

`refinar-experiencia-visual-frontend` continua pausada, com os trabalhos e pendências das Fases 1–3 preservados e Fases 4–7 não iniciadas. Não alterar seus arquivos, evidências ou checkboxes. Esta change precede sua retomada, não depende de concluí-la.

Na retomada futura, reconciliar proposal.md, design.md e deltas visuais de shell, Dashboard e Carteiras com a nova base funcional. Revisar tasks 3.1–3.2 (shell), 4.2/4.8 (histórico e detalhe agora com posições), 5.4–5.7 (formulário fixo), 6.1–6.2 (seletor agora global), 6.4 (posições) e 7.13–7.16 (regressão de contratos/escopo). Não executar essa reconciliação nesta change. A task visual 6.2 não poderá restaurar seletor local do Dashboard; a apresentação visual deverá seguir o shell aprovado aqui.

## Risks / Trade-offs

- [URL e store em ciclo] → coordenador único, comparação de identidade e distinção entre seleção e normalização.
- [Respostas fora de ordem] → invalidação imediata e geração/cancelamento lógico, inclusive na coleção após mutações.
- [Formulário enviado para outra Carteira] → captura imutável desde abertura, query preservada e teste que verifica o corpo real do POST.
- [localStorage inválido ou indisponível] → validação contra coleção e continuidade em memória; nenhuma sincronização live entre abas.
- [Conflito com deltas visuais ainda pausados] → reconciliação futura explicitamente mapeada; não aplicar a change visual sobre os requisitos antigos sem revisão.
- [Specs históricas com frases de placeholders e Purpose básico] → não ampliar esta change para limpeza documental não relacionada; prevalecem capabilities funcionais já aprovadas e deltas específicos desta mudança.
- [Rótulo A/B/C/D sem legenda recuperável] → a proposta existente não registra o significado das categorias; não atribuir uma letra inventada. A classificação descritiva é alteração funcional transversal exclusivamente frontend, com quatro capabilities existentes e sem alteração de contrato backend.

## Migration Plan

Implementação futura na ordem: contexto e coordenador; shell; Dashboard; sincronização de Carteiras e posições; operações e retorno; verificação automatizada e manual. Nenhuma migração de dados. A chave versionada não altera registros do backend. Reversão futura deve ser restrita aos arquivos desta implementação, sem reset do trabalho visual preexistente; a preferência isolada pode ser ignorada pela versão anterior.

O planejamento termina com validação strict da change e global e verificações Git somente leitura. Testes, build e verificações manuais listados nas tasks pertencem exclusivamente à implementação futura.
