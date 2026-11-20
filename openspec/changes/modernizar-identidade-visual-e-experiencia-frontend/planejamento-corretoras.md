# 6.4 — Corretoras: análise e proposta visual

Atualização de 2026-11-12: proposta visual e alinhamento ADMIN/USER aprovados; após conferência do fluxo completo BrasilAPI/ViaCEP/opcionais/endereço/confirmação, o usuário autorizou implementar 6.4. O planejamento abaixo é preservado como histórico. Evidências da implementação serão referenciadas em tasks.md, sem concluir 6.4 antes do aceite real. Confirmação contextual e contratos backend não foram simplificados.

2026-11-12. Somente planejamento autorizado após aceite explícito de Ativos (6.3). Nenhuma alteração de código ou contrato de Corretoras; implementação aguarda avaliação visual. Não repetir testes/build de Ativos nem propagar mudanças para outras áreas. Sidebar suspensa.

## Inventário atual

| Superfície | Conteúdo e comportamento preservados |
| --- | --- |
| /corretoras | Uma tabela semântica de Instituição, CNPJ, Localidade, Situação cadastral e Ações; razão social, fantasia somente se diferente, cidade/UF, situação textual e Ver detalhes; ordem da API |
| Consulta | CNPJ exato com ou sem máscara, formato básico de 14 dígitos, somente após submissão; navega ao detalhe com DTO transitório, sem filtrar a coleção nem consultar ao digitar; Limpar não faz GET adicional |
| Cadastro /corretoras/nova e diálogo | Somente CNPJ; máscara/normalização atuais, validação e bloqueio de envio duplicado; feedback de progresso, erros e sucesso; página navega ao detalhe, diálogo devolve DTO incorporado à lista sem nova consulta |
| Ausência na consulta | Apenas 404/code=null abre diálogo com CNPJ, cancelamento e convite ao cadastro com prefill, sem request automático; erro de provider/código preenchido continua erro contextual |
| Confirmação | Somente após 409/SITUACAO_CADASTRAL_NAO_ATIVA; situação quando disponível e mensagem recebida; Cancelar não envia outra escrita, Confirmar envia segundo POST com flag explícita true |
| /corretoras/:id | Razão social/CNPJ no cabeçalho; fantasia/e-mail/telefone, endereço completo, situação cadastral, validação de mercado financeiro e data de cadastro; campos opcionais Não informado; DTO transitório compatível evita GET redundante |
| Estados | Loading, coleção vazia, validação, erro/retry, detalhe 404, falha externa, cadastro pendente e sucesso; anúncios, foco, Escape, cancelamentos e restauração existentes |

Não existem edição, exclusão ou atualização cadastral manual nesta feature. Não acrescentar ações dessas naturezas, métricas, contagens, filtros, ordenação, busca parcial ou campos manuais de nome/endereço.

## Contratos e integrações

CorretorasService usa a configuração central de API e apenas GET /corretoras, GET /corretoras/{id}, GET /corretoras/por-cnpj?cnpj=... e POST /corretoras. Primeiro cadastro envia {cnpj}; confirmarSituacaoCadastralNaoAtiva:true existe exclusivamente no segundo envio confirmado após o erro específico, sem checkbox preventivo. Normalização, máscaras e validações atuais permanecem: frontend valida formato, backend valida CNPJ e dados externos. BrasilAPI e ViaCEP permanecem exclusivamente no backend. Preservar respostas, StandardError, gatilhos, ordem, cookies, CSRF, guards e permissões. ADMIN cadastra; ADMIN/USER consultam. Não transformar situação ATIVA ou validadaMercadoFinanceiro em recomendação/credenciamento financeiro.

## Proposta visual única: continuidade C2 com consulta integrada

Cabeçalho limpo com título Corretoras e ação Cadastrar corretora para ADMIN. Consulta por CNPJ integrada ao início da coleção, esclarecendo abertura do detalhe sem filtragem. Tabela desktop com cinco colunas, maior espaço para razão social e CNPJ; fantasia secundária, localidade e situação textual com cores semânticas atuais. No mobile, razão social/fantasia e CNPJ em linhas integrais; localidade/situação em colunas quando houver espaço; ação em rodapé próprio. Quebra de nomes extensos, sem truncar informações ou duplicar tabela/cartões ativos.

Detalhe: cabeçalho com razão social/CNPJ; identificação/contato em área ampla, endereço e situação em seções abaixo, empilhadas no mobile. Preservar campos e suas distinções, especialmente situação cadastral e validação de mercado financeiro. Cadastro em página e diálogo com a mesma identidade C2, campo único CNPJ, instrução de consulta dos demais dados e ação principal azul; ações empilhadas quando necessário.

Ausência: diálogo informativo azul, sem confundir ausência local com falha externa. Confirmação não ativa: texto de atenção e situação em destaque sem usar aparência de exclusão, mantendo Cancelar/Confirmar cadastro e o comportamento atual de foco. Estados vazios/loading/erro seguem as mesmas superfícies, sem dados artificiais. A mensagem ilustrativa de confirmação não substitui a mensagem real retornada pelo backend.

Prévia da conversa: corretoras-proposta.html no diretório de visualizações desta conversa. Seletor de superfície inclui listagem/consulta, detalhe, cadastro em página/diálogo, ausência, confirmação não ativa, loading, vazio, erro e validação, com desktop/360/320px. Dados fictícios e navegação local entre exemplos; o cenário de confirmação representa somente o erro contextual, não todo cadastro. Não houve inspeção de renderização em navegador da proposta; sintaxe verificada, sem afirmar homologação geométrica de 360/320. O protótipo não é captura da aplicação.

## Ponto funcional para decisão separada

CorretoraNotFoundDialogComponent ainda oferece cadastro para USER, embora outras entradas e backend restrinjam escrita a ADMIN. É a mesma inconsistência de apresentação já corrigida em Ativos, mas o aceite de Ativos não é autorização automática para alterar Corretoras. Propor alinhamento equivalente (ADMIN mantém cadastro/prefill; USER recebe ausência e retorno, sem convite) e aguardar autorização funcional junto à visual. A prévia de ausência representa ADMIN; não confundir com aprovação da variante USER ou implementação de correção. Não modificar permissões do backend.

## Plano de implementação após aprovação

1. Registrar escolha visual e decisão funcional de USER; confirmar fronteiras públicas de teste com base na autorização recebida.
2. Aplicar estilos locais opt-in azuis, reutilizando tokens, PageHeader, formulários, coleção e diálogos existentes, sem modificar compartilhados ou outras features.
3. Preservar corpos funcionais, salvo o alinhamento USER se explicitamente autorizado; eventual comportamento novo segue um ciclo RED/GREEN por vez.
4. Reaproveitar corretoras-list-page.component.spec.ts, corretoras-list-dialog.integration.spec.ts, corretora-create-page.component.spec.ts, corretora-create-dialog.spec.ts, corretora-detail-page.component.spec.ts, testes dos dois diálogos, corretoras.service.spec.ts, corretoras.routes.spec.ts e corretora-formatters.spec.ts. CorretoraNamesService permanece intacto; testar consumidores somente se houver impacto concreto.
5. Verificar listagem/consulta/cadastro/ausência/confirmar-cancelar e campos opcionais; CNPJ com/sem máscara e erros de provider; nenhum envio automático, nenhuma repetição indevida, DTO transitório e contratos inalterados. Se aprovada a correção de USER, acrescentar somente teste de ações por perfil e fluxo de retorno/foco, reaproveitando o fluxo ADMIN/prefill.
6. Inspecionar implementação real em desktop/360/320, razão social/fantasia extensas, CNPJ e endereço completos, situação longa, labels/erros e ações; teclado/foco/Escape, baixa altura e múltiplos diálogos na confirmação contextual. Não repetir suíte global por rotina; build/budgets e regressão dos afetados conforme mudança. Zoom/leitor de tela/reduced motion e matriz final continuam rastreados em 8.1/8.2, sem declarar validação antes das evidências.

6.4 continua aberta. Nenhuma implementação ou teste executado nesta rodada de planejamento; somente atualização documental, leitura e prévia isolada. Sem commit, push, merge ou arquivamento.
