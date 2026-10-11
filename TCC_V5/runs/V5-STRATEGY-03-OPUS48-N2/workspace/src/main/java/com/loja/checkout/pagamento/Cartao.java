package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {

    private static final int SEM_JUROS_ATE = 3;
    private static final int MAXIMO_PARCELAS = 12;
    private static final double TAXA = 0.0199;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= SEM_JUROS_ATE) {
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, Dinheiro.MODO);
            return new ResultadoPagamento(totalPedido, parcela);
        }
        // Tabela Price: parcela = total * taxa / (1 - (1 + taxa)^-n)
        double fator = TAXA / (1 - Math.pow(1 + TAXA, -parcelas));
        BigDecimal parcela = Dinheiro.centavos(totalPedido.multiply(BigDecimal.valueOf(fator)));
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }
}
