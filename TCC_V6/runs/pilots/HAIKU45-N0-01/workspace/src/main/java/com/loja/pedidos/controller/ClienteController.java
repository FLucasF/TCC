package com.loja.pedidos.controller;

import com.loja.pedidos.dto.RespostaErro;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.service.AvisoService;
import com.loja.pedidos.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteService clienteService;
    private final AvisoService avisoService;

    @GetMapping("/{cpf}")
    public ResponseEntity<?> obterCliente(@PathVariable String cpf) {
        try {
            var cliente = clienteService.obterCliente(cpf);
            return ResponseEntity.ok(cliente);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }

    @GetMapping("/{cpf}/avisos")
    public ResponseEntity<?> obterAvisos(@PathVariable String cpf) {
        try {
            var avisos = avisoService.obterAvisos(cpf);
            return ResponseEntity.ok(avisos);
        } catch (Exception e) {
            return ResponseEntity
                .status(500)
                .body(new RespostaErro("ERRO_INTERNO"));
        }
    }
}
