## MODIFIED Requirements

### Requirement: Coleções comparáveis com representação responsiva única
Coleções tabulares SHALL usar cabeçalhos associados e semântica de tabela no desktop e cards completos no mobile, com separadores discretos e ações estáveis. Apenas uma representação SHALL estar ativa para tecnologia assistiva e teclado por breakpoint. A troca de apresentação MUST NOT duplicar efeitos, requisições, dados ou anúncios nem introduzir ordenação, filtros, paginação ou buscas. Hover de linha inteira MUST NOT sugerir uma ação inexistente.

#### Scenario: Tabela no desktop
- **WHEN** a largura útil comporta a coleção tabular
- **THEN** cabeçalhos identificam células, números são comparáveis e ações têm nomes acessíveis sem tornar toda linha falsamente clicável

#### Scenario: Cards no mobile
- **WHEN** a largura útil exige apresentação compacta
- **THEN** cada card preserva todos os campos apresentados no desktop e somente a representação visível participa da leitura e foco

#### Scenario: Mudança de largura
- **WHEN** o viewport cruza o breakpoint da coleção
- **THEN** ordem, conteúdo e contexto permanecem iguais sem novas requisições ou controles funcionais

#### Scenario: Colecoes financeiras padronizadas
- **WHEN** usuario consulta Posicoes abertas, Acoes, Corretoras ou Carteiras
- **THEN** encontra superficies brancas delimitadas, cabecalho discreto, identidade principal destacada, metadados secundarios e acoes identificaveis, com numeros alinhados e estados textuais sem depender apenas de cor
- **AND** todos os campos atuais, valores autoritativos, timestamps, ordem, links e requests permanecem iguais, sem totalizar moedas ou recalcular dados
