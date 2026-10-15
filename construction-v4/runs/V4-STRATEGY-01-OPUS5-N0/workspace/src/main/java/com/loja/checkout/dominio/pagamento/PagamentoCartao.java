package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Cartao de credito: ate 3x sem juros, de 4x a 12x com juros de 1,99% ao mes (tabela Price). */
@Component
public class PagamentoCartao implements FormaPagamento {

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
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Moeda.emCentavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), Moeda.PRECISAO_INTERNA));
            return new ResultadoPagamento(Moeda.emCentavos(totalPedido), parcelas, parcela);
        }
        BigDecimal parcela = parcelaPrice(totalPedido, parcelas);
        BigDecimal totalFinal = Moeda.emCentavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcelas, parcela);
    }

    /**
     * Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas).
     * Escrita como total x taxa x fator / (fator - 1), com fator = (1 + taxa)^parcelas,
     * para nao depender de potencia de expoente negativo.
     */
    private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Moeda.PRECISAO_INTERNA);
        BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL, Moeda.PRECISAO_INTERNA)
                .multiply(fator, Moeda.PRECISAO_INTERNA);
        BigDecimal denominador = fator.subtract(BigDecimal.ONE);
        return Moeda.emCentavos(numerador.divide(denominador, Moeda.PRECISAO_INTERNA));
    }
}
