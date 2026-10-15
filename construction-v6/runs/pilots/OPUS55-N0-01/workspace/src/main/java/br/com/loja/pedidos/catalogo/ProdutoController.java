package br.com.loja.pedidos.catalogo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.pedidos.comum.ErroNegocio;

@RestController
public class ProdutoController {

    private final ProdutoRepositorio produtos;

    public ProdutoController(ProdutoRepositorio produtos) {
        this.produtos = produtos;
    }

    @GetMapping("/produtos/{sku}")
    public Produto consultar(@PathVariable String sku) {
        return produtos.buscar(sku).orElseThrow(() -> ErroNegocio.naoEncontrado("PRODUTO_NAO_ENCONTRADO"));
    }
}
