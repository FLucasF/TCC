package com.loja.checkout.pagamento;

import com.loja.checkout.service.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final double TAXA_MENSAL = 0.0199;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(totalPedido, valorParcela);
        }

        double fatorJuros = TAXA_MENSAL / (1 - Math.pow(1 + TAXA_MENSAL, -parcelas));
        BigDecimal valorParcela = Dinheiro.arredondar(
                totalPedido.multiply(BigDecimal.valueOf(fatorJuros)));
        BigDecimal valorFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(valorFinal, valorParcela);
    }
}
