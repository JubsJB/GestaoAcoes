package com.projeto.mappers;

import com.projeto.dto.CarteiraResponse;
import com.projeto.entities.Carteira;
import org.springframework.stereotype.Component;

@Component
public class CarteiraMapper {

    public CarteiraResponse toResponse(Carteira carteira) {
        return new CarteiraResponse(
                carteira.getId(),
                carteira.getNome(),
                carteira.getDataCriacao()
        );
    }
}
