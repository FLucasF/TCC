package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final BigDecimal UM = BigDecimal.ONE;

    public String codigo() { return "CARTAO"; }
    public boolean parcelamentoValido(int parcelas) { return parcelas >= 1 && parcelas <= 12; }
    public boolean atende(BigDecimal totalPedido) { return true; }

    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) return semJuros(totalPedido, parcelas);
        return comJuros(totalPedido, parcelas);
    }

    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(Dinheiro.ZERO, Dinheiro.emCentavos(totalPedido), parcela, parcelas);
    }

    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = UM.add(TAXA);
        BigDecimal fator = UM.subtract(UM.divide(umMaisTaxa.pow(parcelas, Dinheiro.MC), Dinheiro.MC));
        BigDecimal parcelaBruta = totalPedido.multiply(TAXA, Dinheiro.MC).divide(fator, Dinheiro.MC);
        BigDecimal parcela = Dinheiro.emCentavos(parcelaBruta);
        BigDecimal totalFinal = Dinheiro.emCentavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = Dinheiro.emCentavos(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(ajuste, totalFinal, parcela, parcelas);
    }
}
