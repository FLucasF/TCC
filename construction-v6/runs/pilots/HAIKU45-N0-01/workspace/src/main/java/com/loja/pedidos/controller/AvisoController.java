package com.loja.pedidos.controller;

import com.loja.pedidos.dto.RespostaErro;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.service.AvisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/avisos")
@RequiredArgsConstructor
public class AvisoController {
    private final AvisoService avisoService;

    @PostMapping("/{id}/leitura")
    public ResponseEntity<?> marcarComoLido(@PathVariable Long id) {
        try {
            var aviso = avisoService.marcarComoLido(id);
            return ResponseEntity.ok(aviso);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }
}
