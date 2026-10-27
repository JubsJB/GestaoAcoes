package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
        description="Corpo opcional; aceita somente motivo. Carteira e original vêm do path.")
public final class EstornoCustodiaCreateRequest {
    @Size(max=1000)
    @JsonDeserialize(using=MotivoTextual.class)
    @Schema(nullable=true,maxLength=1000,description="Branco normaliza para null; texto não branco é preservado")
    private String motivo;
    public String getMotivo(){return motivo;}
    public void setMotivo(String valor){motivo=valor;}
    @JsonAnySetter
    public void rejectUnknownProperty(String campo,Object valor){
        throw new IllegalArgumentException("Campo não permitido no estorno de custódia: "+campo);
    }
    /** Restringe somente este campo; não altera coerções dos demais contratos Jackson. */
    public static final class MotivoTextual extends ValueDeserializer<String> {
        @Override public String deserialize(JsonParser parser,DeserializationContext contexto){
            if(!parser.hasToken(JsonToken.VALUE_STRING)){
                return contexto.reportInputMismatch(String.class,"Motivo deve ser string ou null");
            }
            return parser.getString();
        }
    }
}
