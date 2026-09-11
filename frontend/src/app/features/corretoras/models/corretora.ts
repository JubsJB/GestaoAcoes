export interface Corretora {
  id: number;
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string | null;
  email: string | null;
  telefone: string | null;
  cep: string;
  logradouro: string;
  numero: string | null;
  complemento: string | null;
  bairro: string;
  cidade: string;
  uf: string;
  situacaoCadastral: string;
  validadaMercadoFinanceiro: boolean;
  dataCadastro: string;
}

export interface CorretoraCreateRequest {
  cnpj: string;
  confirmarSituacaoCadastralNaoAtiva?: true;
}
