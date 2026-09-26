package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ganha 2% do valor dos produtos de volta, em credito para a proxima compra. */
@Component
public class NivelPrata implements NivelClube {

    private static final BigDecimal CASHBACK = Dinheiro.de("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CASHBACK);
    }
}
