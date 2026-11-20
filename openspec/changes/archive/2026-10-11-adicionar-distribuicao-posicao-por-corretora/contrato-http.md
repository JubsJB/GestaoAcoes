# Contrato HTTP final

Este documento integra o design e os criterios normativos dos deltas. Nao e codigo implementado. Nenhum contrato anterior e modificado.

## Endpoint e status

GET /carteiras/{carteiraId}/posicoes/distribuicao-corretoras,application/json,sem body funcional,sem filtros/paginacao nesta fatia.
- 200:Carteira existente,com posicoes abertas ou posicoes:[],inclusive deficits e divergencias diagnosticados.
- 404:Carteira inexistente,StandardError vigente,code:null e details:{}.
- 400:identificador que nao pode ser convertido ao tipo Long,REQUEST_INVALIDO,conforme tratamento existente.
- 409:HISTORICO_OPERACOES_INCONSISTENTE quando replay CONSOLIDADO e estrutural/cronologicamente invalido;nao fabricar distribuicao parcial.
- 500:falha tecnica inesperada,StandardError seguro com code:FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS,message:"Nao foi possivel consultar a distribuicao por corretora.",details:{}. Nenhuma stack/SQL/credencial no response. Novo codigo localizado segue padrao de ApiException/StandardError;nao muda handlers/erros dos demais endpoints.
Deficit de corretora e divergencia NAO sao409/500. Falhas financeiras de outros endpoints nao devem ser importadas por calcular preco/custo neste GET quantitativo.

## Schema exato

Todos os campos abaixo sao OBRIGATORIOS,presentes inclusive quando nulos. Objetos de sucesso nao possuem propriedades adicionais nesta versao. Todas as colecoes sao arrays nunca null. IDs sao JSON integer,int64 positivo;quantidades JSON number decimal exato (BigDecimal),nunca double/string no wire;Angular preserva token numerico como string lossless. Bool e JSON boolean,nunca texto.

| Objeto | Campos e tipos |
| --- | --- |
| DistribuicaoCarteiraResponse | carteiraId:int64;posicoes:PosicaoDistribuicaoResponse[] |
| PosicaoDistribuicaoResponse | acaoId:int64;ticker:string nao vazia;mercado:BRASIL ou EUA;moeda:BRL ou USD;quantidadeConsolidada:decimal positivo;quantidadeAtribuidaTotal:decimal com sinal;reconciliada:boolean;diagnosticoReconciliacao:DiagnosticoReconciliacaoResponse ou null;corretoras:QuantidadeCorretoraResponse[];diagnosticosCorretoras:DiagnosticoCorretoraResponse[] |
| QuantidadeCorretoraResponse | corretoraId:int64 ou null;nome:string nao vazia;quantidadeLiquida:decimal nao zero,positivo ou negativo |
| DiagnosticoCorretoraResponse | corretoraId:int64 ou null;nome:string nao vazia;quantidadeFinal:decimal com sinal,inclusive zero;saldoNegativoAtual:boolean;possuiDeficitHistorico:boolean;primeiroDeficit:PrimeiroDeficitResponse ou null |
| PrimeiroDeficitResponse | operacaoId:int64;dataOperacao:string date ISO8601 conforme LocalDate da Operacao persistida;ordemNoDia:integer int32 positivo;saldoAposOperacao:decimal estritamente negativo |
| DiagnosticoReconciliacaoResponse | codigo:literal DIVERGENCIA_QUANTIDADES |

Nomes:trim do nomeFantasia quando nao vazio,senao trim da razaoSocial. Se ambos nao forem utilizaveis,rotulo identificador "Corretora #<id>",no padrao frontend existente,sem inferir instituicao/custodia. Null significa SOMENTE categoria sem corretora,e nome exatamente "Sem corretora informada";sem instituicao ficticia/lookup externo. Mercado BRASIL implica BRL/EUA implica USD.

Cada agrupamento observado tem exatamente UM diagnosticoCorretoras,inclusive saudavel ou final zero. A lista quantitativa tem exatamente os mesmos agrupamentos cujo final nao e zero;ID/nome/quantidade correspondem ao diagnostico. O diagnostico saudavel possui flagsfalse/false e primeiroDeficit:null;nao gera alerta. Zero saudavel nao gera linha ou alerta;zero com deficit tem diagnostico e alerta identificavel sem linha quantitativa. Unicidade por corretoraId,inclusive um unico null,por posicao. Nao usar dois objetos para flags atual/historica.

saldoNegativoAtual e verdadeiro se e somente se quantidadeFinal<0. possuiDeficitHistorico e verdadeiro se qualquer saldo apos operacao foi negativo. Negativo atual implica historico. primeiroDeficit e nao nulo se e somente se possuiDeficitHistorico:true;primeira operacao em dataOperacao ASC,ordemNoDia ASC,id ASC com saldo apos aplicacao<0. Evidencia nao e saldo minimo,nao muda com regularizacao/novo ciclo;identificador e desempate tecnico,nunca nova ordem financeira. Nenhum timestamp de Clock ou valor financeiro na evidencia.

quantidadeAtribuidaTotal soma TODOS os finais com sinal,inclusive negativos,null ezeros;quantidadeConsolidada vem do replay oficial no MESMO snapshot. reconciliada equivale a igualdade numerica (compareTo,nao equals de escala). diagnosticoReconciliacao:null se e somente se reconciliada:true;senao objeto DIVERGENCIA_QUANTIDADES. Nao existe corretoraId/nome no diagnostico da posicao. Os dois valores distintos ja estao no objeto pai;nao duplicar/deduzir diferenca no browser. Reconciliada pode coexistir com deficit historico/negativo atual.

Precisao:operandos existentes inteirosBRASIL/ate6decimaisEUA;somar/subtrair exatamente. Finais e totais derivados nao possuem limite artificial19digitos;poderiam conter compensacoes maiores que consolidado. Nao introduzir arredondamento/truncamento,limite de magnitude no parser,FX ou metricas monetarias.

## Ordenacao completa

Data da evidencia reproduz o texto civil da Operacao sem Clock/timezone nem novos limites financeiros de referencia de cotacao;nao rejeitar registro antigo por regras de DATA introduzidas para aquisicao de cotacoes.

Comparacao no backend,independente da collationSQL/locale do host. Textos comparados por sequencia ordinal de unidadesUTF-16 (String.compareTo),case-sensitive,sem case-folding ou remocao/normalizacao de acentos.
- Posicoes:mercado em ordem explicitaBRASIL,EUA;depois ticker ordinal;depois acaoId numericoASC.
- Agrupamentos:corretoraId nao nulo primeiro;nome ordinalASC;corretoraId numericoASC como desempate. Categoria null sempre por ultimo,independentemente do nome.
- diagnosticosCorretoras usam EXATAMENTE a mesma chave de grupos,incluindo zeros. Objetos sao unicos por grupo;nao ha ordenacao por severidade/tipo.
- Nenhuma ordenacao frontend adicional altera dados ou primeira ocorrencia. Diagnostico de reconciliacao e unico objeto,no array inexistente de avisos globais.

## Exemplos JSON completos

Todos os numeros de exemplo sao pequenos e exatos,nao autorizam coercaoNumber no consumidor. Datas/IDs referem-se apenas a fixtures documentais. A divergencia e exemplo de falha da verificacao,nunca instrucao de fabricar/sanear dados noPostgreSQL.

### Distribuicao reconciliada

```json
{
  "carteiraId": 1,
  "posicoes": [
    {
      "acaoId": 1,
      "ticker": "PETR4",
      "mercado": "BRASIL",
      "moeda": "BRL",
      "quantidadeConsolidada": 100,
      "quantidadeAtribuidaTotal": 100,
      "reconciliada": true,
      "diagnosticoReconciliacao": null,
      "corretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeLiquida": 80
        },
        {
          "corretoraId": null,
          "nome": "Sem corretora informada",
          "quantidadeLiquida": 20
        }
      ],
      "diagnosticosCorretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeFinal": 80,
          "saldoNegativoAtual": false,
          "possuiDeficitHistorico": false,
          "primeiroDeficit": null
        },
        {
          "corretoraId": null,
          "nome": "Sem corretora informada",
          "quantidadeFinal": 20,
          "saldoNegativoAtual": false,
          "possuiDeficitHistorico": false,
          "primeiroDeficit": null
        }
      ]
    }
  ]
}
```

### Saldo negativo atual

```json
{
  "carteiraId": 1,
  "posicoes": [
    {
      "acaoId": 1,
      "ticker": "PETR4",
      "mercado": "BRASIL",
      "moeda": "BRL",
      "quantidadeConsolidada": 60,
      "quantidadeAtribuidaTotal": 60,
      "reconciliada": true,
      "diagnosticoReconciliacao": null,
      "corretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeLiquida": 100
        },
        {
          "corretoraId": 2,
          "nome": "Corretora B",
          "quantidadeLiquida": -40
        }
      ],
      "diagnosticosCorretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeFinal": 100,
          "saldoNegativoAtual": false,
          "possuiDeficitHistorico": false,
          "primeiroDeficit": null
        },
        {
          "corretoraId": 2,
          "nome": "Corretora B",
          "quantidadeFinal": -40,
          "saldoNegativoAtual": true,
          "possuiDeficitHistorico": true,
          "primeiroDeficit": {
            "operacaoId": 20,
            "dataOperacao": "2026-10-02",
            "ordemNoDia": 2,
            "saldoAposOperacao": -40
          }
        }
      ]
    }
  ]
}
```

### Deficit historico regularizado

```json
{
  "carteiraId": 1,
  "posicoes": [
    {
      "acaoId": 1,
      "ticker": "PETR4",
      "mercado": "BRASIL",
      "moeda": "BRL",
      "quantidadeConsolidada": 110,
      "quantidadeAtribuidaTotal": 110,
      "reconciliada": true,
      "diagnosticoReconciliacao": null,
      "corretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeLiquida": 100
        },
        {
          "corretoraId": 2,
          "nome": "Corretora B",
          "quantidadeLiquida": 10
        }
      ],
      "diagnosticosCorretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeFinal": 100,
          "saldoNegativoAtual": false,
          "possuiDeficitHistorico": false,
          "primeiroDeficit": null
        },
        {
          "corretoraId": 2,
          "nome": "Corretora B",
          "quantidadeFinal": 10,
          "saldoNegativoAtual": false,
          "possuiDeficitHistorico": true,
          "primeiroDeficit": {
            "operacaoId": 20,
            "dataOperacao": "2026-10-02",
            "ordemNoDia": 2,
            "saldoAposOperacao": -10
          }
        }
      ]
    }
  ]
}
```

### Corretora zerada com deficit historico

```json
{
  "carteiraId": 1,
  "posicoes": [
    {
      "acaoId": 1,
      "ticker": "PETR4",
      "mercado": "BRASIL",
      "moeda": "BRL",
      "quantidadeConsolidada": 100,
      "quantidadeAtribuidaTotal": 100,
      "reconciliada": true,
      "diagnosticoReconciliacao": null,
      "corretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeLiquida": 100
        }
      ],
      "diagnosticosCorretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeFinal": 100,
          "saldoNegativoAtual": false,
          "possuiDeficitHistorico": false,
          "primeiroDeficit": null
        },
        {
          "corretoraId": 2,
          "nome": "Corretora B",
          "quantidadeFinal": 0,
          "saldoNegativoAtual": false,
          "possuiDeficitHistorico": true,
          "primeiroDeficit": {
            "operacaoId": 20,
            "dataOperacao": "2026-10-02",
            "ordemNoDia": 2,
            "saldoAposOperacao": -10
          }
        }
      ]
    }
  ]
}
```

### Divergencia de reconciliacao

```json
{
  "carteiraId": 1,
  "posicoes": [
    {
      "acaoId": 1,
      "ticker": "PETR4",
      "mercado": "BRASIL",
      "moeda": "BRL",
      "quantidadeConsolidada": 101,
      "quantidadeAtribuidaTotal": 100,
      "reconciliada": false,
      "diagnosticoReconciliacao": {
        "codigo": "DIVERGENCIA_QUANTIDADES"
      },
      "corretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeLiquida": 100
        }
      ],
      "diagnosticosCorretoras": [
        {
          "corretoraId": 1,
          "nome": "Corretora A",
          "quantidadeFinal": 100,
          "saldoNegativoAtual": false,
          "possuiDeficitHistorico": false,
          "primeiroDeficit": null
        }
      ]
    }
  ]
}
```

Carteira sem posicoes abertas:{"carteiraId":1,"posicoes":[]}. Posicoes consolidadas zeradas nao sao retornadas,inclusive se historico contem deficit por corretora. Nao apagar esse historico.

## Aceite frontend e snapshots

Renderizar valores/flags/evidencia recebidos;nao somar quantidades,reavaliar flags ou comparar painel com GET /posicoes adquirido em outro snapshot. Mostrar a identidade da posicao do proprio response;diferenca entre respostas independentes nao vira diagnostico inventado,nem autoriza sobrescrever tabela financeira. Recarga apos operacao consulta ambos sem prometer snapshot unico entrerequests.

No maximo um aviso por corretora inconsistente,combinando flags sem perder primeira evidencia;corretora zero identificada somente no aviso. Reconciliacao divergente recebe aviso separado dePOSICAO,nao "Sem corretora informada". Loading/vazio/erro/retry tem estados proprios e nao apagam consolidado valido. Proibidos percentuais,roscas,graficos de participacao,modulo de negativos ou normalizacao de subconjuntos nesta fatia. Avisos nao dependem somente de cor;nomes,foco,navegacao,anuncios e reflow360px sao criterios de aceite.
