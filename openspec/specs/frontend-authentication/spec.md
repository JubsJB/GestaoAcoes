# frontend-authentication Specification

## Purpose

Integrar autenticação ao Angular com navegação autorizada, retorno seguro e isolamento de dados entre identidades e sessões.

## Requirements

### Requirement: Telas e ações conforme perfil
O Angular SHALL oferecer login, redefinição e gestão ADMIN compatíveis com layout atual, proteger rotas e ocultar/desabilitar escritas para USER, inclusive cotação, snapshot, custódia e estorno. MUST NOT modernizar globalmente dashboard/navegação nesta change.

#### Scenario: USER abre formulário por URL
- **WHEN** USER tenta navegar diretamente a uma rota de escrita ou administração
- **THEN** guard nega acesso e mantém destino consultável, além de backend negar request forçado

#### Scenario: ADMIN gerencia conta
- **WHEN** ADMIN abre gestão de usuários
- **THEN** pode cadastrar, consultar, mudar perfil/situação e copiar código autorizado, sem campo para escolher senha do titular

### Requirement: Restauração por cookie e atividade real
O Angular SHALL restaurar identidade/permissões por /auth/me no carregamento/reload, mantendo conteúdo protegido bloqueado até confirmação de sessão válida. MUST NOT ler JWT, criar header Bearer ou guardar credencial em memória/Web Storage. SHALL integrar token CSRF independente nas escritas da API mesma origem. Após dez minutos sem atividade global válida ou quatro horas absolutas SHALL exigir login, revalidando sessão comum antes de decidir por timer local. Ao voltar de aba suspensa SHALL verificar /me antes de permitir interação protegida. /me e obtenção de CSRF MUST NOT registrar atividade.
#### Scenario: Timer e polling
- **WHEN** há requests automáticos mas nenhum evento humano por dez minutos
- **THEN** cliente encerra sessão e não envia atividade para prolongá-la

#### Scenario: Destino externo
- **WHEN** uma requisição é enviada a origem externa à API
- **THEN** nenhuma credencial ou token CSRF da aplicação é anexado a esse destino

#### Scenario: Reload com sessão válida
- **WHEN** página é recarregada com sessão compartilhada válida
- **THEN** /me restaura identidade/perfil e a rota autorizada sem login e sem atualizar atividade, sem recuperar automaticamente campos financeiros não salvos

#### Scenario: Reload ou aba suspensa com sessão inválida
- **WHEN** cookie está ausente/inválido/expirado ou sessão foi revogada ao recarregar ou retomar aba
- **THEN** a revalidação nega shell protegido, limpa estado e direciona para login preservando somente retorno permitido

### Requirement: Retorno autorizado por identidade
O sistema SHALL preservar apenas rota relativa com parâmetros/fragmento necessários e permitidos após interrupção para autenticação, consumi-la uma vez e revalidar identidade, perfil e referências após login. MUST NOT restaurar intenção de outra identidade, aceitar redirect externo ou repetir escrita automaticamente.

#### Scenario: Retorno à carteira com parâmetros permitidos
- **WHEN** sessão da mesma identidade expira em página consultável com carteiraId e parâmetros de navegação conhecidos e seguros, incluindo origem quando prevista, e o usuário autentica novamente
- **THEN** retorna à mesma página com os parâmetros permitidos e dados novamente consultados, sem serializar filtros locais de custódia nem restaurar campos não salvos, conforme o recorte Angular aprovado

#### Scenario: Perfil ou referência mudou
- **WHEN** retorno aponta para página agora proibida ou carteira excluída
- **THEN** usuário vai para destino autorizado com aviso, sem redirecionamento em loop

#### Scenario: ReturnUrl malicioso ou troca de conta
- **WHEN** intenção contém URL externa, path de login/reset ou usuário autenticado é diferente do anterior
- **THEN** intenção é descartada e abre dashboard autorizado e limpo

### Requirement: Dados isolados no encerramento
O sistema SHALL limpar carteira selecionada, preferência global legada, caches, formulários e dados financeiros em logout, expiração ou troca de identidade; SHALL descartar respostas tardias da sessão anterior e sinalizar encerramento entre abas sem compartilhar tokens. MUST NOT persistir dados financeiros na intenção de retorno.

#### Scenario: Resposta atrasada de conta anterior
- **WHEN** GET iniciado pela identidade A retorna depois do login da identidade B
- **THEN** não preenche estado, seleção ou tela da sessão de B

#### Scenario: Logout com seleção persistida
- **WHEN** sessão encerra e outro usuário autentica no mesmo navegador
- **THEN** não herda carteira, formulário, preferência nem intenção da sessão anterior

#### Scenario: Erros distintos
- **WHEN** API retorna 401, 403 ou 503
- **THEN** 401 exige login e limpeza, 403 ACESSO_NEGADO informa falta de permissão e 403 CSRF_INVALIDO permite obter token novo para submissão explícita posterior, ambos sem login em loop ou repetição automática de escrita, e 503 informa indisponibilidade sem liberar ações

### Requirement: Sincronização visual sem autoridade local
O Angular SHALL reagir a sinais de login/logout/atividade entre abas sem compartilhar credenciais nem dados financeiros. SHALL mascarar conteúdo protegido em aba oculta e revalidar no foco; abas visíveis SHALL revalidar sessão a cada trinta segundos como fallback, sem renovar atividade. Mudança de identidade SHALL limpar estado anterior antes de exibir nova conta. Revogação no backend SHALL ser imediata para novas autorizações após commit, independentemente da latência visual.

#### Scenario: Logout percebido nas abas
- **WHEN** uma aba confirma logout da sessão compartilhada
- **THEN** demais abas que recebem sinal mascaram/limpam dados e verificam estado; sem sinal, detectam no próximo acesso ou polling visível de até trinta segundos, sem autorização nesse intervalo

#### Scenario: Atividade compartilhada e timeout local
- **WHEN** timer de A indica inatividade local mas B manteve sessão comum ativa
- **THEN** A consulta /me e usa prazo global válido, sem revogar sessão por ausência de interação apenas em A

#### Scenario: Login de outra identidade no navegador
- **WHEN** cookie compartilhado passa a representar usuário B enquanto A tinha requests pendentes
- **THEN** abas revalidam identidade, descartam geração anterior e dados de A, sem restaurar seleção/formulário/intenção de A para B

#### Scenario: Revalidação indisponível
- **WHEN** /me falha por rede ou 503 no reload ou retorno de aba
- **THEN** conteúdo protegido permanece bloqueado com erro recuperável, sem interpretar falha como sessão autorizada ou divulgar dados de conta anterior
