package com.loja.checkout.pagamento;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean isParcelasValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Arredondamento.centavos(
                totalPedido.subtract(Arredondamento.centavos(totalPedido.multiply(DESCONTO))));
        return new ResultadoPagamento(totalFinal, totalFinal, 1);
    }
}
