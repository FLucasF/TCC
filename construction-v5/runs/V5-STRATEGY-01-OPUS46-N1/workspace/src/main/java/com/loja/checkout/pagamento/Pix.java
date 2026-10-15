package com.loja.checkout.pagamento;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Pix implements FormaPagamento {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido, int parcelas) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Arredondamento.centavos(
                totalPedido.multiply(new BigDecimal("0.05"))
        );
        BigDecimal totalFinal = Arredondamento.centavos(totalPedido.subtract(desconto));
        return new ResultadoPagamento(totalFinal, totalFinal, 1);
    }
}
