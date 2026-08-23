package com.projeto.resources;

import com.projeto.dto.CarteiraCreateRequest;
import com.projeto.dto.CarteiraResponse;
import com.projeto.dto.CarteiraUpdateRequest;
import com.projeto.dto.OperacaoResponse;
import com.projeto.services.CarteiraService;
import com.projeto.services.OperacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/carteiras")
public class CarteiraResource {

    private final CarteiraService service;
    private final OperacaoService operacaoService;

    public CarteiraResource(CarteiraService service, OperacaoService operacaoService) {
        this.service = service;
        this.operacaoService = operacaoService;
    }

    @PostMapping
    public ResponseEntity<CarteiraResponse> cadastrar(@Valid @RequestBody CarteiraCreateRequest request) {
        CarteiraResponse response = service.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CarteiraResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarteiraResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/{carteiraId}/operacoes")
    public ResponseEntity<List<OperacaoResponse>> listarOperacoes(
            @PathVariable Long carteiraId
    ) {
        return ResponseEntity.ok(operacaoService.listarPorCarteira(carteiraId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CarteiraResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CarteiraUpdateRequest request
    ) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
