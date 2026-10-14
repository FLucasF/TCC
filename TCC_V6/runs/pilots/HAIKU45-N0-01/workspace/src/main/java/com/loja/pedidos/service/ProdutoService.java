package com.loja.pedidos.service;

import com.loja.pedidos.dto.RespostaProduto;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProdutoService {
    private final ProdutoRepository produtoRepository;

    public RespostaProduto obterProduto(String sku) {
        var produto = produtoRepository.findById(sku)
            .orElseThrow(ErroNegocio::produtoNaoEncontrado);

        return RespostaProduto.builder()
            .sku(produto.getSku())
            .nome(produto.getNome())
            .preco(produto.getPreco())
            .pesoKg(produto.getPesoKg())
            .estoque(produto.getEstoque())
            .foraDeLinha(produto.isForaDeLinha())
            .build();
    }
}
