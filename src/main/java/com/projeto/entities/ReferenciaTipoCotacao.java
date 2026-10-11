package com.projeto.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public enum ReferenciaTipoCotacao {
    INSTANTE, DATA, DESCONHECIDA;

    void validar(OffsetDateTime instante, LocalDate data) {
        boolean valida = switch (this) {
            case INSTANTE -> instante != null && data == null;
            case DATA -> data != null && instante == null;
            case DESCONHECIDA -> instante == null && data == null;
        };
        if (!valida) {
            throw new IllegalArgumentException("Referência temporal incompatível com o tipo da cotação");
        }
    }
}
