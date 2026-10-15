package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Arredondamento;

public class PagamentoCartao implements EstrategiaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public boolean disponivel(BigDecimal total, int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public int parcelasPermitidas(BigDecimal total) {
        return 12;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal parcela = calcularParcela(total, parcelas);
        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
        return Arredondamento.arredondar(totalComJuros.subtract(total));
    }

    @Override
    public BigDecimal calcularParcela(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            return Arredondamento.arredondar(total.divide(new BigDecimal(parcelas), 2, java.math.RoundingMode.HALF_EVEN));
        }

        BigDecimal taxa = TAXA_JUROS;
        BigDecimal numerador = total.multiply(taxa);

        BigDecimal um = BigDecimal.ONE;
        BigDecimal um_mais_taxa = um.add(taxa);
        BigDecimal expoente = calcularExpoente(um_mais_taxa, parcelas);
        BigDecimal inv_expoente = um.divide(expoente, 30, java.math.RoundingMode.HALF_EVEN);
        BigDecimal denominador = um.subtract(inv_expoente);

        BigDecimal parcela = numerador.divide(denominador, 20, java.math.RoundingMode.HALF_EVEN);
        return Arredondamento.arredondar(parcela);
    }

    private BigDecimal calcularExpoente(BigDecimal base, int expoente) {
        BigDecimal resultado = BigDecimal.ONE;
        for (int i = 0; i < expoente; i++) {
            resultado = resultado.multiply(base);
        }
        return resultado;
    }

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.CARTAO;
    }
}
