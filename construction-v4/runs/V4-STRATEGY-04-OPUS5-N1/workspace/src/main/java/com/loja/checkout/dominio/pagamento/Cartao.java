package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Centavos;
import java.math.BigDecimal;

/** Credito em ate 12x: ate 3x sem juros, de 4x a 12x com juros pela tabela Price. */
public final class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return new Cobranca(Centavos.arredondar(totalPedido), Centavos.dividir(totalPedido, parcelas));
        }
        BigDecimal parcela = parcelaPrice(totalPedido, parcelas);
        BigDecimal totalFinal = Centavos.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Cobranca(totalFinal, parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Centavos.CONTEXTO_INTERMEDIARIO);
        BigDecimal divisor = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(fator, Centavos.CONTEXTO_INTERMEDIARIO));
        return Centavos.arredondar(totalPedido.multiply(TAXA_MENSAL)
                .divide(divisor, Centavos.CONTEXTO_INTERMEDIARIO));
    }
}
