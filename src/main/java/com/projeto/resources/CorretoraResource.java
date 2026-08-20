package com.projeto.resources;

import com.projeto.dto.CorretoraCreateRequest;
import com.projeto.dto.CorretoraResponse;
import com.projeto.services.CorretoraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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
}
