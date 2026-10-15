package com.loja.pedidos.controller;

import com.loja.pedidos.domain.Produto;
import com.loja.pedidos.dto.ProdutoDTO;
import com.loja.pedidos.exception.PedidoException;
import com.loja.pedidos.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {
    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping("/{sku}")
    public ResponseEntity<ProdutoDTO> obterProduto(@PathVariable String sku) {
        Produto produto = produtoRepository.findById(sku)
            .orElseThrow(() -> new PedidoException("PRODUTO_NAO_ENCONTRADO", "Product not found", HttpStatus.NOT_FOUND));

        ProdutoDTO dto = new ProdutoDTO(
            produto.getSku(),
            produto.getNome(),
            produto.getPreco(),
            produto.getPesoKg(),
            produto.getEstoque(),
            produto.isForaDeLinha()
        );

        return ResponseEntity.ok(dto);
    }
}
