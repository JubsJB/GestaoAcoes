const JSON_NUMBER = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/;

export function parseLosslessJson<T>(text: string, decimalFields: ReadonlySet<string>): T {
  if (typeof text !== 'string' || !text.trim()) {
    throw new Error('Resposta JSON vazia.');
  }
  return JSON.parse(quoteDecimalFields(text, decimalFields)) as T;
}

function quoteDecimalFields(json: string, decimalFields: ReadonlySet<string>): string {
  let output = '';
  let index = 0;
  while (index < json.length) {
    if (json[index] !== '"') {
      output += json[index++];
      continue;
    }

    const start = index++;
    while (index < json.length) {
      if (json[index] === '\\') {
        index += 2;
        continue;
      }
      if (json[index++] === '"') break;
    }
    if (index > json.length || json[index - 1] !== '"') {
      throw new Error('Resposta JSON malformada.');
    }

    const token = json.slice(start, index);
    output += token;
    let cursor = index;
    while (/\s/.test(json[cursor] ?? '')) cursor++;
    if (json[cursor] !== ':') continue;

    const key = JSON.parse(token) as string;
    output += json.slice(index, cursor + 1);
    index = cursor + 1;
    if (!decimalFields.has(key)) continue;

    while (/\s/.test(json[index] ?? '')) output += json[index++];
    if (json[index] === '"' || json.startsWith('null', index)) continue;
    const number = JSON_NUMBER.exec(json.slice(index));
    if (!number) throw new Error(`Campo decimal ${key} inválido.`);
    output += JSON.stringify(number[0]);
    index += number[0].length;
  }
  return output;
}
