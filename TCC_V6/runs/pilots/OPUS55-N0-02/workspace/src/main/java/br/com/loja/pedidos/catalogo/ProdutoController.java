package br.com.loja.pedidos.catalogo;

import br.com.loja.pedidos.comum.ErroNegocio;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProdutoController {

    private final ProdutoRepositorio produtos;

    public ProdutoController(ProdutoRepositorio produtos) {
        this.produtos = produtos;
    }

    @GetMapping("/produtos/{sku}")
    public ProdutoResposta consultar(@PathVariable String sku) {
        return produtos.buscar(sku)
                .map(ProdutoResposta::de)
                .orElseThrow(() -> ErroNegocio.naoEncontrado("PRODUTO_NAO_ENCONTRADO"));
    }

    public record ProdutoResposta(String sku, String nome, BigDecimal preco, BigDecimal pesoKg, int estoque,
                                  boolean foraDeLinha) {

        static ProdutoResposta de(Produto p) {
            BigDecimal peso = p.pesoKg().stripTrailingZeros();
            if (peso.scale() < 2) {
                peso = peso.setScale(2);
            }
            return new ProdutoResposta(p.sku(), p.nome(), p.preco(), peso, p.estoque(), p.foraDeLinha());
        }
    }
}
