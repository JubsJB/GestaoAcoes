package com.projeto.resources;

import com.projeto.dto.AcaoCreateRequest;
import com.projeto.dto.AcaoResponse;
import com.projeto.entities.Mercado;
import com.projeto.services.AcaoService;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/acoes")
public class AcaoResource {

    private final AcaoService service;

    public AcaoResource(AcaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AcaoResponse> cadastrar(@Valid @RequestBody AcaoCreateRequest request) {
        AcaoResponse response = service.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AcaoResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-ticker")
    public ResponseEntity<AcaoResponse> buscarPorTickerEMercado(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) Mercado mercado
    ) {
        return ResponseEntity.ok(service.buscarPorTickerEMercado(ticker, mercado));
    }

    @PatchMapping("/{id}/cotacao")
    public ResponseEntity<AcaoResponse> atualizarCotacao(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        if (hasBody(request)) {
            throw invalidBody();
        }

        return ResponseEntity.ok(service.atualizarCotacao(id));
    }

    private boolean hasBody(HttpServletRequest request) {
        try {
            return request.getInputStream().read() != -1;
        } catch (IOException exception) {
            throw invalidBody();
        }
    }

    private ApiException invalidBody() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.REQUEST_INVALIDO,
                "A atualização de cotação não aceita corpo de requisição"
        );
    }
}
