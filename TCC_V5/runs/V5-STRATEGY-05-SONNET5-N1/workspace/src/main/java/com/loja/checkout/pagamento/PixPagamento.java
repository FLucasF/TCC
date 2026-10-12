package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixPagamento implements FormaPagamentoCalculadora {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public FormaPagamento getTipo() {
        return FormaPagamento.PIX;
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal ajuste = Arredondamento.monetario(totalPedido.multiply(PERCENTUAL_DESCONTO).negate());
        BigDecimal totalFinal = Arredondamento.monetario(totalPedido.add(ajuste));
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
