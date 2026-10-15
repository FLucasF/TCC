package com.loja.pedidos.controller;

import com.loja.pedidos.dto.RespostaErro;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class ProdutoController {
    private final ProdutoService produtoService;

    @GetMapping("/{sku}")
    public ResponseEntity<?> obterProduto(@PathVariable String sku) {
        try {
            var produto = produtoService.obterProduto(sku);
            return ResponseEntity.ok(produto);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }
}
