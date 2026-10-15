package com.loja.checkout.pagamento;

import com.loja.checkout.service.Arredondamento;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component("CARTAO")
public class CartaoPagamento implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO_INTERMEDIARIA = new MathContext(20, RoundingMode.HALF_EVEN);

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Arredondamento.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO_INTERMEDIARIA));
            return new ResultadoPagamento(totalPedido, valorParcela, Arredondamento.ZERO);
        }

        BigDecimal fatorJuros = BigDecimal.ONE.add(TAXA_JUROS_MENSAL).pow(parcelas, PRECISAO_INTERMEDIARIA);
        BigDecimal fatorDesconto = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fatorJuros, PRECISAO_INTERMEDIARIA));
        BigDecimal parcelaComJuros = totalPedido.multiply(TAXA_JUROS_MENSAL)
                .divide(fatorDesconto, PRECISAO_INTERMEDIARIA);
        BigDecimal valorParcela = Arredondamento.centavos(parcelaComJuros);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(totalFinal, valorParcela, ajuste);
    }
}
