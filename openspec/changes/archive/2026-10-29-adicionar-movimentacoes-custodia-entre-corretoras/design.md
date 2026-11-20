## Context

Esta é uma proposta técnica para revisão, não autorização de implementação. Escopo funcional conforme pedido aprovado; ver proposal.md para motivação. OpenSpec1.9.0/schema spec-driven verificados. Escritas desta etapa limitadas à própria change.

### Estado verificado e pontos de integração

- `src/main/java/com/projeto/services/OperacaoService.java:cadastrar`: valida referências, ticker, operandos NUMERIC(19,6) e data por mercado; não é transacional.
- `services/OperacaoPersistenceService.java:persistir`: transação padrão, lock de carteira, confirmação de referências, MAX(ordemNoDia)+1, candidato, replay consolidado e saveAndFlush. Nenhum saldo local.
- `repositories/CarteiraRepository.java:findByIdForUpdate`: PESSIMISTIC_WRITE. CarteiraService.excluir compartilha lock e bloqueia operações/snapshots.
- `repositories/OperacaoRepository.java:listarParaDistribuicao`: EntityGraph ação/corretora e ordem data/ordem/id.
- `services/CalculadoraPosicao.java:validarQuantidade/reproduzir`: autoridade consolidada, somente COMPRA/VENDA.
- `services/CalculadoraDistribuicaoCorretora.java:calcular`: BigDecimal, grupos incluindo null, primeira ocorrência negativa, reconciliação e filtro de posições abertas.
- `services/DistribuicaoPosicaoCorretoraService.java:consultar`: readOnly/SERIALIZABLE/REQUIRES_NEW. Manter snapshot próprio de leitura; não invocar esse método na validação da escrita.
- `dto/PrimeiroDeficitResponse.java` e `mappers/DistribuicaoCorretoraMapper.java`: evidência exclusiva de operação, precisa evoluir.
- `frontend/src/app/features/carteiras/pages/carteira-detail-page.component.ts:openOperationDialog`: contexto/geração, histórico, loadPositions e distributionVersion; reutilizar padrão.
- `features/carteiras/distribuicao-corretoras.contract.ts`: fields verifica quantidade exata de propriedades; mudança aditiva também exige atualização coordenada.
- `features/operacoes/operacao-validators.ts`, `core/http/lossless-json.ts`, `shared/formatters/financial-value.formatter.ts` e `offset-date-time.formatter.ts`: reutilizar políticas de apresentação/precisão adequadas sem alterar experimentos temporais.
- `entities/Operacao.java` não possui instante de cadastro; Carteira possui OffsetDateTime. Não fabricar auditoria financeira bitemporal legada.
- Arquivos Liquibase001–007 e master verificados novamente:008 disponível. A migration não é criada nesta entrega.
- PRD RF16/RF17, README distribuição e AGENTS seção7 ainda descrevem limites anteriores. Atualização somente na etapa6 autorizada.

Inspeção inicial: branch dev; alteração preexistente em offset-date-time.formatter.spec.ts, teste JDBC não rastreado e temporalpoc/ não rastreado. Não incorporar/corrigir esses arquivos.

## Goals / Non-Goals

**Objetivos técnicos:** uma fonte quantitativa derivada; chaves imutáveis; comparação diferencial; neutralização auditável; lock por carteira; contratos explícitos que permitam testes incrementais.

**Limites:** não reimplementar financeiro por corretora, criar tabela de saldos/diagnósticos permanentes, alterar enum financeiro, sincronizar custódia externa, editar/excluir originais, autorizar execução de tasks ou atingir arquivos fora da change nesta entrega. Não garantir reconstrução "como conhecido na época" de operações financeiras sem dataRegistro.

## Decisions

### 1. Domínio separado e duas tabelas

Escolha: MovimentacaoCustodia (TRANSFERENCIA/ATRIBUICAO) e EstornoMovimentacaoCustodia. Estorno é fato de auditoria ligado à original, não uma terceira variante com preço ou evento financeiro.

Alternativas:
- Estender Operacao: rejeitada por preço/total positivos, CHECK COMPRA/VENDA, VARCHAR(10), DTO discriminado e impacto financeiro.
- Tabela única de eventos: possível, mas estorno duplicaria ação, quantidade, direção e posição; aumenta constraints condicionais.
- Duas tabelas: vínculo único simples, original imutável e situação derivada. Adotada.

| MovimentacaoCustodia | Persistência proposta |
|---|---|
| id | BIGINT identity |
| carteira_id, acao_id | BIGINT NOT NULL, FK restritiva |
| natureza | VARCHAR(20) NOT NULL, CHECK TRANSFERENCIA/ATRIBUICAO |
| corretora_origem_id | BIGINT null somente em ATRIBUICAO |
| corretora_destino_id | BIGINT NOT NULL |
| quantidade | NUMERIC(19,6) NOT NULL, >0 |
| data_efetiva | DATE NOT NULL |
| data_registro | TIMESTAMP WITH TIME ZONE NOT NULL, gerado por Clock servidor |
| ordem_financeira_ancora | INTEGER NOT NULL, >=0 |
| posicao_numerador, posicao_denominador | VARCHAR(512) NOT NULL, dígitos ASCII canônicos |

Estorno: id BIGINT identity; movimentacao_original_id BIGINT NOT NULL FK/UNIQUE; data_registro TIMESTAMP WITH TIME ZONE; motivo VARCHAR(1000) nullable. Data efetiva e posição são derivadas da original, evitando dados duplicados divergentes. Resposta expõe ambas as datas. Situação original derivada de existência do vínculo: ATIVA/ESTORNADA; não persistir segundo status mutável.

Constraints: origem/destino por natureza; destino não nulo; origem diferente quando não nula; quantidade/âncora válidas; formatos de chave; UNIQUE(carteira_id,acao_id,data_efetiva,ordem_financeira_ancora,posicao_numerador,posicao_denominador); UNIQUE do estorno; FKs sem cascade. Canonicidade matemática validada pelo domínio. Inserções fora dos serviços não são caminho suportado; histórico estrutural inválido deve falhar explicitamente.

Índices de histórico por carteira/data_registro/id, de replay por carteira/ação/data_efetiva/âncora e de FKs conforme necessidade concreta. Estorno consultado em lote; sem N+1. Não impor limite NUMERIC(19,6) a acumuladores derivados.

#### Decisão arquitetural aprovada: entidades JPA diretas (alternativa B)

MovimentacaoCustodia e EstornoMovimentacaoCustodia serão adaptadas ao padrão JPA existente em com.projeto.entities, sem duplicar modelos de domínio e entidades persistentes. Classes e campos persistentes não finais, construtor protegido sem argumentos e acesso por campos permitirão hidratação pelo Hibernate e geração de identidade. Imutabilidade significa ausência de setters públicos ou operações de negócio que alterem registros de auditoria; não impede o preenchimento interno pelo ORM. Nenhuma edição/exclusão de custódia, cascata de remoção ou orphanRemoval será introduzida. PosicaoRacionalCustodia permanecerá objeto de valor imutável, matemático e independente de JPA.

Criação e reconstrução histórica terão caminhos distintos. Criação valida estrutura, quantidade/direção e data não futura na zona do mercado, captura dataRegistro pelo Clock do servidor e exige posição interna válida. Reconstrução preserva id, dataEfetiva, dataRegistro e posição armazenados, sem consultar Clock, regenerar instantes ou reaplicar validações temporais de novo cadastro a histórico legítimo. A precisão temporal e o roundtrip serão verificados nas tarefas de persistência; leitura não truncará nem corrigirá valores silenciosamente.

Numerador e denominador serão persistidos canônicos nas duas colunas VARCHAR(512) já propostas, mantendo âncora e data efetiva separadas. A geração matemática pode reduzir operandos; a leitura histórica será estrita: textos ASCII sem sinais/zeros iniciais, até 512 dígitos por componente armazenado, denominador positivo, 0<n<d e gcd(n,d)=1. Frações persistidas não canônicas, inclusive 2/4, serão rejeitadas explicitamente em vez de normalizadas para 1/2. Fronteiras 0/1 e 1/1 serão rejeitadas na entrada do modelo persistível e na reconstrução, ainda que o tipo matemático também represente fronteiras para gerar mediantas. Nenhuma mudança da representação ou do limite512 é autorizada por essa decisão.

Hidratação acionará validação estrutural reutilizável dos campos locais, sem consultas adicionais, Clock ou alterações silenciosas. Validações que precisam de dados associados usarão referências carregadas no mesmo contexto consistente, sem consultas por linha ou carregamento implícito no callback. Invariantes de domínio serão preservadas na criação e reconstrução; CHECKs, FKs e UNIQUE serão defesa adicional, não sua única implementação. Falha estrutural interrompe a leitura, sem reparar registros.

Estorno terá vínculo obrigatório e único à original, com FK restritiva e sem duplicar posição, quantidade ou data efetiva. Esses dados permanecem na original e serão derivados dela; estorno registra apenas sua identidade, vínculo, instante próprio e motivo conforme contrato. Situação ATIVA/ESTORNADA será derivada do vínculo efetivamente persistido e carregado junto à original no contexto consistente, nunca de um status armazenado ou apenas da existência de uma candidata em memória. Identidade gerada não comprova commit. Preservar a posição da original inclusive após estorno; vínculo consultado em lote, sem exigir associação inversa em cada original.

A responsabilidade pela situação pertence ao fluxo de consulta: original e vínculos serão obtidos no mesmo contexto consistente, em lote, evitando N+1. O contrato mínimo VinculoEstornoConsultado identifica a original e distingue vínculo presente de ausência explicitamente verificada por consulta completa. Informação não carregada não produz ATIVA; contexto nulo e vínculo incompatível são rejeitados. O futuro fluxo fornece esse resultado explicitamente ao derivar a situação; não deve transformar candidata de estorno em vínculo histórico confirmado. Entidades isoladas, identidade gerada, persist/flush ou callbacks JPA não comprovam confirmação transacional. Nenhuma marca transitória de carregamento ORM condicionará a situação. Callbacks permanecem exclusivamente para validação estrutural local. O mapper apenas utiliza os resultados fornecidos pelo fluxo e não consulta banco; resposta será montada no contexto consistente com as referências necessárias carregadas. Este contrato não executa consultas, não certifica commit e não antecipa serviços, mapper HTTP ou repositórios de 1.4.

Esta decisão não altera Operacao, TipoOperacao, fórmulas ou cálculos financeiros. Também não libera integração transacional: desenvolvimento provisório de 1.3 segue as dependências incrementais, enquanto conclusão da Etapa 1 e integração da Etapa 3 continuam condicionadas às homologações PostgreSQL 17 de 1.10/1.9. O registro documental não autoriza modificar os modelos, implementar reconstrução racional estrita ou executar migrations/testes nesta entrega.

### 2. Chave exata inserível com âncora financeira

Escolha técnica proposta para revisão: âncora inteira + fração racional canônica n/d, 0<n<d, armazenada como dois textos ASCII decimais até512 dígitos cada. Java usa BigInteger e comparação cruzada; nenhum float/double, divisão arredondada ou ordem lexicográfica SQL.

| Alternativa | Avaliação |
|---|---|
| Ordem única compartilhada | Mudaria geração financeira e precisaria acomodar legado |
| Inteiro anexado ao intervalo | Não insere antes de custódia já existente |
| Decimal de escala fixa | Intervalos podem esgotar e induzir renumeração |
| Caminho/lista com predecessor | Exige invariantes de grafo/ciclos e consultas mais complexas |
| Racional canônico textual | Comparação exata, intervalo sempre divisível antes do limite físico; escolhido |

VARCHAR evita depender da precisão máxima de NUMERIC para chaves em H2/PostgreSQL. Não afirmar homologação sem os testes previstos. Números sem zeros iniciais, gcd(n,d)=1; unicidade textual corresponde à matemática no caminho validado. CHECKs de formato deverão ser implementados com SQL compatível/dialetos controlados; formato e redução também validados no domínio. SQL só carrega candidatos; Java determina ordem, independente de collation.

Chave de comparação:
- Operação financeira ordem k: (data,k,0).
- Custódia após ordem k: (data,k,1,n/d).
- Custódia antes da primeira operação: âncora0.
- ID apenas desempate técnico dentro do mesmo domínio; colisão estrutural deve falhar, não ser ocultada por ID.

Fronteiras de cada intervalo:0/1 e1/1. Entre vizinhas a/b<c/d gerar medianta (a+c)/(b+d), reduzir por gcd. Exemplos: primeira1/2; entre1/2 e2/3 gera3/5; antes de1/2 gera1/3. As chaves existentes nunca mudam. Manter originais estornadas como pontos transparentes/reservas, sem reutilizar sua chave.

Capacidade proposta: máximo de 512 dígitos em cada componente depois da redução. Há evidências preliminares favoráveis em H2 2.4.240 e PostgreSQL 16.8; a capacidade permanece pendente de homologação no PostgreSQL 17 e no modelo integrado. Essas evidências permitem desenvolvimento incremental autorizado, sem certificar outro banco ou versão. Se a primeira posição quantitativamente válida exigir uma chave que não possa ser representada exatamente dentro do limite confirmado, retornar 409 LIMITE_POSICAO_CUSTODIA com rollback. Não escolher outra posição, renumerar, reutilizar chaves reservadas, truncar ou arredondar. A falha é de representação, distinta de ausência de saldo e de conflito transacional transitório. Ajustar representação ou capacidade requer revisão explícita do planejamento antes de aplicar.

#### Riscos e critério de liberação da chave

| Aspecto | Risco a verificar em PostgreSQL e H2 | Evidência futura necessária |
|---|---|---|
| Armazenamento | VARCHAR preserva dígitos, mas largura declarada não certifica armazenamento, validação de formato, roundtrip JPA/JDBC ou custo por registro | Persistir e reler componentes curtos, máximos e inválidos, mantendo texto ASCII canônico sem transformação |
| Comparação | Ordenação textual não representa a ordem racional; conversão numérica limitada ou multiplicação de inteiros fixos pode perder precisão | Comparação cruzada BigInteger com sinais/limites validados, sem ORDER BY textual como autoridade cronológica |
| Unicidade | UNIQUE textual não reconhece equivalência como 1/2 e 2/4; zeros iniciais também criam representações distintas | Redução por gcd e formato canônico antes de persistir, unicidade composta para a forma canônica e rejeição explícita de histórico malformado |
| Indexação | Comprimento combinado dos textos, demais colunas, representação interna e implementação do índice podem afetar tamanho admitido e desempenho; sucesso em H2 não homologa PostgreSQL | Criar o índice proposto em bancos descartáveis, inserir chaves no limite, verificar unicidade e planos/custos de consultas representativas |
| Crescimento | Inserções sucessivas em espaços estreitos podem aumentar componentes; capacidade física não torna o conjunto de chaves ilimitado | Exercitar mediantas/redução e sequências adversariais, verificando erro explícito no limite e preservação de todas as chaves existentes |

Os dois componentes de até 512 dígitos integram o mesmo índice composto; não basta testar cada coluna isoladamente. Registrar versões dos bancos, definições de índice/CHECK utilizadas, dados de teste e resultados de armazenamento, comparação, unicidade e indexação. Não presumir equivalência de dialetos, collation, regex ou limites de índice entre PostgreSQL e H2.

**Liberação incremental e homologação obrigatória:** o planejamento permite, após autorização de implementação, desenvolver domínio e testes unitários de 1.1/1.2 e antecipar a matemática pura de 2.2, mantendo sua identificação na Etapa 2. Posteriormente podem avançar modelos/mapeamentos, migration, constraints e repositórios de 1.3–1.6 como desenvolvimento provisório; testes H2 exigem ambiente isolado autorizado. A tarefa 1.10 permanece aberta até repetir a verificação preliminar em PostgreSQL 17 descartável, independentemente da aplicação. A tarefa 1.9 homologa depois o schema e os mapeamentos integrados em H2 e PostgreSQL 17, sem dependência circular. A conclusão da Etapa 1 e a integração transacional da Etapa 3 exigem ambas as homologações PostgreSQL 17. Não aplicar migrations em bancos existentes antes dessa homologação, nem habilitar escritas de custódia ou alterar escritas financeiras existentes antes da liberação da Etapa 3. Inviabilidade exige revisão/aprovação da representação ou capacidade; nenhuma alternativa ou aumento do limite silencioso é autorizado. Esta revisão documental não autoriza implementação ou execução de testes.

Dia originalmente sem operação: custódia ancora0; primeira operação posteriormente recebe ordem1, permanecendo depois da custódia. Dia com operação k final: nova operação k+1 permanece depois das custódias ancoradas em k. Não há promoção automática de saldo de compra cadastrada depois para uma custódia anterior.

### 3. Busca determinística

Sob lock, ordenar histórico completo do ativo; usar originais estornadas como checkpoints sem efeito. Enumerar espaços da data por ordem, antes/depois de cada evento e entre custódias; obter estado anterior ao espaço, simular candidata e sufixo completo. Escolher primeiro espaço com:
1. saldo de origem antes >=quantidade;
2. consolidado financeiro válido;
3. déficit não maior em cada checkpoint/grupo;
4. todas as custódias ativas com origem antes >=quantidade.

Gerar a chave somente para o espaço escolhido. Não persistir tentativa rejeitada e não buscar em outra data. O relógio de registro não define prioridade efetiva.

Exemplos:
- compra A100 ordem1; venda B60 ordem2; transferência A→B60: espaço antes da compra falha, espaço após compra passa, selecionado.
- compra A100; venda A80; transferência A→B50: nenhum espaço passa.
- T1 A→B60; T2 B→C20; T3 B→D40 precisa entrar depois de T1 e pode ficar antes de T2; validar todo sufixo. Se essa for a primeira posição válida, preservar chaves de T1/T2.
- custódia em data sem operação, financiada por compra de dia anterior: mantém âncora0 após operações posteriormente cadastradas.

Primeiro protótipo pode simular cada espaço integralmente, custo O(espaços × eventos × grupos afetados), sem novo framework. Reutilizar prefixos é otimização posterior somente com equivalência demonstrada; não reduzir validação a saldo atual/mínimo.

### 4. Replay único e comparação diferencial

Motor puro quantitativo (nome de implementação a decidir) recebe dados do chamador, sem repository/provider. Eventos normalizados:
- financeiro: domínio FINANCEIRO, identidade(id), data/ordem, uma variação por grupo;
- custódia ativa: domínio CUSTODIA, identidade(id), data/âncora/fração, duas variações indivisíveis;
- original estornada: checkpoint identificado, efeito zero;
- estorno: somente auditoria/vínculo de neutralização, nunca variação na dataRegistro.

Calcular antes/depois sobre mesmo snapshot, alinhar união dos pontos por posição e identidade discriminada, não por índices. Candidato usa identidade temporária até flush. Em ponto ausente, transportar saldo do replay; em ponto neutralizado, aplicar efeito zero. Avaliar estado após evento indivisível para todos os grupos relevantes: D=max(0,-saldo), exigir Ddepois<=Dantes. Nunca compensar déficit de B com saldo de A. Considerar checkpoints após regularização para detectar prolongamento.

Validações locais separadas: nova venda precisa saldo do grupo antes; nova custódia idem. Não exigir isso de todas as vendas antigas. Validar todas as custódias ativas que permanecem no replay. Aplicar às operações mesmo sem custódia registrada.

Precedência de conflitos: estrutura e campos permitidos; referências existentes e contexto da carteira; duplicidade de estorno (quando aplicável, após lock); consolidado financeiro; saldo local da candidata; custódia ativa invalidada; agravamento diferencial. Duplicidade é precondição do estorno, anterior à simulação de uma nova neutralização. Dentro de cada classe quantitativa, selecionar primeiro ponto conflitante e grupos em ordem determinística. Essa prioridade entre classes não é substituída pelo evento de menor índice. Para busca de custódia: saldo local insuficiente elimina o espaço sem simular sua inserção; se origem insuficiente em todos os espaços, SALDO_ORIGEM_INSUFICIENTE. Se existe origem suficiente mas nenhum sufixo válido, POSICAO_CUSTODIA_INDISPONIVEL com motivo/causa do primeiro espaço com saldo. Não emitir uma transferência inválida para "testar" no banco.

#### Exemplo completo de checkpoints: inserção aceita com déficit legado inalterado

Mesma carteira/ação BRASIL; A, B e C são corretoras cadastradas. Todos os eventos abaixo têm data efetiva 2026-10-01. Histórico anterior: F101 COMPRA A100 ordem1; F102 COMPRA B10 ordem2; F103 VENDA legada B20 ordem3; F104 COMPRA B10 ordem4. O consolidado é sempre válido; B tem déficit legado de 10 após F103 e regulariza em F104.

Inserir candidata M*, TRANSFERENCIA A→C5, após F101 e antes de F102. Posição da candidata: âncora1, chave1/2. Identidade temporária (CUSTODIA,M*) é usada nos dois cenários até a persistência; não se inventa ID financeiro. As posições de F101–F104 permanecem iguais.

Antes: [F101, F102, F103, F104]. Depois: [F101, M*, F102, F103, F104]. União cronológica dos checkpoints: [F101, M*, F102, F103, F104]. Nos tuples abaixo a ordem é (A,B,C); déficits são max(0,-saldo) por corretora, sem compensação entre grupos.

| Checkpoint correspondente | Posição estável | Saldos antes | Saldos depois | Déficits antes | Déficits depois | Comparação |
|---|---|---|---|---|---|---|
| (FINANCEIRO,101) | ordem1 | (100,0,0) | (100,0,0) | (0,0,0) | (0,0,0) | Iguais |
| (CUSTODIA,M*) | âncora1,1/2 | (100,0,0), transportado de F101: M* não existe antes | (95,0,5) | (0,0,0) | (0,0,0) | Novo checkpoint sem déficit |
| (FINANCEIRO,102) | ordem2 | (100,10,0) | (95,10,5) | (0,0,0) | (0,0,0) | Iguais |
| (FINANCEIRO,103) | ordem3 | (100,-10,0) | (95,-10,5) | (0,10,0) | (0,10,0) | Déficit legado de B inalterado |
| (FINANCEIRO,104) | ordem4 | (100,0,0) | (95,0,5) | (0,0,0) | (0,0,0) | Regularização no mesmo checkpoint |

Resultado esperado: aceitar esse espaço, pois origem A tem100 antes da candidata, todos os déficits depois são <= aos anteriores e não há custódia ativa invalidada. É o primeiro espaço válido: antes de F101 a origem tem0. A soma consolidada final permanece100; o aviso de B permanece no replay, pois F103 ainda produz déficit.

Comparar índices seria incorreto: o índice2 anterior é F102 e o índice2 posterior é M*; o índice3 anterior é F103 e o índice3 posterior é F102. Isso pode ocultar o déficit ou atribuí-lo ao evento errado. A correspondência é por (domínio,identidade,posição), usando saldo transportado no ponto exclusivo da candidata. Grupos que não apareçam em um cenário começam com saldo0, não são omitidos da comparação.

#### Contraexemplo: saldos finais não negativos não bastam

Histórico anterior: F201 COMPRA A100 em D; F202 COMPRA B100 em D+1; F203 VENDA A90 em D+2; T204 TRANSFERENCIA B→A20 em D+3. Inserir M* A→B20 na data D, após F201. Todos os pontos anteriores são preservados; a única posição com saldo na data D fica após F201.

| União de checkpoints | Saldos antes (A,B) | Saldos depois (A,B) | Déficits antes | Déficits depois |
|---|---|---|---|---|
| F201 | (100,0) | (100,0) | (0,0) | (0,0) |
| M* | (100,0), transportado | (80,20) | (0,0) | (0,0) |
| F202 | (100,100) | (80,120) | (0,0) | (0,0) |
| F203 | (10,100) | (-10,120) | (0,0) | (10,0) |
| T204 | (30,80) | (10,100) | (0,0) | (0,0) |

Resultado esperado: rejeitar o espaço por novo déficit em A no checkpoint F203 (10>0). Saldos finais são positivos nos dois cenários e consolidado final é110 em ambos; olhar só o final aceitaria indevidamente. Como nenhum outro espaço da data D é válido, o cadastro retorna409 POSICAO_CUSTODIA_INDISPONIVEL com causa CONFLITO_HISTORICO_QUANTITATIVO identificando F203. Não mover a candidata para D+3. Da mesma forma, se um checkpoint antes regularizado passar a ter saldo negativo, a desigualdade detecta prolongamento mesmo que o pior déficit global seja igual.

#### Exemplos concretos de precedência dos erros

| Erros simultaneamente possíveis | Erro que prevalece | HTTP/code | Justificativa |
|---|---|---|---|
| Request TRANSFERENCIA com quantidade0 e destino inexistente | Estrutura/quantidade inválida | 400 REQUEST_INVALIDO | Request inválido é rejeitado antes de resolver referências; não atribuir404 a corpo não admissível |
| Nova VENDA B20 com consolidado10 e saldo B0 | Consolidado insuficiente | 409 POSICAO_INSUFICIENTE | Preservar prioridade e contrato financeiro existentes antes do saldo local |
| Custódia A→B60 com origem A50 em todos os espaços; sua inserção hipotética também invalidaria T2 | Origem insuficiente | 409 SALDO_ORIGEM_INSUFICIENTE | Nenhum espaço passa a pré-condição; não simular custódia sem saldo para escolher erro posterior |
| Alguns espaços não têm saldo, mas um tem saldo e invalida T2; nenhum espaço é válido | Ausência de posição válida, com causa da tentativa que passou saldo local | 409 POSICAO_CUSTODIA_INDISPONIVEL; causa MOVIMENTACAO_CUSTODIA_INVALIDADA | Não retornar erro do primeiro espaço sem saldo quando outro satisfaz a origem; busca examina todos os espaços |
| Compra A100; T1 A→B80 ativa; candidata VENDA A30 anterior a T1: venda local passa, T1 fica sem80 e o replay produziria A-10 | Custódia ativa invalidada | 409 MOVIMENTACAO_CUSTODIA_INVALIDADA | Proteção de T1 tem prioridade sobre o déficit que a execução inválida produziria; manter T1 sem alterar dados |
| Original já estornada e nova tentativa de estorno produziria conflitos se fosse neutralizada novamente | Duplicidade de estorno | 409 ESTORNO_DUPLICADO | Após estrutura/referência e lock, não há nova neutralização admissível para simular; constraint confirma a mesma regra |
| Request de estorno duplicado inclui quantidade proibida | Estrutura inválida | 400 REQUEST_INVALIDO | Duplicidade não sobrepõe validação do contrato de entrada |
| ID original não pertence à carteira solicitada, ainda que exista estorno em outra carteira | Referência/contexto inexistente | 404, code=null | Não consultar nem expor situação de outro contexto; preservar padrão404 existente |

Nas tentativas de custódia, erros do sufixo aparecem como causa quando o erro externo é POSICAO_CUSTODIA_INDISPONIVEL; operação financeira e estorno não têm busca de espaço e usam diretamente o código quantitativo prevalente. O exemplo de venda com T1 contém consolidado sempre não negativo (100-30=70), portanto a prioridade de custódia não contraria POSICAO_INSUFICIENTE. Nenhum código novo é introduzido por estes exemplos.

Diagnósticos recalculados: primeira ocorrência negativa do replay efetivo, preservada após compra posterior regularizadora no mesmo replay. Se compra/entrada retroativa válida elimina ocorrência, aviso desaparece. Auditoria permanece; não há armazenamento permanente de flags. Legado estruturalmente inválido/consolidado negativo continua erro, não licença para novos déficits.

### 5. Estorno e transações

Estorno remove efeito quantitativo original desde sua posição e compara antes/depois. T1 A→B40 com venda B30 ou T2 B→C30 dependente não pode ser neutralizada sem cobertura independente. Estorno válido não exige saldo no destino "hoje", pois não é transferência inversa; exige validade do histórico resultante.

Todas as escritas quantitativas reutilizam carteira.findByIdForUpdate em transação de escrita. OperacaoPersistenceService recebe validação adicional; novos serviços de custódia/estorno usam o mesmo núcleo. Adquirir carteira primeiro, confirmar referências, carregar fontes, simular, salvar/flush e confirmar. Propagação REQUIRED; nunca invocar consultar público REQUIRES_NEW para candidata. Não herdar contexto readOnly/isolation inadequado de chamador; entradas de escrita devem ter fronteira transacional apropriada.

Leitura da distribuição mantém SERIALIZABLE/readOnly/REQUIRES_NEW e carrega operações, originais e estornos no mesmo snapshot, nomes por fetch/batch. Histórico/detalhe de custódia também usarão snapshot próprio consistente; resposta montada dentro da transação para evitar lazy loading fora dela.

Lock de carteira coordena venda/transferência/estorno/exclusão sem lock global. UNIQUE estorno protege duplicidade; verificar no serviço após lock, traduzir constraint conhecida com ConstraintNameExtractor. Qualquer erro causa rollback; não continuar após flush inválido. Conflitos reconhecidos de lock/deadlock/serialização produzem409 transitório. Sem retry automático ou idempotency key no MVP.

### 6. Contratos HTTP fechados para revisão

Não criar contrato-http.md extra: definição central nesta seção e requisitos nas specs.

| Rota | Request | Resposta |
|---|---|---|
| POST /carteiras/{c}/movimentacoes-custodia | Cadastro discriminado | 201 original e Location individual |
| GET /carteiras/{c}/movimentacoes-custodia | natureza/situacao opcionais | 200 array unificado |
| GET /carteiras/{c}/movimentacoes-custodia/{m} | sem body | 200 {movimentacao,estorno}, estorno null se ausente |
| POST /carteiras/{c}/movimentacoes-custodia/{m}/estornos | sem body, {}, ou {motivo:string/null} | 201 {movimentacao,estorno}, Location do detalhe original |

Sem endpoint individual extra de estorno: detalhe contém registro criado e seu ID. Identidade do detalhe é sempre ID de original, não ID da linha ESTORNO. Nenhuma paginação no MVP, acompanhando listagens atuais; sem limite silencioso ou parâmetros não documentados.

#### Completude e desempenho do histórico sem paginação

GET de histórico deve retornar o conjunto completo abrangido pelos filtros, no snapshot da resposta, com ordenação determinística dataRegistro DESC, tipoRegistro MOVIMENTACAO antes de ESTORNO e id DESC. Não aplicar LIMIT/TOP oculto, corte no serviço/serializador/frontend ou máximo arbitrário de registros. Se ocorrer falha técnica, responder erro seguro; nunca entregar subconjunto como histórico completo. Paginação será avaliada em evolução futura e não será implementada nesta change.

Os testes futuros devem usar históricos representativos com originais ativas/estornadas, linhas de estorno, datas de registro empatadas e filtros combinados. Conferir contagem, conjunto de identidades e ordem completos, além de tempo de resposta, volume de payload, uso de memória quando mensurável e contagem de consultas. Volumes de teste não são limites de produto nem autorização para truncar. Registrar metodologia e evidências; problemas de desempenho devem gerar investigação/revisão futura, não paginação ou restrição silenciosa. Testes apenas em ambiente isolado após autorização, não nesta revisão.

Cadastro aceita exatamente {natureza,ticker,mercado,corretoraOrigemId?,corretoraDestinoId,quantidade,dataEfetiva}. Quantidade aceita número JSON exato ou string decimal conforme binding BigDecimal vigente; Angular envia string normalizada, respostas usam número JSON preservado lossless. IDs positivos. TRANSFERENCIA exige origem; ATRIBUICAO aceita omitida/null. Campos desconhecidos/controlados proibidos. Motivo estorno máximo1000 caracteres; branco normaliza para null e demais textos preservam conteúdo validado.

Linha unificada: todos os campos sempre presentes:
{id,tipoRegistro,natureza,situacao,carteiraId,acaoId,ticker,mercado,corretoraOrigemId,corretoraDestinoId,quantidade,dataEfetiva,dataRegistro,posicaoCronologica,movimentacaoOriginalId,estornoId,motivo}.
- original: tipoRegistro=MOVIMENTACAO, natureza TRANSFERENCIA/ATRIBUICAO, situação ATIVA/ESTORNADA, movimentacaoOriginalId=null, estornoId do vínculo ou null, motivo=null;
- estorno: tipoRegistro=ESTORNO, natureza ESTORNO, situacao=null, id próprio, movimentacaoOriginalId original, estornoId=id, motivo informado/null; demais campos vêm da original neutralizada.
- origem null identifica grupo sem corretora, destino sempre ID cadastrado.
- posição de custódia={ordemFinanceiraAncora,numerador,denominador}, n/d textuais; dataRegistro ISO8601 com offset, dataEfetiva civil.
- histórico usa (tipoRegistro,id) como identidade; números de domínios diferentes podem coincidir.

Ordenação: dataRegistro DESC, tipoRegistro MOVIMENTACAO antes de ESTORNO, id DESC, sem collation. Originais antecedem estornos no mesmo instante exato por desempate técnico; não afirma horário real entre registros. Filtros: natureza restringe natureza; situacao restringe originais. ESTORNO explícito retorna estornos mesmo com situacao; sem natureza e com situação retorna apenas originais; sem situação inclui estornos quando natureza os admite. Não inventar dataRegistro financeira legada.

Erros:
| HTTP/code | Aplicação |
|---|---|
| 400 REQUEST_INVALIDO | Estrutura, campos, precisão, filtros, direção e motivo |
| 400 DATA_EFETIVA_FUTURA | Somente data futura de custódia; operação mantém REQUEST_INVALIDO atual |
| 404 code=null | Referência ausente/fora da carteira; padrão ObjectNotFoundException |
| 409 POSICAO_INSUFICIENTE | Consolidado financeiro; contrato preservado |
| 409 SALDO_CORRETORA_INSUFICIENTE | Nova venda local |
| 409 SALDO_ORIGEM_INSUFICIENTE | Custódia sem saldo em todos espaços |
| 409 POSICAO_CUSTODIA_INDISPONIVEL | Origem suficiente em algum espaço, mas nenhuma inserção válida |
| 409 CONFLITO_HISTORICO_QUANTITATIVO | Ddepois>Dantes |
| 409 MOVIMENTACAO_CUSTODIA_INVALIDADA | Origem de custódia ativa fica insuficiente |
| 409 ESTORNO_DUPLICADO | Serviço/constraint de vínculo |
| 409 LIMITE_POSICAO_CUSTODIA | Capacidade da chave da primeira posição válida |
| 409 CONFLITO_TRANSACIONAL_TRANSITORIO | Lock/serialização/deadlock reconhecido |
| 409 CARTEIRA_POSSUI_MOVIMENTACOES_CUSTODIA | Exclusão protegida depois dos bloqueios antigos |
| 409 INTEGRIDADE_DADOS_VIOLADA | Fallback existente, sem classificação inventada |

StandardError vigente preservado. Details de novos conflitos: carteiraId, acaoId, origem={corretoraId,categoria} (categoria CORRETORA/SEM_CORRETORA_INFORMADA), quantidadeSolicitada/quantidadeDisponivel quando aplicáveis, evento={dominio,id,natureza,dataEfetiva,posicaoCronologica} ou null para candidata não persistida, saldoAntes/saldoDepois e deficitAntes/deficitDepois quando disponíveis, motivo estável. Identificar também grupo conflitante quando diferente da origem. Não introduzir null em Map.of; usar DTO/map adequado. Não expor nomes internos de constraint/SQL/stacktrace. Lossless também para details.

### 7. Evolução coordenada da distribuição

Manter endpoint, campos do pai/grupos, ordenações de apresentação, arrays, regras de abertas e reconciliação. Substituir primeiroDeficit antigo pelo objeto:
{dominio,id,natureza,dataEfetiva,posicaoCronologica,saldoAposEvento}.
- FINANCEIRO: natureza COMPRA/VENDA e posição {ordemNoDia}.
- CUSTODIA: natureza TRANSFERENCIA/ATRIBUICAO e posição {ordemFinanceiraAncora,numerador,denominador}.
- saldoAposEvento número JSON decimal exato; null iff sem déficit.
- ESTORNO não é origem de variação; eventual novo negativo após neutralização identifica evento efetivo e o request falha se criar/agravar déficit.

Exemplo de evidência financeira:
```json
{"dominio":"FINANCEIRO","id":12,"natureza":"VENDA","dataEfetiva":"2026-10-01","posicaoCronologica":{"ordemNoDia":2},"saldoAposEvento":-10.000000}
```
Exemplo estrutural de evidência de custódia para teste do contrato, não de escrita válida:
```json
{"dominio":"CUSTODIA","id":12,"natureza":"TRANSFERENCIA","dataEfetiva":"2026-10-01","posicaoCronologica":{"ordemFinanceiraAncora":1,"numerador":"1","denominador":"2"},"saldoAposEvento":-10.000000}
```
Um registro ativo válido não produz novo negativo; caso estrutural genérico pode ser testado isoladamente sem permitir escrita inválida. Legado quantitativo financeiro continua consultável.

Modificar DTOs, mapper, exemplos, OpenAPI, modelos/guard/parser Angular e fixtures conjuntamente. IDs devem ser seguros conforme política atual do parser; n/d nunca Number. Não preservar campos operacaoId falsos para custódia nem deixar parser antigo receber schema novo. Não criar v2. Deploy coordenado deve evitar janela com cliente antigo usando backend novo.

### 8. Frontend e proteção de auditoria

Dialog de movimentação próprio no detalhe; carteira fixa, ação existente, origem por natureza, quantidade/data. Saldo atual do painel é auxiliar e não garantia da data retroativa. Não adicionar GET de prévia temporal obrigatório nem cálculos no navegador. Formulário financeiro mantém tipos e consulta de preço; amplia mensagens de conflitos em todas as entradas (Carteira, Dashboard, página contextual).

Histórico próprio com colunas/vínculos, filtros e estados independentes. Estorno exige confirmação explícita que explica neutralização; motivo opcional. Cancelamento não escreve. Recarregar histórico/distribuição por geração no contexto capturado. POST201 seguido de GET falho deve informar sucesso+falha de leitura; retry só GET.

CarteiraService.excluir mantém precedência operações→snapshots→custódia; adiciona proteção de originais/estornos, mesmo estornados. FKs restritivas asseguram integridade. Não adicionar exclusão física de custódia.

## Risks / Trade-offs

- [Legado rejeitado indevidamente] → comparar antes/depois, validar local só candidata financeira e custódias ativas.
- [Chave cresce/tamanho de índice] → formato limitado, erro explícito sem renumeração e testes adversariais/PG isolado; nenhuma certificação implícita por H2.
- [Busca cara em histórico grande] → replay puro e testes de contagem; otimização equivalente depois de evidência.
- [Contratos rígidos incompatíveis durante deploy] → entrega coordenada com parser/schema; cliente antigo não é compatível com novo primeiroDeficit.
- [Leitura de duas fontes em instantes diferentes] → mesma transação de snapshot, não comparar GETs independentes.
- [Estorno interpretado como inversão] → efeito zero original e auditoria separada.
- [Deadlock/rollback inadequado] → lock carteira primeiro, sem rede, erro transitório sem retry e flush atômico.
- [DateTime perde precisão/offset no roundtrip] → comparar DTO ao valor persistido e testar Clock/serialização; não usar dataRegistro para cronologia efetiva.
- [Fixtures financeiras negativas criadas via POST novo] → preparar legado explicitamente em testes isolados, sem afrouxar produção.
- [Rollback de aplicação antiga aceita vendas sem custódia] → não reativar escritas antigas sobre histórico novo; manter esquema/auditoria e bloquear escritas até versão compatível.
- [Documentação vigente conflitante] → harmonizar apenas em etapa6 autorizada, preservar migrations e archives antigos.

## Migration Plan

Nenhuma migration executada/criada agora. Depois de autorização:
1. Reconfirmar prefixo008 livre; adicionar schema de duas tabelas, constraints/índices sem mexer001–007 e sem backfill de Operacao.
2. Testar instalação limpa e upgrade preservando dados em H2 descartável autorizado. Antes da integração transacional da Etapa 3, concluir 1.10 em PostgreSQL 17 descartável e homologar Liquibase/Hibernate, constraints e índice integrado nesse banco em 1.9. Não aplicar migrations em bancos existentes antes dessa homologação; sua execução posterior também depende de autorização.
3. Implementar motor/serviços protegidos e contratos; entrega completa deve ativar nova validação de vendas também sem custódias.
4. Atualizar frontend e backend coordenadamente. Validar manualmente originais, estornos, filtros e mensagens.
5. Rollback operacional preserva tabelas/registros; não executar drop com auditoria. Suspender escritas se houver retorno a backend incompatível; reversão destrutiva precisa plano explícito futuro.

## Revisão antes da implementação e verificações adiáveis

Funcionalmente fechados: primeiro espaço válido, comparação ponto a ponto, aviso efetivo recalculado, estorno opcional, data por mercado, endpoint único da distribuição, histórico ordenado por registro e filtros conforme pedido.

Estratégia racional mantida; limite512 apoiado por evidências preliminares em H2 2.4.240 e PostgreSQL 16.8, permitindo desenvolvimento incremental autorizado, mas ainda sujeito à homologação obrigatória PostgreSQL 17 da tarefa1.10 e integrada da tarefa1.9. Histórico sem paginação é decisão funcional aprovada, com completude obrigatória e eventual evolução futura. Demais detalhes da proposta permanecem para revisão: duas tabelas, motivo1000 caracteres, ausência de endpoint individual extra de estorno e Location do detalhe original. Exemplos de precedência apenas esclarecem os códigos existentes nesta change.

Verificação preliminar independente da aplicação: armazenamento, comparação, canonicidade/unicidade, formato, índice composto e crescimento adversarial em H2/PostgreSQL isolados (1.10), com evidências por versão e PostgreSQL 17 obrigatório para conclusão. Homologação integrada posterior (1.9 e etapas seguintes): mapeamentos/constraints, isolamento após espera pelo lock, precisão temporal, custo da busca e histórico representativo sem paginação. Desenvolvimento puro e integração H2 podem anteceder a homologação PostgreSQL 17 conforme os critérios de liberação incremental; a integração transacional da Etapa 3 e a conclusão da Etapa 1 não podem. Essas tarefas não autorizam banco local, execução agora ou mudança silenciosa dos contratos. Ajustes que mudem abordagem/capacidade exigem revisão/aprovação do planejamento.

## Rastreabilidade dos critérios mínimos

| Critérios do pedido | Cobertura |
|---|---|
| 1–7: transferência/atribuição, saldo e direções | custody-movement-registration: Cadastro discriminado, Saldo local, Quantidade exata |
| 8–11: precisão, futuro, retroatividade/agravamento | cadastro: Quantidade exata e Saldo local |
| 12–15: primeira posição, ausência, intervalos e estabilidade | cadastro: Primeira posição válida e estabilidade cronológica |
| 16–19: vendas por grupo e legado | operation-registration: VENDA limitada e Inserção retroativa; cadastro: comparação |
| 20–24: estorno, duplicidade/dependências/cadeia | custody-movement-reversal; cadastro: Cadeia de atribuições |
| 25–27: concorrência/rollback | cadastro/estorno: Atomicidade/Unicidade; tasks3/6 |
| 28: invariância financeira | cadastro/estorno e distribuição: Preservação |
| 29: auditoria | custody-movement-query: Histórico unificado e detalhe |
| 30–31: evidências/parser | distribuição: Diagnósticos e Schema; frontend: distribuição; tasks4/5 |
| 32: recarga após sucesso | frontend-portfolio-management: Sucesso de escrita separado |
| Extras: dia sem operações, prolongamento, limite, encerradas, isolamento, exclusão | cenários específicos nos respectivos deltas e tasks |
