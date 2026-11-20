/** Paths decodificados pelo Router; o backend mantém a autorização efetiva. */
export function requiresAdminPath(path: string): boolean {
  return /^\/usuarios(?:\/|$)/.test(path)
    || /^\/(?:carteiras|acoes|corretoras|operacoes)\/nova$/.test(path)
    || /^\/carteiras\/[1-9]\d*\/editar$/.test(path);
}
