package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.carrinho.Carrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Economica implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.centavos(TAXA_FIXA.add(POR_KG.multiply(carrinho.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
