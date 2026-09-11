## Why
Colecoes restantes ainda apresentam tabelas planas, sem a hierarquia e delimitacao dos paineis aprovados do Dashboard. Padronizar a leitura desktop preservando densidade e dados.

## What Changes
- Posicoes: ticker/empresa hierarquizados, moeda/mercado, numeros alinhados e estados textuais coerentes.
- Corretoras e Acoes: superficies delimitadas, cabecalhos, badges e links de detalhes alinhados.
- Carteiras: incluir apenas o mesmo acabamento compartilhado, pois usa a mesma tabela plana sem delimitacao; manter fluxo/contexto integral.
- Preservar uma unica tabela e reflow existente, sem novas consultas ou regras.

## Capabilities
### Modified Capabilities
- `frontend-visual-experience`: acabamento consistente das colecoes de posicoes, corretoras, acoes e carteiras.

## Impact
Templates e SCSS de colecoes frontend, componente compartilhado de posicoes e testes. Sem backend, endpoints, formatadores financeiros novos, dependencias ou budgets. Reutilizacao do componente de posicoes tambem reflete o acabamento no detalhe de carteira.
