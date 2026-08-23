package com.projeto.services;

import com.projeto.dto.CarteiraCreateRequest;
import com.projeto.dto.CarteiraResponse;
import com.projeto.dto.CarteiraUpdateRequest;
import com.projeto.entities.Carteira;
import com.projeto.mappers.CarteiraMapper;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class CarteiraService {

    private static final int MAX_NAME_LENGTH = 255;

    private final CarteiraRepository repository;
    private final OperacaoRepository operacaoRepository;
    private final CarteiraMapper mapper;
    private final Clock clock;

    public CarteiraService(
            CarteiraRepository repository,
            OperacaoRepository operacaoRepository,
            CarteiraMapper mapper,
            Clock clock
    ) {
        this.repository = repository;
        this.operacaoRepository = operacaoRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public CarteiraResponse cadastrar(CarteiraCreateRequest request) {
        String nome = normalizeAndValidateName(request == null ? null : request.getNome());
        OffsetDateTime dataCriacao = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        Carteira carteira = new Carteira(nome, dataCriacao);

        return mapper.toResponse(repository.saveAndFlush(carteira));
    }

    @Transactional(readOnly = true)
    public List<CarteiraResponse> listar() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CarteiraResponse buscarPorId(Long id) {
        Carteira carteira = repository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Carteira não encontrada para o id: " + id
                ));

        return mapper.toResponse(carteira);
    }

    @Transactional
    public CarteiraResponse atualizar(Long id, CarteiraUpdateRequest request) {
        Carteira carteira = repository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Carteira não encontrada para o id: " + id
                ));
        String nome = normalizeAndValidateName(request == null ? null : request.getNome());
        carteira.atualizarNome(nome);

        return mapper.toResponse(repository.saveAndFlush(carteira));
    }

    @Transactional
    public void excluir(Long id) {
        Carteira carteira = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Carteira não encontrada para o id: " + id
                ));

        if (operacaoRepository.existsByCarteiraId(id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.CARTEIRA_POSSUI_OPERACOES,
                    "Carteira possui operações e não pode ser excluída",
                    Map.of("carteiraId", id)
            );
        }

        repository.delete(carteira);
    }

    private String normalizeAndValidateName(String value) {
        if (value == null) {
            throw invalidName("Nome é obrigatório");
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw invalidName("Nome é obrigatório");
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw invalidName("Nome deve possuir no máximo 255 caracteres");
        }
        return normalized;
    }

    private ApiException invalidName(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.REQUEST_INVALIDO,
                "Dados da requisição inválidos",
                Map.of("nome", message)
        );
    }
}
