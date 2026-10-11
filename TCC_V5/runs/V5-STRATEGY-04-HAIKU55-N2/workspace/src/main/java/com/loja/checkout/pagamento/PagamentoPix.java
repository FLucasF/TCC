package com.loja.checkout.pagamento;

import static com.loja.checkout.calculo.Dinheiro.centavos;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PagamentoPix implements FormaPagamento {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public int parcelasMaximas() {
        return 1;
    }

    @Override
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = totalPedido.subtract(centavos(totalPedido.multiply(new BigDecimal("0.05"))));
        return new Pagamento(valorFinal, valorFinal, 1);
    }
}
