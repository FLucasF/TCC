package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Pix implements AVista {

    private static final BigDecimal ALIQUOTA_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(totalPedido, ALIQUOTA_DESCONTO);
        BigDecimal valorFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
        return new Cobranca(valorFinal, parcelas, valorFinal);
    }
}
