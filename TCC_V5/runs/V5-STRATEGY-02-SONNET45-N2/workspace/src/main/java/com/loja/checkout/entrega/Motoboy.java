package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.model.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("MOTOBOY")
public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal LIMITE_PESO = new BigDecimal("5.00");

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return TAXA_FIXA.setScale(2);
    }

    @Override
    public Integer prazoEntregaDias() {
        return 0;
    }

    @Override
    public void validarDisponibilidade(Pedido pedido) {
        if (pedido.pesoTotal().compareTo(LIMITE_PESO) > 0) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }
}
