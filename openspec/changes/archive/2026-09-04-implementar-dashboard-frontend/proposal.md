# Why

O destino `/dashboard` ainda é apenas estrutural, embora o backend já forneça resumo, posições abertas e resultados realizados autoritativos por carteira. A aplicação precisa transformar esse destino em uma visão financeira útil, preservando precisão decimal e separação entre BRL e USD sem reproduzir cálculos financeiros no Angular.

# What Changes

- Substituir o placeholder lazy de `/dashboard` por uma página funcional dentro do shell existente.
- Exigir contexto de carteira: selecionar automaticamente somente quando existir exatamente uma carteira; quando existirem várias, aguardar seleção explícita.
- Representar a carteira selecionada por `carteiraId` na query string, preservando contexto em reload e tratando valores ausentes, inválidos ou inexistentes.
- Consumir `GET /carteiras/{id}/resumo`, `GET /carteiras/{id}/posicoes` e `GET /carteiras/{id}/resultados-realizados` em paralelo e oferecer reload conjunto.
- Exibir cards de resumo separados por moeda, quantidade estrutural de posições abertas, posições abertas e resultados realizados por ação.
- Preservar todos os campos `BigDecimal` como texto lossless e formatá-los apenas para apresentação, sem `number`, aritmética financeira, agregação ou conversão cambial no frontend.
- Tratar explicitamente loading, ausência de carteiras, espera por seleção, respostas vazias, 404, 409, 422 e erros técnicos normalizados.
- Manter acessibilidade, responsividade e navegação para a carteira selecionada e para o cadastro contextual de operação.
- Manter fora desta change evolução patrimonial, snapshots, gráficos, atualização periódica e qualquer mudança de backend ou dependência.

# Capabilities

## New Capabilities

- `frontend-dashboard-management`: dashboard global e contextual por carteira, com dados financeiros exclusivamente autoritativos, precisão lossless, separação de moedas e estados acessíveis.

## Modified Capabilities

- `frontend-application-shell`: reconhecer Dashboard como capability funcional, substituindo somente seu placeholder e preservando rota, shell e limite lazy atuais.

# Impact

- Frontend Angular: feature `dashboard`, models financeiros, service HTTP, infraestrutura lossless compartilhada, formatadores de apresentação e testes relacionados.
- Reutilização de `CarteirasService`, navegação, componentes de feedback e padrões visuais já existentes.
- APIs consumidas sem alteração: `/carteiras`, `/carteiras/{id}/resumo`, `/carteiras/{id}/posicoes` e `/carteiras/{id}/resultados-realizados`.
- Nenhuma alteração em backend, contratos HTTP, Carteiras, Operações, Ações ou Corretoras; nenhuma dependência nova.
- Fora do escopo: `/patrimonio`, `/evolucao-patrimonial`, snapshots, gráficos, detalhe de posição, totais financeiros calculados no frontend, patrimônio global entre carteiras e conversão cambial.
