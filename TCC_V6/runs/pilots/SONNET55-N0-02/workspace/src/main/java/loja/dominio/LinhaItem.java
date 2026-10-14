package loja.dominio;

import java.math.BigDecimal;

/** Item do carrinho já cruzado com o catálogo. */
public record LinhaItem(String sku, BigDecimal preco, BigDecimal pesoKg, int quantidade) {

    public BigDecimal total() {
        return Dinheiro.arredondar(preco.multiply(BigDecimal.valueOf(quantidade)));
    }
}
