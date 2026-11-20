import { NormalizedHttpError } from './normalized-http-error';

const MENSAGENS: Readonly<Record<string, string>> = {
  POSICAO_INSUFICIENTE: 'A quantidade excede a posição disponível nesse ponto cronológico.',
  SALDO_CORRETORA_INSUFICIENTE: 'O saldo da corretora informada ou do grupo sem corretora é insuficiente na data da operação.',
  SALDO_ORIGEM_INSUFICIENTE: 'A origem não tem saldo suficiente na data efetiva.',
  POSICAO_CUSTODIA_INDISPONIVEL: 'Não há posição cronológica disponível que preserve o histórico.',
  CONFLITO_HISTORICO_QUANTITATIVO: 'A movimentação criaria ou agravaria um déficit histórico.',
  MOVIMENTACAO_CUSTODIA_INVALIDADA: 'A movimentação afetaria uma custódia já registrada.',
  DATA_EFETIVA_FUTURA: 'A data efetiva não pode ser futura no mercado selecionado.',
  ESTORNO_DUPLICADO: 'Esta movimentação já possui estorno. Atualize o histórico para consultar o vínculo.',
  LIMITE_POSICAO_CUSTODIA: 'O limite da posição cronológica foi atingido.',
  CONFLITO_TRANSACIONAL_TRANSITORIO: 'Houve um conflito temporário. Confira os dados antes de tentar novamente.'
};
export function codigoQuantitativo(code: unknown): code is string { return typeof code === 'string' && Object.hasOwn(MENSAGENS, code); }
export function mensagemQuantitativa(erro: NormalizedHttpError | null, contexto: 'operacao' | 'cadastro' | 'estorno' = 'operacao'): string | null {
  if (!codigoQuantitativo(erro?.code)) return null;
  if (contexto === 'cadastro' && erro.code === 'POSICAO_INSUFICIENTE') return 'A posição consolidada é insuficiente.';
  if (contexto === 'estorno') {
    const mensagens: Record<string, string> = {
      MOVIMENTACAO_CUSTODIA_INVALIDADA: 'O estorno invalidaria uma custódia posterior dependente.',
      CONFLITO_HISTORICO_QUANTITATIVO: 'O estorno criaria ou agravaria um déficit histórico.',
      POSICAO_INSUFICIENTE: 'O estorno é incompatível com a posição disponível no histórico.',
      SALDO_ORIGEM_INSUFICIENTE: 'O histórico resultante não preserva o saldo de origem.',
      CONFLITO_TRANSACIONAL_TRANSITORIO: 'Houve um conflito temporário. Nenhum estorno foi confirmado nesta tentativa.'
    };
    return mensagens[erro.code] ?? MENSAGENS[erro.code];
  }
  return MENSAGENS[erro.code];
}

/** Projeta somente os campos públicos do contrato quantitativo; não serializa causas internas. */
export function detalhesQuantitativos(erro: NormalizedHttpError | null): Record<string, unknown> {
  if (!erro || erro.kind === 'technical' || erro.status >= 500) return {};
  if (!codigoQuantitativo(erro.code)) return erro.details ?? {};
  const d = objeto(erro.details) ? erro.details : {};
  const resultado = escalares(d, ['carteiraId', 'acaoId', 'ticker', 'mercado', 'corretoraId',
    'quantidadeSolicitada', 'quantidadeDisponivel', 'saldoAntes', 'saldoDepois', 'deficitAntes', 'deficitDepois', 'motivo']);
  for (const chave of ['origem', 'grupo']) if (Object.hasOwn(d, chave))
    resultado[chave] = d[chave] === null ? null : escalares(d[chave], ['corretoraId', 'categoria']);
  if (Object.hasOwn(d, 'evento')) {
    if (d['evento'] === null) resultado['evento'] = null;
    else if (objeto(d['evento'])) {
      const evento = d['evento'];
      resultado['evento'] = { ...escalares(evento, ['dominio', 'id', 'natureza', 'dataEfetiva']),
        ...(Object.hasOwn(evento, 'posicaoCronologica') ? { posicaoCronologica: evento['posicaoCronologica'] === null ? null
          : escalares(evento['posicaoCronologica'], ['ordemNoDia', 'ordemFinanceiraAncora', 'numerador', 'denominador']) } : {}) };
    }
  }
  return resultado;
}
function objeto(v: unknown): v is Record<string, unknown> { return v !== null && typeof v === 'object' && !Array.isArray(v); }
function escalares(value: unknown, campos: string[]): Record<string, unknown> {
  if (!objeto(value)) return {};
  return Object.fromEntries(campos.filter(c => Object.hasOwn(value, c)
    && (value[c] === null || typeof value[c] === 'string' || typeof value[c] === 'number' || typeof value[c] === 'boolean'))
    .map(c => [c, value[c]]));
}
