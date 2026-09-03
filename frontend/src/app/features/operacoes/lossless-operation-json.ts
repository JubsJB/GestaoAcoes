const DECIMAL_FIELDS = new Set(['quantidade', 'precoUnitario', 'precoUnitarioSugerido', 'valorTotal']);

export function parseOperationJson<T>(text: string): T {
  if (typeof text !== 'string' || !text.trim()) throw new Error('Resposta JSON de Operações vazia.');
  return JSON.parse(quoteDecimalFields(text)) as T;
}

function quoteDecimalFields(json: string): string {
  let output = '';
  let index = 0;
  while (index < json.length) {
    if (json[index] !== '"') { output += json[index++]; continue; }
    const start = index++;
    while (index < json.length) {
      if (json[index] === '\\') { index += 2; continue; }
      if (json[index++] === '"') break;
    }
    if (index > json.length || json[index - 1] !== '"') throw new Error('Resposta JSON de Operações malformada.');
    const token = json.slice(start, index);
    output += token;
    let cursor = index;
    while (/\s/.test(json[cursor] ?? '')) cursor++;
    if (json[cursor] !== ':') continue;
    const key = JSON.parse(token) as string;
    output += json.slice(index, cursor + 1);
    index = cursor + 1;
    if (!DECIMAL_FIELDS.has(key)) continue;
    while (/\s/.test(json[index] ?? '')) output += json[index++];
    if (json[index] === '"' || json.startsWith('null', index)) continue;
    const number = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/.exec(json.slice(index));
    if (!number) throw new Error(`Campo decimal ${key} inválido.`);
    output += JSON.stringify(number[0]);
    index += number[0].length;
  }
  return output;
}
