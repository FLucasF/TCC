package br.com.loja.checkout.pagamento;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Confere o ajuste de cada forma de pagamento com os totais que o financeiro
 * usou nos exemplos do combinado.
 */
class FormasDePagamentoTest {

    private final Pix pix = new Pix();
    private final Boleto boleto = new Boleto();
    private final Cartao cartao = new Cartao();

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("Pix desconta 5% do total do pedido")
    void pix() {
        Cobranca cobranca = pix.cobrar(valor("401.83"), 1);

        assertThat(cobranca.valorFinal()).isEqualTo(valor("381.74"));
        assertThat(cobranca.valorParcela()).isEqualTo(valor("381.74"));
    }

    @Test
    @DisplayName("Boleto soma a tarifa de R$ 3,49")
    void boleto() {
        Cobranca cobranca = boleto.cobrar(valor("367.80"), 1);

        assertThat(cobranca.valorFinal()).isEqualTo(valor("371.29"));
    }

    @Test
    @DisplayName("Cartao em 3x nao tem juros: o valor final e o proprio total")
    void cartaoSemJuros() {
        Cobranca cobranca = cartao.cobrar(valor("259.30"), 3);

        assertThat(cobranca.valorFinal()).isEqualTo(valor("259.30"));
        assertThat(cobranca.valorParcela()).isEqualTo(valor("86.43"));
    }

    @Test
    @DisplayName("Cartao em 6x cobra juros pela tabela Price")
    void cartaoComJuros() {
        Cobranca cobranca = cartao.cobrar(valor("425.30"), 6);

        assertThat(cobranca.valorParcela()).isEqualTo(valor("75.90"));
        assertThat(cobranca.valorFinal()).isEqualTo(valor("455.40"));
    }

    @Test
    void pix_e_boleto_sao_sempre_a_vista() {
        assertThat(pix.permiteParcelas(1)).isTrue();
        assertThat(pix.permiteParcelas(2)).isFalse();
        assertThat(boleto.permiteParcelas(1)).isTrue();
        assertThat(boleto.permiteParcelas(3)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 11, 12})
    void cartao_parcela_de_1_a_12(int parcelas) {
        assertThat(cartao.permiteParcelas(parcelas)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 13, 24})
    void cartao_nao_parcela_fora_de_1_a_12(int parcelas) {
        assertThat(cartao.permiteParcelas(parcelas)).isFalse();
    }

    @Test
    void boleto_so_atende_ate_1000() {
        assertThat(boleto.atende(valor("1000.00"))).isTrue();
        assertThat(boleto.atende(valor("1000.01"))).isFalse();
        assertThat(pix.atende(valor("5000.00"))).isTrue();
        assertThat(cartao.atende(valor("5000.00"))).isTrue();
    }
}
