package com.loja.checkout;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.dto.PedidoDto;
import com.loja.checkout.dto.ResumoDto;
import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static final ItemDto CAMISETA = new ItemDto("Camiseta", 79.90, 2, 0.30);
    private static final ItemDto TENIS = new ItemDto("Tênis", 249.90, 1, 1.20);

    // Exemplo 1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE
    @Test
    void exemplo1() {
        PedidoDto pedido = new PedidoDto(
                List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"
        );
        ResumoDto r = service.calcular(pedido);
        assertEquals(409.70, r.subtotalProdutos(), 0.001);
        assertEquals(40.97, r.descontoCupom(), 0.001);
        assertEquals(33.10, r.frete(), 0.001);
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(10.24, r.seguro(), 0.001);
        assertEquals(-20.60, r.ajustePagamento(), 0.001);
        assertEquals(391.47, r.totalFinal(), 0.001);
        assertEquals(1, r.parcelas());
        assertEquals(391.47, r.valorParcela(), 0.001);
        assertEquals(0.00, r.creditoProximaCompra(), 0.001);
        assertFalse(r.brinde());
    }

    // Exemplo 2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE
    @Test
    void exemplo2() {
        PedidoDto pedido = new PedidoDto(
                List.of(CAMISETA, TENIS),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );
        ResumoDto r = service.calcular(pedido);
        assertEquals(409.70, r.subtotalProdutos(), 0.001);
        assertEquals(0.00, r.descontoCupom(), 0.001);
        assertEquals(15.60, r.frete(), 0.001);
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(6.15, r.seguro(), 0.001);
        assertEquals(30.55, r.ajustePagamento(), 0.001);
        assertEquals(462.00, r.totalFinal(), 0.001);
        assertEquals(6, r.parcelas());
        assertEquals(77.00, r.valorParcela(), 0.001);
        assertEquals(8.19, r.creditoProximaCompra(), 0.001);
        assertFalse(r.brinde());
    }

    // Exemplo 3: Fone 199,90 x2, MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE
    @Test
    void exemplo3() {
        ItemDto fone = new ItemDto("Fone", 199.90, 2, 0.25);
        PedidoDto pedido = new PedidoDto(
                List.of(fone),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"
        );
        ResumoDto r = service.calcular(pedido);
        assertEquals(399.80, r.subtotalProdutos(), 0.001);
        assertEquals(50.00, r.descontoCupom(), 0.001);
        assertEquals(18.00, r.frete(), 0.001);
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(8.00, r.seguro(), 0.001);
        assertEquals(3.49, r.ajustePagamento(), 0.001);
        assertEquals(379.29, r.totalFinal(), 0.001);
        assertEquals(1, r.parcelas());
        assertEquals(379.29, r.valorParcela(), 0.001);
        assertEquals(0.00, r.creditoProximaCompra(), 0.001);
        assertFalse(r.brinde());
    }

    // Exemplo 4: Meia 19,90 x7 + Camiseta 79,90 x2, RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL
    @Test
    void exemplo4() {
        ItemDto meia = new ItemDto("Meia", 19.90, 7, 0.10);
        ItemDto camiseta = new ItemDto("Camiseta", 79.90, 2, 0.30);
        PedidoDto pedido = new PedidoDto(
                List.of(meia, camiseta),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );
        ResumoDto r = service.calcular(pedido);
        assertEquals(299.10, r.subtotalProdutos(), 0.001);
        assertEquals(39.80, r.descontoCupom(), 0.001);
        assertEquals(0.00, r.frete(), 0.001);
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(2.99, r.seguro(), 0.001);
        assertEquals(0.00, r.ajustePagamento(), 0.001);
        assertEquals(262.29, r.totalFinal(), 0.001);
        assertEquals(3, r.parcelas());
        assertEquals(87.43, r.valorParcela(), 0.001);
        assertEquals(5.98, r.creditoProximaCompra(), 0.001);
        assertFalse(r.brinde());
    }

    // Exemplo 5: mesmos itens, EXPRESSA, sem cupom, PIX, OURO, SUDESTE
    @Test
    void exemplo5() {
        PedidoDto pedido = new PedidoDto(
                List.of(CAMISETA, TENIS),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"
        );
        ResumoDto r = service.calcular(pedido);
        assertEquals(409.70, r.subtotalProdutos(), 0.001);
        assertEquals(0.00, r.descontoCupom(), 0.001);
        assertEquals(0.00, r.frete(), 0.001);
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(4.10, r.seguro(), 0.001);
        assertEquals(-20.69, r.ajustePagamento(), 0.001);
        assertEquals(393.11, r.totalFinal(), 0.001);
        assertEquals(1, r.parcelas());
        assertEquals(393.11, r.valorParcela(), 0.001);
        assertEquals(20.48, r.creditoProximaCompra(), 0.001);
        assertFalse(r.brinde());
    }

    // Erros
    @Test
    void erroCarrinhoVazio() {
        PedidoDto pedido = new PedidoDto(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroItemPrecoZero() {
        ItemDto item = new ItemDto("X", 0.0, 1, 0.5);
        PedidoDto pedido = new PedidoDto(List.of(item), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroNivelClubeInvalido() {
        PedidoDto pedido = new PedidoDto(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("NIVEL_CLUBE_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroRegiaoInvalida() {
        PedidoDto pedido = new PedidoDto(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", "EXTERIOR");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("REGIAO_INVALIDA", ex.getCodigo());
    }

    @Test
    void erroModalidadeInvalida() {
        PedidoDto pedido = new PedidoDto(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("MODALIDADE_INVALIDA", ex.getCodigo());
    }

    @Test
    void erroMotoboySobreCarregado() {
        ItemDto pesado = new ItemDto("Geladeira", 999.00, 1, 6.0);
        PedidoDto pedido = new PedidoDto(List.of(pesado), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void erroCupomInvalido() {
        PedidoDto pedido = new PedidoDto(List.of(CAMISETA), "EXPRESSA", "DESCONTO99", "PIX", 1, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("CUPOM_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroCupomMenos50SemMinimo() {
        ItemDto barato = new ItemDto("Boné", 50.00, 1, 0.2);
        PedidoDto pedido = new PedidoDto(List.of(barato), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigo());
    }

    @Test
    void erroFormaPagamentoInvalida() {
        PedidoDto pedido = new PedidoDto(List.of(CAMISETA), "EXPRESSA", null, "CRIPTOMOEDA", 1, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ex.getCodigo());
    }

    @Test
    void erroParcelamentoPixMaisDeUma() {
        PedidoDto pedido = new PedidoDto(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroParcelamentoCartaoAcima12() {
        PedidoDto pedido = new PedidoDto(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroBoletoAcima1000() {
        ItemDto caro = new ItemDto("TV", 1200.00, 1, 3.0);
        PedidoDto pedido = new PedidoDto(List.of(caro), "MOTOBOY", null, "BOLETO", 1, "BRONZE", "SUL");
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(pedido));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void freteGratisDescontoCupomIgualFrete() {
        PedidoDto pedido = new PedidoDto(
                List.of(CAMISETA, TENIS),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUL"
        );
        ResumoDto r = service.calcular(pedido);
        // Frete EXPRESSA: 25 + 4.5 * (0.6+1.2) = 25 + 8.1 = 33.10
        assertEquals(33.10, r.descontoCupom(), 0.001);
        assertEquals(0.00, r.frete(), 0.001);
    }

    @Test
    void ouronaoPageFrete() {
        PedidoDto pedido = new PedidoDto(
                List.of(CAMISETA, TENIS),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUL"
        );
        ResumoDto r = service.calcular(pedido);
        assertEquals(0.00, r.frete(), 0.001);
    }

    @Test
    void ouroBrindeSoProdutosAcima500() {
        ItemDto caro = new ItemDto("Sofá", 600.00, 1, 4.0);
        PedidoDto pedido = new PedidoDto(
                List.of(caro),
                "MOTOBOY", null, "PIX", 1, "OURO", "SUL"
        );
        ResumoDto r = service.calcular(pedido);
        assertTrue(r.brinde());
    }
}
