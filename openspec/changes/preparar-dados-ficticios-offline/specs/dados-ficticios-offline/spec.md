## ADDED Requirements

### Requirement: Catálogo offline exclusivo
A fonte SHALL entregar somente registros sintéticos conhecidos, executar somente no perfil explícito da apresentação local e impedir o transporte para os provedores reais.

#### Scenario: Identificador fora do catálogo
- **WHEN** o cadastro solicita identificador desconhecido
- **THEN** a consulta falha localmente antes de persistir

### Requirement: Carga verificável
A preparação SHALL confirmar identidade, perfil e catálogo, consultar o estado inicial, usar APIs autenticadas com CSRF e registrar pontos de retomada sem segredos.

#### Scenario: Escrita ambígua
- **WHEN** uma etapa tem resultado desconhecido
- **THEN** a execução para, preserva o diagnóstico e impede repetição automática

#### Scenario: Retorno ao offline normal
- **WHEN** o operador restaura o backend HTTP offline original
- **THEN** a fonte é desativada e os dados da apresentação seguem disponíveis pelas consultas existentes

#### Scenario: Identidades do cenário acadêmico
- **WHEN** o pré-voo confere o cenário de 24/11/2026
- **THEN** exige a conta julia com papel USER quando existente e verifica Carteira de Crescimento e Carteira Conservadora sem adotar registros divergentes
