package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 18,00; prazo 0 dias (mesmo dia); so leva pedidos de ate 5 kg. */
@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(VALOR);
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoKg) {
        if (pesoKg.compareTo(PESO_MAXIMO) > 0) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }
    }
}
