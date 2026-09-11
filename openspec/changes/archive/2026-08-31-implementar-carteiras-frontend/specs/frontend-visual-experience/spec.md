## MODIFIED Requirements

### Requirement: Feedback contextual compartilhado
A aplicação SHALL oferecer feedback contextual nas variantes `success`, `info`, `warning` e `error`, com texto, detalhes opcionais e semântica assistiva compatível com o propósito. Feedback MUST NOT depender somente de cor nem criar outro formato de erro; mensagens e detalhes de `StandardError` SHALL permanecer disponíveis quando aplicáveis. Sucesso transitório de operação SHALL ser apresentado como toast acessível próximo ao topo da área visível, sem cobrir toolbar ou conteúdo essencial, enquanto erros técnicos ou externos SHALL permanecer no feedback contextual da página.

#### Scenario: Feedback urgente
- **WHEN** um erro ou warning exige atenção imediata
- **THEN** o feedback usa semântica de alerta e é apresentado em posição de destaque sem remover validações locais dos campos

#### Scenario: Feedback informativo
- **WHEN** uma informação ou sucesso contextual é anunciado sem urgência
- **THEN** o feedback usa região de status adequada e não interrompe desnecessariamente tecnologia assistiva

#### Scenario: StandardError preservado
- **WHEN** uma feature fornece `message` e `details` de erro padronizado
- **THEN** o feedback os apresenta sem substituir, ocultar ou reinterpretar seus dados técnicos

#### Scenario: Toast de sucesso transitório
- **WHEN** uma operação conclui com sucesso transitório
- **THEN** a infraestrutura Material existente apresenta toast curto com semântica de sucesso, descarte automático em oito segundos (`8000 ms`) e posicionamento superior responsivo, sem se tornar a única comunicação de informação persistente relevante
