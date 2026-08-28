package com.projeto.resources;

import com.projeto.dto.CorretoraCreateRequest;
import com.projeto.dto.CorretoraResponse;
import com.projeto.services.CorretoraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/corretoras")
public class CorretoraResource {

    private final CorretoraService service;

    public CorretoraResource(CorretoraService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CorretoraResponse> cadastrar(@Valid @RequestBody CorretoraCreateRequest request) {
        CorretoraResponse response = service.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CorretoraResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CorretoraResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-cnpj")
    public ResponseEntity<CorretoraResponse> buscarPorCnpj(
            @RequestParam(required = false) String cnpj
    ) {
        return ResponseEntity.ok(service.buscarPorCnpj(cnpj));
    }
}
