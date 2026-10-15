package com.loja.checkout.pagamento;

import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final int LIMITE_SEM_JUROS = 3;
    private static final int MAXIMO_PARCELAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext CONTEXTO = new MathContext(20);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= LIMITE_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(BigDecimal.ZERO, totalPedido, parcelas, valorParcela);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal fatorDesconto = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas, CONTEXTO), CONTEXTO));
        BigDecimal parcelaExata = totalPedido.multiply(TAXA_MENSAL).divide(fatorDesconto, CONTEXTO);
        BigDecimal valorParcela = Arredondamento.centavos(parcelaExata);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, parcelas, valorParcela);
    }
}
