# Product Requirements Document (PRD)

**Produto:** Sistema de Gestão e Controle de Carteira de Investimentos  
**Versão:** 1.1  
**Status:** Planejamento inicial  
**Escopo inicial:** Controle de carteira de ações brasileiras e americanas

---

## 1. Visão Geral do Produto

O projeto consiste no desenvolvimento de uma aplicação web para gerenciamento e acompanhamento de uma carteira de investimentos.

A primeira versão será focada exclusivamente no controle de **ações brasileiras e americanas**, permitindo ao usuário cadastrar ativos, registrar operações de compra e venda, associar opcionalmente operações a corretoras e acompanhar a posição consolidada de cada investimento.

O sistema deverá calcular automaticamente informações como:

- quantidade atual;
- preço médio;
- custo da posição;
- cotação mais recente;
- valor atual da posição;
- lucro ou prejuízo realizado;
- lucro ou prejuízo não realizado;
- rentabilidade;
- patrimônio total;
- evolução patrimonial.

A aplicação deverá utilizar integrações externas para obtenção e validação de informações sobre ações, empresas, corretoras e endereços.

Embora o MVP seja focado em ações, a arquitetura deverá permitir futuras expansões para outros tipos de investimentos.

---

## 2. Problema Identificado

Atualmente, muitas pessoas possuem dificuldade em realizar o monitoramento dos próprios investimentos.

Esse acompanhamento pode ser considerado complexo devido à necessidade de manter planilhas, realizar cálculos manualmente e comparar informações provenientes de diferentes fontes.

Além disso, alguns investidores deixam de realizar esse acompanhamento por falta de tempo, conhecimento ou organização.

A ausência de monitoramento da carteira dificulta a compreensão da evolução dos investimentos e do patrimônio.

Além da disciplina de poupar, analisar ativos e investir regularmente, é importante acompanhar a carteira para identificar:

- quais ativos estão presentes;
- quantidade mantida de cada ativo;
- quanto foi investido;
- preço médio;
- valor atual;
- lucro ou prejuízo;
- resultados já realizados;
- rentabilidade;
- evolução do patrimônio.

Algumas plataformas existentes apresentam uma grande quantidade de dados e indicadores simultaneamente, podendo tornar a experiência complexa para usuários que procuram principalmente uma maneira simples de acompanhar seus investimentos.

O projeto busca proporcionar uma experiência centralizada, clara e de fácil compreensão.

---

## 3. Objetivo Geral

Desenvolver uma aplicação web com backend baseado em API REST utilizando Java e Spring Boot e frontend desenvolvido em Angular para gerenciamento e acompanhamento de uma carteira de ações.

A aplicação deverá permitir o registro de operações e utilizar informações provenientes de serviços externos para fornecer uma visão consolidada da carteira.

---

## 4. Objetivos Específicos

- Desenvolver uma aplicação em camadas utilizando Java e Spring Boot.
- Desenvolver uma interface web utilizando Angular.
- Criar uma interface responsiva, limpa e de fácil compreensão.
- Consumir APIs externas utilizando clientes HTTP.
- Validar dados utilizando serviços externos e regras de negócio.
- Persistir informações em banco de dados relacional.
- Cadastrar ações brasileiras e americanas.
- Cadastrar e validar corretoras.
- Registrar operações de compra e venda.
- Calcular automaticamente as posições da carteira.
- Calcular o preço médio dos ativos.
- Calcular resultados realizados em vendas.
- Calcular resultados não realizados das posições abertas.
- Calcular o patrimônio atual.
- Permitir acompanhamento da evolução patrimonial.
- Implementar tratamento centralizado de exceções.
- Documentar os endpoints da aplicação.
- Aplicar boas práticas de organização de código.
- Isolar integrações externas das regras de negócio.
- Preparar a arquitetura para futuras expansões do produto.

---

## 5. Público-Alvo

O produto será inicialmente direcionado a investidores que desejam controlar uma carteira de ações de maneira simples e centralizada.

O sistema busca atender principalmente usuários que:

- investem em ações brasileiras e/ou americanas;
- utilizam uma ou mais corretoras;
- atualmente utilizam planilhas ou controles manuais;
- possuem dificuldade em acompanhar seus investimentos;
- desejam acompanhar preço médio e resultados;
- desejam visualizar rapidamente a situação da carteira;
- preferem uma interface com menor complexidade visual.

---

## 6. Escopo do MVP

O MVP será focado exclusivamente no controle de ações.

Deverá contemplar:

- ações brasileiras;
- ações americanas;
- cadastro e consulta de ações;
- consulta de cotações;
- cadastro de corretoras;
- validação de CNPJ;
- consulta e validação de CEP;
- cadastro de operações;
- operações de compra;
- operações de venda;
- associação opcional entre operação e corretora;
- criação da carteira;
- posição consolidada por ação;
- cálculo da quantidade atual;
- cálculo de preço médio;
- cálculo do custo da posição;
- cálculo do valor atual;
- lucro ou prejuízo realizado;
- lucro ou prejuízo não realizado;
- rentabilidade;
- patrimônio total;
- evolução patrimonial;
- integração com serviços externos;
- interface Angular responsiva.

---

## 7. Fora do Escopo Inicial

Não fazem parte obrigatoriamente do MVP:

- autenticação;
- gerenciamento de usuários;
- autorização por perfil;
- fundos imobiliários;
- ETFs;
- renda fixa;
- criptomoedas;
- fundos de investimento;
- dividendos;
- juros sobre capital próprio;
- cálculo tributário;
- declaração de Imposto de Renda;
- integração automática com corretoras;
- importação automática de notas de corretagem;
- eventos corporativos complexos.

Esses recursos poderão ser considerados em versões posteriores.

---

## 8. Modelo Conceitual

O domínio inicial deverá possuir quatro elementos principais:

```text
Carteira
   |
   | possui
   v
Operação ---------> Ação
   |
   | opcionalmente realizada por
   v
Corretora
```

### Ação

Representa o ativo negociado no mercado.

Exemplos:

```text
PETR4
VALE3
AAPL
MSFT
```

### Operação

Representa uma movimentação realizada sobre determinada ação.

Inicialmente:

```text
COMPRA
VENDA
```

### Corretora

Representa a instituição pela qual uma determinada operação pode ter sido realizada.

Sua associação com a operação será opcional.

### Carteira

Representa o conjunto consolidado das operações e posições de investimento.

---

## 9. Entidades

### 9.1 Corretora

Atributos inicialmente previstos:

- id;
- cnpj;
- razaoSocial;
- nomeFantasia;
- email;
- telefone;
- cep;
- logradouro;
- numero;
- complemento;
- bairro;
- cidade;
- uf;
- situacaoCadastral;
- validadaMercadoFinanceiro;
- dataCadastro.

---

### 9.2 Ação

Atributos inicialmente previstos:

- id;
- ticker;
- nomeEmpresa;
- mercado;
- moeda;
- cotacaoAtual;
- dataHoraCotacao.

A ação não deverá possuir associação direta obrigatória com uma corretora.

#### Mercado

Inicialmente:

```text
BRASIL
EUA
```

#### Moeda

Inicialmente:

```text
BRL
USD
```

---

### 9.3 Carteira

Atributos inicialmente previstos:

- id;
- nome;
- dataCriacao.

No MVP, não será necessário associar a carteira a um usuário autenticado.

A arquitetura deverá permitir que essa associação seja introduzida futuramente.

---

### 9.4 Operação

Atributos inicialmente previstos:

- id;
- carteira;
- acao;
- corretora;
- tipoOperacao;
- quantidade;
- precoUnitario;
- dataOperacao;
- valorTotal.

A associação com `corretora` será opcional.

#### Tipo de Operação

O tipo deverá ser limitado inicialmente aos valores:

```text
COMPRA
VENDA
```

Na implementação Java, poderá ser representado por um `enum`.

---

### 9.5 Histórico de Cotação

Entidade candidata para armazenamento das cotações obtidas ao longo do tempo.

Possíveis atributos:

- id;
- acao;
- cotacao;
- dataHora.

A estratégia definitiva de armazenamento deverá ser definida durante a modelagem técnica.

---

### 9.6 Snapshot da Carteira

Entidade candidata para acompanhamento da evolução patrimonial.

Possíveis atributos:

- id;
- carteira;
- valorPatrimonial;
- dataHora.

Sua necessidade e frequência de geração deverão ser definidas durante a implementação.

---

## 10. Requisitos Funcionais

### 10.1 Corretoras

#### RF01 — Cadastrar corretora

O sistema deverá permitir cadastrar uma corretora a partir de seu CNPJ.

#### RF02 — Consultar dados cadastrais

O sistema deverá consultar automaticamente os dados cadastrais da empresa utilizando a BrasilAPI.

#### RF03 — Validar dados da corretora

O sistema deverá validar os dados obtidos antes de concluir o cadastro.

Caso seja implementada validação específica da atuação da instituição no mercado financeiro, deverá ser utilizada uma fonte pública adequada.

#### RF04 — Consultar CEP

O sistema deverá consultar e validar o CEP informado utilizando a ViaCEP.

#### RF05 — Listar corretoras

O sistema deverá permitir listar as corretoras cadastradas.

#### RF06 — Consultar corretora

O sistema deverá permitir buscar uma corretora por ID ou CNPJ.

---

### 10.2 Ações

#### RF07 — Cadastrar ação

O sistema deverá permitir cadastrar uma ação informando ticker e mercado.

#### RF08 — Consultar cotação da ação

O sistema deverá consultar, por meio da API externa correspondente ao mercado do ativo, o preço de mercado mais recente disponível da ação.

A aplicação deverá registrar:

- valor da cotação obtida;
- data/hora da consulta ou cotação;
- origem da informação quando necessário.

Para ações brasileiras deverá ser utilizada a BRAPI.

Para ações americanas deverá ser utilizada a Alpha Vantage.

#### RF09 — Listar ações

O sistema deverá permitir listar as ações cadastradas.

#### RF10 — Consultar ação

O sistema deverá permitir buscar uma ação por ID ou ticker.

#### RF11 — Atualizar cotação

O sistema deverá permitir solicitar uma nova consulta à API correspondente para atualizar a última cotação conhecida de uma ação cadastrada.

#### RF12 — Impedir duplicidade

O sistema deverá impedir o cadastro duplicado da mesma combinação de ticker e mercado.

---

### 10.3 Carteira e Operações

#### RF13 — Criar carteira

O sistema deverá permitir criar uma carteira de investimentos.

#### RF14 — Registrar compra

O sistema deverá permitir registrar uma operação de compra de ações.

#### RF15 — Registrar venda

O sistema deverá permitir registrar uma operação de venda de ações.

#### RF16 — Associar corretora

O sistema deverá permitir associar opcionalmente uma operação a uma corretora cadastrada.

#### RF17 — Consultar posição consolidada

O sistema deverá apresentar a posição consolidada de cada ação da carteira.

#### RF18 — Calcular quantidade atual

O sistema deverá calcular automaticamente a quantidade atual disponível de cada ação.

#### RF19 — Calcular preço médio

O sistema deverá calcular automaticamente o preço médio de aquisição de cada ação com base nas operações de compra registradas.

Novas operações de compra deverão recalcular o preço médio utilizando o custo médio ponderado da posição.

Operações de venda deverão reduzir a quantidade disponível da posição, sem alterar o preço médio unitário das ações remanescentes.

#### RF20 — Calcular custo da posição

O sistema deverá calcular o custo correspondente às ações atualmente mantidas.

#### RF21 — Calcular valor atual

O sistema deverá calcular o valor atual da posição utilizando a cotação mais recente disponível.

#### RF22 — Calcular resultado não realizado

O sistema deverá calcular o lucro ou prejuízo não realizado das ações que permanecem na carteira.

#### RF23 — Calcular rentabilidade

O sistema deverá apresentar a rentabilidade percentual da posição aberta.

#### RF24 — Calcular patrimônio

O sistema deverá apresentar o patrimônio atual total da carteira.

#### RF25 — Exibir evolução patrimonial

O sistema deverá permitir acompanhar a evolução do patrimônio da carteira ao longo do tempo.

#### RF26 — Calcular resultado realizado

O sistema deverá calcular o lucro ou prejuízo realizado decorrente das operações de venda.

---

## 11. Regras de Negócio

### Corretoras

**RN01** — Uma corretora somente poderá ser cadastrada se o CNPJ possuir formato válido e existir na BrasilAPI.

**RN02** — Os principais dados cadastrais disponíveis deverão ser obtidos através da BrasilAPI, evitando preenchimento manual quando a informação estiver disponível externamente.

**RN03** — Quando houver validação específica da instituição no mercado financeiro, o resultado deverá ser armazenado ou explicitamente apresentado.

**RN04** — O CEP deverá ser validado através da ViaCEP antes do salvamento dos dados de endereço.

---

### Ações

**RN05** — Uma ação somente poderá ser cadastrada se o ticker puder ser encontrado na API correspondente ao mercado selecionado.

**RN06** — O sistema deverá distinguir ações brasileiras e americanas.

**RN07** — A API utilizada deverá ser determinada pelo mercado da ação:

```text
BRASIL → BRAPI
EUA    → Alpha Vantage
```

**RN08** — Não será permitido cadastrar duas ações com a mesma combinação de ticker e mercado.

---

### Operações e Carteira

**RN09** — Toda operação deverá estar associada a uma ação válida.

**RN10** — A associação entre operação e corretora será opcional.

**RN11** — Quantidade e preço unitário deverão possuir valores maiores que zero.

**RN12 — Recálculo da posição**

O sistema deverá recalcular automaticamente a posição do ativo após cada operação de compra ou venda.

O recálculo deverá atualizar a quantidade, o custo da posição e os resultados correspondentes. O preço médio unitário deverá ser recalculado somente quando ocorrer uma nova operação de compra.

**RN13 — Operação de compra**

Uma operação de compra deverá:

- aumentar a quantidade da posição;
- aumentar o custo da posição;
- provocar o recálculo do preço médio ponderado.

**RN14 — Operação de venda**

Uma operação de venda deverá:

- reduzir a quantidade disponível;
- reduzir o custo da posição;
- manter inalterado o preço médio unitário das ações remanescentes;
- utilizar o preço médio vigente imediatamente antes da venda para determinar o custo correspondente às unidades vendidas;
- calcular o lucro ou prejuízo realizado.

**RN15 — Preço médio após venda**

A operação de venda não deverá recalcular nem alterar o preço médio unitário das ações remanescentes.

O custo da posição deverá ser reduzido proporcionalmente à quantidade vendida utilizando o preço médio vigente antes da venda.

Caso uma venda encerre completamente a posição do ativo, deixando sua quantidade igual a zero, a posição será considerada encerrada. Uma futura compra do mesmo ativo deverá iniciar um novo cálculo de preço médio com base nas novas operações de compra.

**RN16 — Resultado realizado**

O lucro ou prejuízo realizado deverá ser calculado considerando:

```text
(preço de venda - preço médio vigente antes da venda)
× quantidade vendida
```

**RN17 — Validação da venda**

Não será permitida uma venda cuja quantidade seja superior à quantidade disponível do ativo na carteira.

**RN18 — Quantidade atual**

A quantidade atual deverá refletir todas as operações registradas:

```text
quantidadeAtual =
totalComprado - totalVendido
```

**RN19 — Valor atual**

O valor atual da posição deverá utilizar a cotação mais recente disponível.

**RN20 — Resultado não realizado**

O lucro ou prejuízo não realizado deverá considerar apenas a posição atualmente mantida.

**RN21 — Falha na atualização de cotação**

Caso uma nova cotação não possa ser obtida, o sistema deverá informar que está utilizando a última cotação válida disponível, quando existente.

**RN22 — Tipo de operação**

Toda operação deverá possuir um tipo válido:

```text
COMPRA
VENDA
```

---

## 12. Regras de Cálculo

### 12.1 Compra e Cálculo do Preço Médio

O preço médio deverá ser calculado utilizando o custo médio ponderado das operações de compra.

Para uma nova compra:

```text
custoNovaCompra =
quantidadeComprada × precoCompra
```

O novo custo da posição será:

```text
novoCusto =
custoAtual + custoNovaCompra
```

A nova quantidade será:

```text
novaQuantidade =
quantidadeAtual + quantidadeComprada
```

O novo preço médio será:

```text
novoPrecoMedio =
novoCusto / novaQuantidade
```

Equivalente a:

```text
novoPrecoMedio =

((quantidadeAtual × precoMedioAtual)
+ (quantidadeComprada × precoCompra))
/
(quantidadeAtual + quantidadeComprada)
```

No escopo inicial do projeto, o cálculo do preço médio considerará a quantidade e o preço unitário das operações de compra, sem incluir taxas, emolumentos ou impostos.

---

### 12.2 Venda

Operações de venda não deverão recalcular o preço médio unitário da posição.

Antes da venda deverá ser identificado o preço médio vigente.

O custo correspondente às unidades vendidas será:

```text
custoQuantidadeVendida =
quantidadeVendida × precoMedioVigente
```

A quantidade restante será:

```text
novaQuantidade =
quantidadeAtual - quantidadeVendida
```

O novo custo da posição será:

```text
novoCusto =
custoAtual - custoQuantidadeVendida
```

O preço médio unitário da posição remanescente permanece inalterado:

```text
novoPrecoMedio =
precoMedioVigente
```

Caso a quantidade restante seja igual a zero, a posição será considerada encerrada.

Uma futura operação de compra do mesmo ativo deverá iniciar um novo cálculo de preço médio com base nas novas aquisições.

---

### 12.3 Resultado Realizado

Para uma venda:

```text
resultadoRealizado =
(precoVenda - precoMedioVigente)
× quantidadeVendida
```

Resultado positivo:

```text
LUCRO
```

Resultado negativo:

```text
PREJUÍZO
```

---

### 12.4 Valor Atual da Posição

```text
valorAtual =
quantidadeAtual × cotacaoAtual
```

---

### 12.5 Custo da Posição Atual

```text
custoPosicao =
quantidadeAtual × precoMedio
```

---

### 12.6 Resultado Não Realizado

```text
resultadoNaoRealizado =
valorAtual - custoPosicao
```

O resultado não realizado representa a valorização ou desvalorização das ações que ainda permanecem na carteira.

---

### 12.7 Rentabilidade da Posição

```text
rentabilidade =
(resultadoNaoRealizado / custoPosicao) × 100
```

---

### 12.8 Patrimônio Atual

```text
patrimonioTotal =
soma(valorAtual de todas as posições abertas)
```

---

## 13. Exemplo de Cálculo

Considere inicialmente:

```text
Compra:
100 ações × R$ 10,00
```

Resultado:

```text
Quantidade: 100
Custo: R$ 1.000,00
Preço médio: R$ 10,00
```

Segunda compra:

```text
50 ações × R$ 16,00
```

Resultado:

```text
Quantidade: 150
Custo: R$ 1.800,00
Preço médio: R$ 12,00
```

O preço médio foi recalculado pela nova operação de compra:

```text
Preço médio =
((100 × R$ 10,00) + (50 × R$ 16,00))
/
(100 + 50)

Preço médio =
R$ 1.800,00 / 150

Preço médio = R$ 12,00
```

Posteriormente:

```text
Venda:
50 ações × R$ 15,00
```

A operação de venda não recalcula o preço médio unitário.

Custo das ações vendidas:

```text
50 × R$ 12,00 = R$ 600,00
```

Resultado realizado:

```text
(R$ 15,00 - R$ 12,00) × 50
= R$ 150,00 de lucro
```

Nova posição:

```text
Quantidade: 100
Custo: R$ 1.200,00
Preço médio: R$ 12,00
```

Portanto, a venda reduz a quantidade e o custo total da posição, mas mantém inalterado o preço médio unitário das ações restantes.

---

## 14. Integrações Externas

As seguintes APIs foram definidas para o projeto.

### 14.1 BRAPI

**Responsabilidade:** ações brasileiras.

Utilização prevista:

- validar ticker brasileiro;
- obter informações básicas do ativo;
- consultar cotação;
- atualizar cotação.

---

### 14.2 Alpha Vantage

**Responsabilidade:** ações americanas.

Utilização prevista:

- validar ticker americano;
- obter informações disponíveis do ativo;
- consultar cotação;
- atualizar cotação.

---

### 14.3 BrasilAPI

**Responsabilidade:** consulta de CNPJ.

Utilização prevista:

- validar existência do CNPJ;
- consultar razão social;
- consultar nome fantasia;
- obter dados cadastrais disponíveis.

---

### 14.4 ViaCEP

**Responsabilidade:** consulta e validação de CEP.

Utilização prevista:

- verificar existência do CEP;
- preencher informações de endereço disponíveis.

---

## 15. Isolamento das Integrações Externas

As regras de negócio não deverão depender diretamente da implementação específica de uma API externa.

Deverão ser utilizadas abstrações para isolar os serviços de terceiros.

Exemplo:

```text
                  ┌── BrapiAdapter
                  │       ↓
CotacaoProvider ──┤      BRAPI
                  │
                  └── AlphaVantageAdapter
                          ↓
                    Alpha Vantage
```

Para dados brasileiros:

```text
CnpjProvider
     ↓
BrasilApiAdapter
     ↓
BrasilAPI
```

```text
CepProvider
     ↓
ViaCepAdapter
     ↓
ViaCEP
```

Padrões de projeto candidatos:

- Adapter;
- Strategy;
- Gateway/Provider.

Essa estrutura deverá permitir substituir uma integração externa sem provocar alterações significativas nas regras de negócio.

---

## 16. Requisitos Não Funcionais

### RNF01 — Backend

A aplicação deverá utilizar Java e Spring Boot.

### RNF02 — Frontend

A aplicação deverá utilizar Angular.

### RNF03 — Arquitetura em camadas

O backend deverá possuir, no mínimo:

```text
controller
service
repository
entity/model
dto
```

### RNF04 — Banco de Dados

Deverão ser utilizados:

- PostgreSQL como banco relacional principal;
- H2 para desenvolvimento e/ou testes quando necessário.

### RNF05 — Comunicação

As APIs REST deverão utilizar JSON.

### RNF06 — Tratamento de erros

O tratamento de exceções deverá ser centralizado.

### RNF07 — Responsividade

A interface deverá funcionar adequadamente em desktop, tablet e dispositivos móveis.

### RNF08 — Usabilidade

A interface deverá priorizar clareza, simplicidade e facilidade de identificação das informações.

### RNF09 — Integrações

Serviços externos deverão ser isolados das regras centrais da aplicação.

### RNF10 — Manutenibilidade

O código deverá apresentar:

- organização;
- legibilidade;
- padronização;
- separação de responsabilidades;
- baixo acoplamento.

---

## 17. Diretrizes de UX/UI

A interface deverá seguir uma abordagem **clean, responsiva e orientada às informações essenciais**.

A proposta deverá evitar excesso de dados simultâneos e priorizar as informações necessárias para que o usuário compreenda rapidamente sua carteira.

### 17.1 Princípios

A interface deverá:

- possuir hierarquia visual clara;
- evitar poluição visual;
- destacar informações relevantes;
- separar informações primárias de detalhes;
- possuir navegação consistente;
- utilizar nomenclaturas compreensíveis;
- utilizar componentes reutilizáveis;
- funcionar em diferentes tamanhos de tela;
- utilizar gráficos somente quando contribuírem para a compreensão das informações.

---

## 18. Dashboard

A página inicial da carteira deverá priorizar indicadores como:

```text
Patrimônio atual

Resultado não realizado

Resultado realizado

Rentabilidade

Evolução patrimonial

Posições atuais
```

Uma posição poderá ser apresentada conceitualmente como:

```text
PETR4 — Petrobras

Quantidade           100
Preço médio           R$ 12,00
Cotação atual         R$ 14,00
Custo da posição      R$ 1.200,00
Valor atual           R$ 1.400,00
Resultado             +R$ 200,00
Rentabilidade         +16,67%
```

Detalhes adicionais deverão preferencialmente ficar disponíveis em uma página específica da posição, evitando excesso de informações no dashboard.

---

## 19. Endpoints Iniciais

### Corretoras

```text
POST /corretoras
GET  /corretoras
GET  /corretoras/{id}
GET  /corretoras/cnpj/{cnpj}
```

### Ações

```text
POST /acoes
GET  /acoes
GET  /acoes/{id}
GET  /acoes/ticker/{ticker}
PUT  /acoes/{id}/atualizar-cotacao
```

### Carteiras

```text
POST /carteiras
GET  /carteiras
GET  /carteiras/{id}
GET  /carteiras/{id}/posicoes
GET  /carteiras/{id}/resumo
GET  /carteiras/{id}/evolucao
```

### Operações

```text
POST /carteiras/{carteiraId}/operacoes
GET  /carteiras/{carteiraId}/operacoes
GET  /carteiras/{carteiraId}/operacoes/{id}
```

Os endpoints poderão ser refinados durante a modelagem da API.

---

## 20. Tratamento de Falhas

O sistema deverá tratar pelo menos:

- CNPJ inválido;
- CNPJ inexistente;
- CEP inválido;
- CEP inexistente;
- ticker inexistente;
- mercado não suportado;
- API externa indisponível;
- timeout;
- limite de requisições excedido;
- tentativa de cadastro duplicado;
- quantidade inválida;
- preço inválido;
- venda superior à posição disponível;
- cotação indisponível;
- dados obrigatórios ausentes.

As respostas da API deverão possuir formato de erro padronizado.

---

## 21. Tecnologias

### Backend

- Java;
- Spring Boot;
- Spring Web;
- Spring Data JPA;
- Bean Validation;
- WebClient ou OpenFeign.

### Frontend

- Angular;
- TypeScript;
- HTML;
- CSS.

### Banco de Dados

- PostgreSQL;
- H2.

### APIs Externas

- BRAPI;
- Alpha Vantage;
- BrasilAPI;
- ViaCEP.

### Ferramentas de desenvolvimento

O fluxo de desenvolvimento também utilizará:

- Codex;
- OpenSpec;
- Graphify.

Essas ferramentas deverão auxiliar na especificação, implementação, compreensão da base de código e manutenção da consistência entre requisitos e código.

---

## 22. Documentação

O projeto deverá possuir README contendo:

- descrição do sistema;
- problema;
- objetivo;
- arquitetura;
- tecnologias utilizadas;
- requisitos para execução;
- configuração do backend;
- configuração do frontend;
- configuração do banco;
- APIs externas utilizadas;
- justificativa das integrações;
- limitações das APIs;
- autenticação necessária para APIs externas;
- limites de requisições;
- instruções para execução.

Os endpoints da API REST também deverão possuir documentação.

---

## 23. Estratégia de Testes

Deverão ser considerados:

- testes unitários;
- testes de regras de negócio;
- testes dos cálculos financeiros;
- testes de integração;
- testes dos repositories;
- testes dos endpoints;
- testes das integrações externas;
- testes de cenários de falha.

Deverão receber atenção especial:

- cálculo de preço médio;
- múltiplas compras;
- venda parcial;
- venda total;
- tentativa de venda superior à posição;
- resultado realizado;
- resultado não realizado;
- atualização de cotação.

Deverá ser entregue uma coleção de testes utilizando Postman ou Insomnia.

---

## 24. Diferenciais

Poderão ser implementados:

- OpenFeign ou WebClient;
- testes unitários;
- testes de integração;
- paginação;
- logs estruturados;
- cache de consultas externas;
- dashboard Angular;
- gráficos de evolução;
- histórico de cotações;
- histórico patrimonial;
- arquitetura extensível para novos investimentos.

---

## 25. Restrições

- Não cadastrar dados completamente fictícios quando houver requisito de integração externa.
- Demonstrar pelo menos três integrações externas reais.
- Tratar indisponibilidade dos serviços externos.
- Tratar limites de requisição.
- Validar ticker.
- Validar CNPJ.
- Validar CEP.
- Não permitir venda superior à quantidade disponível.
- Não depender exclusivamente de uma API externa para informações históricas já persistidas.
- Autenticação não fará parte do MVP inicial.

---

## 26. Entregáveis

- código-fonte completo do backend;
- código-fonte completo do frontend;
- README;
- documentação das APIs externas;
- documentação dos endpoints;
- coleção Postman ou Insomnia;
- diagrama simplificado das entidades;
- aplicação Angular responsiva;
- apresentação prática do sistema funcionando.

---

## 27. Evoluções Futuras

### 27.1 Usuários e autenticação

Em uma versão posterior:

- cadastro de usuários;
- login;
- Spring Security;
- autenticação;
- autorização;
- associação das carteiras ao usuário;
- múltiplas carteiras por usuário.

### 27.2 Outros investimentos

A arquitetura deverá permitir futura inclusão de:

- fundos imobiliários;
- ETFs;
- renda fixa;
- fundos de investimento;
- criptomoedas;
- outros ativos.

### 27.3 Controle avançado

Poderão ser avaliados posteriormente:

- dividendos;
- JCP;
- taxas de corretagem;
- emolumentos;
- bonificações;
- desdobramentos;
- grupamentos;
- eventos corporativos;
- cálculo tributário;
- importação de notas de corretagem.

---

## 28. Critérios de Sucesso do MVP

O MVP será considerado funcional quando for possível:

1. cadastrar uma corretora utilizando um CNPJ real;
2. consultar seus dados através da BrasilAPI;
3. consultar e validar endereço através da ViaCEP;
4. cadastrar uma ação brasileira utilizando a BRAPI;
5. cadastrar uma ação americana utilizando a Alpha Vantage;
6. consultar a cotação de uma ação;
7. criar uma carteira;
8. registrar uma compra;
9. registrar uma venda;
10. associar opcionalmente uma operação a uma corretora;
11. calcular corretamente a quantidade atual;
12. calcular corretamente o preço médio após compras;
13. reduzir corretamente o custo da posição após vendas;
14. calcular lucro ou prejuízo realizado;
15. calcular lucro ou prejuízo não realizado;
16. calcular a rentabilidade;
17. apresentar o patrimônio atual;
18. acompanhar a evolução patrimonial;
19. apresentar as informações através de uma interface Angular clara e responsiva;
20. tratar adequadamente falhas das integrações externas.

---

## 29. Princípio de Evolução do Produto

O desenvolvimento deverá seguir o princípio:

> **Primeiro tornar o controle de ações simples, confiável e funcional; posteriormente expandir a aplicação para uma plataforma mais completa de gestão de investimentos.**

A arquitetura deverá permitir evolução sem adicionar ao MVP complexidades que pertencem apenas às versões futuras.