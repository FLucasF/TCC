package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ClubeOuro implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal SUBTOTAL_PARA_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal frete(ModalidadeEntrega modalidade, Pedido pedido) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, TAXA_CREDITO);
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(SUBTOTAL_PARA_BRINDE) > 0;
    }
}
