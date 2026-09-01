export interface CarteiraResponse {
  id: number;
  nome: string;
  dataCriacao: string;
}

export interface CarteiraCreateRequest {
  nome: string;
}

export interface CarteiraUpdateRequest {
  nome: string;
}
