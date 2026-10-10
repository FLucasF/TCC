package com.loja.resumo.controller;

import com.loja.resumo.dto.ResumoRequest;
import com.loja.resumo.dto.ResumoResponse;
import com.loja.resumo.service.ResumoCompraService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class ResumoController {

    private final ResumoCompraService service;

    public ResumoController(ResumoCompraService service) {
        this.service = service;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return service.calcular(request);
    }
}
