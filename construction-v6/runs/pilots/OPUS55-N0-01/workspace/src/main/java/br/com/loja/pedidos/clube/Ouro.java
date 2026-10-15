package br.com.loja.pedidos.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.comum.Dinheiro;
import br.com.loja.pedidos.entrega.ModalidadeEntrega;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = Dinheiro.reais("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.porcentagem(subtotalProdutos, new BigDecimal("5"));
    }

    @Override
    public boolean isentaFrete(ModalidadeEntrega modalidade) {
        return modalidade.isentavelPeloClube();
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
