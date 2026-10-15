package com.loja.checkout;

import com.loja.checkout.api.PedidoRecusado;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoRequest.ItemRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A ordem de conferência dos problemas: devolve sempre o primeiro. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class RecusasTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");

    @Autowired
    private CalculadoraResumo calculadora;

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private void recusa(String codigoEsperado, ResumoRequest request) {
        assertThatThrownBy(() -> calculadora.calcular(request))
                .isInstanceOf(PedidoRecusado.class)
                .extracting(e -> ((PedidoRecusado) e).codigo())
                .isEqualTo(codigoEsperado);
    }

    private ResumoRequest pedido(List<ItemRequest> itens, String entrega, String cupom,
                                 String pagamento, Integer parcelas, String clube, String regiao) {
        return new ResumoRequest(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
    }

    @Test
    void carrinhoVazioOuItemRuim() {
        recusa("PEDIDO_INVALIDO", pedido(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"));
        recusa("PEDIDO_INVALIDO", pedido(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"));
        recusa("PEDIDO_INVALIDO", pedido(List.of(item("X", "0.00", 1, "0.30")), "EXPRESSA", null,
                "PIX", 1, "BRONZE", "SUDESTE"));
        recusa("PEDIDO_INVALIDO", pedido(List.of(item("X", "10.00", 0, "0.30")), "EXPRESSA", null,
                "PIX", 1, "BRONZE", "SUDESTE"));
        recusa("PEDIDO_INVALIDO", pedido(List.of(item("X", "10.00", 1, "-0.30")), "EXPRESSA", null,
                "PIX", 1, "BRONZE", "SUDESTE"));
        recusa("PEDIDO_INVALIDO", pedido(
                Arrays.asList(new ItemRequest("X", null, 1, new BigDecimal("0.3"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"));
    }

    @Test
    void itemRuimVemAntesDeTodoOResto() {
        recusa("PEDIDO_INVALIDO", pedido(List.of(), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 9,
                "NAO_EXISTE", "NAO_EXISTE"));
    }

    @Test
    void nivelDoClubeAntesDaRegiao() {
        recusa("NIVEL_CLUBE_INVALIDO", pedido(List.of(CAMISETA), "NAO_EXISTE", null, "PIX", 1,
                "DIAMANTE", "NAO_EXISTE"));
        recusa("NIVEL_CLUBE_INVALIDO", pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1,
                null, "SUDESTE"));
    }

    @Test
    void regiaoAntesDaModalidade() {
        recusa("REGIAO_INVALIDA", pedido(List.of(CAMISETA), "NAO_EXISTE", null, "PIX", 1,
                "BRONZE", "NORDESTINA"));
        recusa("REGIAO_INVALIDA", pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1,
                "BRONZE", null));
    }

    @Test
    void modalidadeInexistenteAntesDoCupom() {
        recusa("MODALIDADE_INVALIDA", pedido(List.of(CAMISETA), "DRONE", "NAO_EXISTE", "PIX", 1,
                "BRONZE", "SUDESTE"));
        recusa("MODALIDADE_INVALIDA", pedido(List.of(CAMISETA), null, null, "PIX", 1,
                "BRONZE", "SUDESTE"));
    }

    @Test
    void motoboyAcimaDeCincoQuilos() {
        recusa("MODALIDADE_INDISPONIVEL", pedido(List.of(item("Halter", "99.90", 2, "2.6")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));
    }

    @Test
    void motoboyAceitaExatamenteCincoQuilos() {
        assertThat(calculadora.calcular(pedido(List.of(item("Halter", "99.90", 2, "2.5")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")).frete())
                .isEqualByComparingTo("18.00");
    }

    @Test
    void cupomInexistenteAntesDoPagamento() {
        recusa("CUPOM_INVALIDO", pedido(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "NAO_EXISTE", 1,
                "BRONZE", "SUDESTE"));
        recusa("CUPOM_INVALIDO", pedido(List.of(CAMISETA), "EXPRESSA", "PROMO_ANTIGA", "PIX", 1,
                "BRONZE", "SUDESTE"));
    }

    @Test
    void menos50AbaixoDeTrezentos() {
        recusa("CUPOM_NAO_APLICAVEL", pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1,
                "BRONZE", "SUDESTE"));
    }

    @Test
    void formaDePagamentoInexistente() {
        recusa("FORMA_PAGAMENTO_INVALIDA", pedido(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1,
                "BRONZE", "SUDESTE"));
        recusa("FORMA_PAGAMENTO_INVALIDA", pedido(List.of(CAMISETA), "EXPRESSA", null, null, 1,
                "BRONZE", "SUDESTE"));
    }

    @Test
    void parcelamentoNaoPermitido() {
        recusa("PARCELAMENTO_INVALIDO", pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2,
                "BRONZE", "SUDESTE"));
        recusa("PARCELAMENTO_INVALIDO", pedido(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3,
                "BRONZE", "SUDESTE"));
        recusa("PARCELAMENTO_INVALIDO", pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13,
                "BRONZE", "SUDESTE"));
        recusa("PARCELAMENTO_INVALIDO", pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0,
                "BRONZE", "SUDESTE"));
    }

    @Test
    void parcelamentoInvalidoAntesDeBoletoIndisponivel() {
        recusa("PARCELAMENTO_INVALIDO", pedido(List.of(item("Sofa", "2000.00", 1, "1.00")),
                "EXPRESSA", null, "BOLETO", 2, "BRONZE", "SUDESTE"));
    }

    @Test
    void boletoAcimaDeMil() {
        recusa("FORMA_PAGAMENTO_INDISPONIVEL", pedido(List.of(item("Sofa", "2000.00", 1, "1.00")),
                "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));
    }

    @Test
    void boletoAceitaExatamenteMil() {
        // 990,10 + seguro 1% (9,90) = 1.000,00 cravado, mais a tarifa de 3,49
        assertThat(calculadora.calcular(pedido(List.of(item("Mesa", "990.10", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")).totalFinal())
                .isEqualByComparingTo("1003.49");
    }

    @Test
    void menos50AceitaExatamenteTrezentos() {
        assertThat(calculadora.calcular(pedido(List.of(item("Jaqueta", "300.00", 1, "1.00")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")).descontoCupom())
                .isEqualByComparingTo("50.00");
    }

    @Test
    void modalidadeIndisponivelAntesDoCupomInexistente() {
        recusa("MODALIDADE_INDISPONIVEL", pedido(List.of(item("Halter", "99.90", 2, "2.6")),
                "MOTOBOY", "NAO_EXISTE", "PIX", 1, "BRONZE", "SUDESTE"));
    }

    @Test
    void cupomNaoAplicavelAntesDaFormaDePagamento() {
        recusa("CUPOM_NAO_APLICAVEL", pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "CRIPTO", 1,
                "BRONZE", "SUDESTE"));
    }
}
