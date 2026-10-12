package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Pix implements CalculoPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.PIX;
    }

    @Override
    public boolean parcelaPermitida(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Arredondamento.paraCentavos(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
    }
}
