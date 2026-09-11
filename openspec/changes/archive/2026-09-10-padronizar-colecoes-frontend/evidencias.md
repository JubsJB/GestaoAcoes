# Evidencias - 2026-09-10

## Escopo e resultado
Posicoes, Acoes e Corretoras padronizadas com superficies brancas, bordas suaves, cabecalho e identidade hierarquizados. Valores atuais/cotacoes destacados; estados financeiros textuais e cores usam financialOutcomeLabel existente, sem formulas novas.
Carteiras incluida apenas pelo acabamento compartilhado e link de detalhes: auditoria confirmou a mesma tabela plana anterior. Fluxo/contexto intactos.
Uma tabela por colecao. Breakpoints e reflow existentes preservados. Componente compartilhado de posicoes tambem apresenta o acabamento no detalhe de carteira.
Nenhuma alteracao em services, HTTP, rotas, contratos, calculos, dados, graficos, dependencias ou budgets nesta change.

## Arquivos
Criado: frontend/src/app/shared/collection/refined-collection.scss.
Modificados: shared/portfolio-positions/portfolio-positions.component.ts, .scss e .spec.ts; features/acoes/pages/acoes-list-page.component.ts e .spec.ts; features/corretoras/pages/corretoras-list-page.component.ts e .spec.ts; features/carteiras/pages/carteiras-list-page.component.ts.
Artefatos OpenSpec: proposal, design, tasks, delta frontend-visual-experience e esta evidencia.
collection.scss original preservado; estilo novo opt-in evita alterar listagem de Operacoes.

## Validacoes
- Focados: 65/65, cinco arquivos.
- Suite frontend completa: 490/490, 68 arquivos, VITEST_MAX_WORKERS=2.
- Production: aprovado. Avisos anteriores initial 514,40kB/limite warning 500kB e evolucao 5,68kB/4kB permanecem.
- Primeiro build apontou warning de 39 bytes no CSS de posicoes; declaracoes redundantes de peso de strong foram removidas. Build final sem warning novo, sem aumentar budgets.
- Edge headless real com build production e dados simulados: 8/8 cenarios (Dashboard, Acoes, Corretoras, Carteiras em 1440x900 e 320x900); nenhum overflow global, celula cortada ou tabela duplicada. Capturas desktop inspecionadas; revisao humana nao substituida.
- Colunas preservadas: Posicoes 8, Acoes 5, Corretoras 5, Carteiras 4. Metadados de ativo/moeda/empresa/timestamp preservados.
- Requests renderizados: Dashboard quatro GETs financeiros mais GET de carteiras do shell; Acoes/Corretoras um GET de lista mais shell; Carteiras mantem dois GETs existentes (shell e lista). Nenhum POST/PATCH.
- Links de detalhes /acoes/1, /corretoras/1, /carteiras/1 preservados nos mocks.
- Harness inicial falhou por resposta simulada de resultados realizados incorreta; corrigido apenas o harness local, sem mudanca de codigo funcional.
- Scripts, screenshots e logs locais em frontend/.cache, ignorados e nao destinados ao versionamento.
- OpenSpec strict change/global e git diff --check aprovados; working tree preservado sem staging.

## Bundle/CSS antes e depois
Valores em bytes, medidos do stats.json production para chunks.
| Item | Antes | Depois |
|---|---:|---:|
| Initial (kB arredondado do build) | 514,40 | 514,40 |
| Dashboard lazy | 67378 | 67378 |
| Acoes lazy | 15495 | 18237 |
| Corretoras lazy | 14465 | 17067 |
| Carteiras lazy | 8273 | 10875 |
| Operacoes lazy | 7769 | 7769 |
| CSS compartilhado, Sass comprimido | 1854 | 3485 |
| CSS Posicoes, Sass comprimido | 2351 | 3982 |

CSS medido via Sass comprimido, antes da encapsulacao Angular. Crescimento dos chunks provem do acabamento SCSS encapsulado por componente e dos wrappers de apresentacao; nenhum componente promovido ao initial. Dashboard CSS e CSS dos graficos inalterados.

## Aceite visual humano — 2026-09-10
Revisao visual humana APROVADA explicitamente pelo usuario. Task 3.3 concluida; 6/6 tasks encerradas. Archive autorizado. Validacoes de testes e build anteriores permanecem validas, sem alteracao de codigo ou repeticao.
