package br.com.loja.checkout;

import static br.com.loja.checkout.PedidoDeTeste.CAMISETA;
import static br.com.loja.checkout.PedidoDeTeste.item;
import static br.com.loja.checkout.PedidoDeTeste.pedido;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.loja.checkout.comum.CodigoErro;
import br.com.loja.checkout.comum.PedidoRecusadoException;
import br.com.loja.checkout.pedido.ItemBruto;
import br.com.loja.checkout.pedido.PedidoBruto;
import br.com.loja.checkout.resumo.CalculadoraResumo;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PedidoRecusadoTest {

    @Autowired
    private CalculadoraResumo calculadora;

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("pedidosRecusados")
    void recusa(String caso, PedidoBruto pedido, CodigoErro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(excecao -> ((PedidoRecusadoException) excecao).codigo())
                .isEqualTo(esperado);
    }

    static Stream<Arguments> pedidosRecusados() {
        return Stream.of(
                caso("carrinho vazio", pedido().montar(), CodigoErro.PEDIDO_INVALIDO),
                caso("itens ausentes",
                        new PedidoBruto(null, "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                        CodigoErro.PEDIDO_INVALIDO),
                caso("preco zero", pedido(item("Meia", "0.00", 1, "0.10")).montar(),
                        CodigoErro.PEDIDO_INVALIDO),
                caso("preco ausente", pedido(item("Meia", null, 1, "0.10")).montar(),
                        CodigoErro.PEDIDO_INVALIDO),
                caso("quantidade negativa", pedido(item("Meia", "19.90", -1, "0.10")).montar(),
                        CodigoErro.PEDIDO_INVALIDO),
                caso("quantidade ausente", pedido(item("Meia", "19.90", null, "0.10")).montar(),
                        CodigoErro.PEDIDO_INVALIDO),
                caso("peso zero", pedido(item("Meia", "19.90", 1, "0.00")).montar(),
                        CodigoErro.PEDIDO_INVALIDO),
                caso("item nulo na lista",
                        new PedidoBruto(Arrays.asList((ItemBruto) null), "ECONOMICA", null, "PIX",
                                1, "BRONZE", "SUDESTE"),
                        CodigoErro.PEDIDO_INVALIDO),
                caso("carrinho vazio na frente do nivel invalido",
                        pedido().clube("DIAMANTE").montar(), CodigoErro.PEDIDO_INVALIDO),

                caso("nivel que nao existe", pedido(CAMISETA).clube("DIAMANTE").montar(),
                        CodigoErro.NIVEL_CLUBE_INVALIDO),
                caso("nivel ausente", pedido(CAMISETA).clube(null).montar(),
                        CodigoErro.NIVEL_CLUBE_INVALIDO),
                caso("nivel na frente da regiao",
                        pedido(CAMISETA).clube("DIAMANTE").regiao("LESTE").montar(),
                        CodigoErro.NIVEL_CLUBE_INVALIDO),

                caso("regiao que nao existe", pedido(CAMISETA).regiao("LESTE").montar(),
                        CodigoErro.REGIAO_INVALIDA),
                caso("regiao ausente", pedido(CAMISETA).regiao(null).montar(),
                        CodigoErro.REGIAO_INVALIDA),
                caso("regiao na frente da modalidade",
                        pedido(CAMISETA).regiao("LESTE").entrega("DRONE").montar(),
                        CodigoErro.REGIAO_INVALIDA),

                caso("modalidade que nao existe", pedido(CAMISETA).entrega("DRONE").montar(),
                        CodigoErro.MODALIDADE_INVALIDA),
                caso("modalidade ausente", pedido(CAMISETA).entrega(null).montar(),
                        CodigoErro.MODALIDADE_INVALIDA),
                caso("modalidade na frente do cupom",
                        pedido(CAMISETA).entrega("DRONE").cupom("XPTO").montar(),
                        CodigoErro.MODALIDADE_INVALIDA),

                caso("motoboy acima de 5 kg",
                        pedido(item("Jaqueta", "100.00", 3, "1.70")).entrega("MOTOBOY").montar(),
                        CodigoErro.MODALIDADE_INDISPONIVEL),
                caso("modalidade indisponivel na frente do cupom",
                        pedido(item("Jaqueta", "100.00", 3, "1.70")).entrega("MOTOBOY")
                                .cupom("XPTO").montar(),
                        CodigoErro.MODALIDADE_INDISPONIVEL),

                caso("cupom que nao existe", pedido(CAMISETA).cupom("XPTO").montar(),
                        CodigoErro.CUPOM_INVALIDO),
                caso("cupom em minusculas", pedido(CAMISETA).cupom("bemvindo10").montar(),
                        CodigoErro.CUPOM_INVALIDO),
                caso("cupom na frente da forma de pagamento",
                        pedido(CAMISETA).cupom("XPTO").pagamento("CRIPTO").montar(),
                        CodigoErro.CUPOM_INVALIDO),

                caso("menos50 abaixo de 300", pedido(CAMISETA).cupom("MENOS50").montar(),
                        CodigoErro.CUPOM_NAO_APLICAVEL),
                caso("cupom nao aplicavel na frente da forma de pagamento",
                        pedido(CAMISETA).cupom("MENOS50").pagamento("CRIPTO").montar(),
                        CodigoErro.CUPOM_NAO_APLICAVEL),

                caso("forma de pagamento que nao existe",
                        pedido(CAMISETA).pagamento("CRIPTO").montar(),
                        CodigoErro.FORMA_PAGAMENTO_INVALIDA),
                caso("forma de pagamento ausente", pedido(CAMISETA).pagamento(null).montar(),
                        CodigoErro.FORMA_PAGAMENTO_INVALIDA),
                caso("forma de pagamento na frente do parcelamento",
                        pedido(CAMISETA).pagamento("CRIPTO").parcelas(99).montar(),
                        CodigoErro.FORMA_PAGAMENTO_INVALIDA),

                caso("pix parcelado", pedido(CAMISETA).pagamento("PIX").parcelas(2).montar(),
                        CodigoErro.PARCELAMENTO_INVALIDO),
                caso("boleto parcelado", pedido(CAMISETA).pagamento("BOLETO").parcelas(2).montar(),
                        CodigoErro.PARCELAMENTO_INVALIDO),
                caso("cartao em 13 vezes", pedido(CAMISETA).pagamento("CARTAO").parcelas(13).montar(),
                        CodigoErro.PARCELAMENTO_INVALIDO),
                caso("cartao em zero vezes", pedido(CAMISETA).pagamento("CARTAO").parcelas(0).montar(),
                        CodigoErro.PARCELAMENTO_INVALIDO),
                caso("parcelamento na frente do boleto acima de mil",
                        pedido(item("Casaco", "1200.00", 1, "0.80")).entrega("RETIRADA_LOJA")
                                .pagamento("BOLETO").parcelas(2).montar(),
                        CodigoErro.PARCELAMENTO_INVALIDO),

                caso("boleto acima de mil",
                        pedido(item("Casaco", "1200.00", 1, "0.80")).entrega("RETIRADA_LOJA")
                                .pagamento("BOLETO").montar(),
                        CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL));
    }

    private static Arguments caso(String nome, PedidoBruto pedido, CodigoErro esperado) {
        return Arguments.of(nome, pedido, esperado);
    }
}
