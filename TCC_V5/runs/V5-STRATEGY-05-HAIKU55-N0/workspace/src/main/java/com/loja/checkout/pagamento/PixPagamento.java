package com.loja.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.checkout.calculo.Dinheiro;

@Component
public class PixPagamento implements FormaPagamento {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = totalPedido.subtract(Dinheiro.percentual(totalPedido, new BigDecimal("0.05")));
        return new Cobranca(valorFinal, valorFinal);
    }
}
