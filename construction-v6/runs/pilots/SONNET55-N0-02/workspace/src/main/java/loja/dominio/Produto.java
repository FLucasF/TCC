package loja.dominio;

import java.math.BigDecimal;

public record Produto(String sku, String nome, BigDecimal preco, BigDecimal pesoKg, int estoque, boolean foraDeLinha) {

    public Produto {
        preco = Dinheiro.arredondar(preco);
        BigDecimal peso = pesoKg.stripTrailingZeros();
        pesoKg = peso.scale() < 2 ? peso.setScale(2) : peso;
    }
}
