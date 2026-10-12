package loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.Dinheiro;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
