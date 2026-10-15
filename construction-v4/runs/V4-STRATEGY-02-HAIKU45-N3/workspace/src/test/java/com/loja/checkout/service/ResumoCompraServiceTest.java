package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.ErroCheckout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Resumo de Compra Service")
public class ResumoCompraServiceTest {

    private ResumoCompraService service;

    @BeforeEach
    void setUp() {
        service = new ResumoCompraService();
    }

    @Test
    @DisplayName("Exemplo 1: Camiseta + Tênis, EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE")
    void exemplo1() throws ErroCheckout {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        );
        ResumoRequest request = new ResumoRequest(
            itens, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"
        );

        ResumoResponse response = service.calcular(request);

        assertEquals(409.70, response.subtotalProdutos(), 0.01);
        assertEquals(40.97, response.descontoCupom(), 0.01);
        assertEquals(33.10, response.frete(), 0.01);
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(10.24, response.seguro(), 0.01);
        assertEquals(-20.60, response.ajustePagamento(), 0.01);
        assertEquals(391.47, response.totalFinal(), 0.01);
        assertEquals(1, response.parcelas());
        assertEquals(391.47, response.valorParcela(), 0.01);
        assertEquals(0.00, response.creditoProximaCompra(), 0.01);
        assertFalse(response.brinde());
    }

    @Test
    @DisplayName("Exemplo 2: Camiseta + Tênis, ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE")
    void exemplo2() throws ErroCheckout {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        );
        ResumoRequest request = new ResumoRequest(
            itens, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );

        ResumoResponse response = service.calcular(request);

        assertEquals(409.70, response.subtotalProdutos(), 0.01);
        assertEquals(0.00, response.descontoCupom(), 0.01);
        assertEquals(15.60, response.frete(), 0.01);
        assertEquals(7, response.prazoEntregaDias());
        assertEquals(6.15, response.seguro(), 0.01);
        assertEquals(30.55, response.ajustePagamento(), 0.01);
        assertEquals(462.00, response.totalFinal(), 0.01);
        assertEquals(6, response.parcelas());
        assertEquals(77.00, response.valorParcela(), 0.01);
        assertEquals(8.19, response.creditoProximaCompra(), 0.01);
        assertFalse(response.brinde());
    }

    @Test
    @DisplayName("Exemplo 3: Fone 2x, MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE")
    void exemplo3() throws ErroCheckout {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Fone", 199.90, 2, 0.25)
        );
        ResumoRequest request = new ResumoRequest(
            itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"
        );

        ResumoResponse response = service.calcular(request);

        assertEquals(399.80, response.subtotalProdutos(), 0.01);
        assertEquals(50.00, response.descontoCupom(), 0.01);
        assertEquals(18.00, response.frete(), 0.01);
        assertEquals(0, response.prazoEntregaDias());
        assertEquals(8.00, response.seguro(), 0.01);
        assertEquals(3.49, response.ajustePagamento(), 0.01);
        assertEquals(379.29, response.totalFinal(), 0.01);
        assertEquals(1, response.parcelas());
        assertEquals(379.29, response.valorParcela(), 0.01);
        assertEquals(0.00, response.creditoProximaCompra(), 0.01);
        assertFalse(response.brinde());
    }

    @Test
    @DisplayName("Exemplo 4: Meia 7x + Camiseta 2x, RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL")
    void exemplo4() throws ErroCheckout {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Meia", 19.90, 7, 0.10),
            new ItemRequest("Camiseta", 79.90, 2, 0.30)
        );
        ResumoRequest request = new ResumoRequest(
            itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );

        ResumoResponse response = service.calcular(request);

        assertEquals(299.10, response.subtotalProdutos(), 0.01);
        assertEquals(39.80, response.descontoCupom(), 0.01);
        assertEquals(0.00, response.frete(), 0.01);
        assertEquals(1, response.prazoEntregaDias());
        assertEquals(2.99, response.seguro(), 0.01);
        assertEquals(0.00, response.ajustePagamento(), 0.01);
        assertEquals(262.29, response.totalFinal(), 0.01);
        assertEquals(3, response.parcelas());
        assertEquals(87.43, response.valorParcela(), 0.01);
        assertEquals(5.98, response.creditoProximaCompra(), 0.01);
        assertFalse(response.brinde());
    }

    @Test
    @DisplayName("Exemplo 5: Camiseta + Tênis, EXPRESSA, sem cupom, PIX, OURO, SUDESTE")
    void exemplo5() throws ErroCheckout {
        List<ItemRequest> itens = List.of(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        );
        ResumoRequest request = new ResumoRequest(
            itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"
        );

        ResumoResponse response = service.calcular(request);

        assertEquals(409.70, response.subtotalProdutos(), 0.01);
        assertEquals(0.00, response.descontoCupom(), 0.01);
        assertEquals(0.00, response.frete(), 0.01);
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(4.10, response.seguro(), 0.01);
        assertEquals(-20.69, response.ajustePagamento(), 0.01);
        assertEquals(393.11, response.totalFinal(), 0.01);
        assertEquals(1, response.parcelas());
        assertEquals(393.11, response.valorParcela(), 0.01);
        assertEquals(20.48, response.creditoProximaCompra(), 0.01);
        assertFalse(response.brinde());
    }

    @Test
    @DisplayName("Validação: Carrinho vazio")
    void erroCarrinhoVazio() {
        ResumoRequest request = new ResumoRequest(
            List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Validação: Nível de clube inválido")
    void erroNivelClubeInvalido() {
        List<ItemRequest> itens = List.of(new ItemRequest("Item", 10.00, 1, 0.1));
        ResumoRequest request = new ResumoRequest(
            itens, "EXPRESSA", null, "PIX", 1, "INVALIDO", "SUDESTE"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    @DisplayName("Validação: Região inválida")
    void erroRegiaoInvalida() {
        List<ItemRequest> itens = List.of(new ItemRequest("Item", 10.00, 1, 0.1));
        ResumoRequest request = new ResumoRequest(
            itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "INVALIDA"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "REGIAO_INVALIDA");
    }

    @Test
    @DisplayName("Validação: Modalidade inválida")
    void erroModalidadeInvalida() {
        List<ItemRequest> itens = List.of(new ItemRequest("Item", 10.00, 1, 0.1));
        ResumoRequest request = new ResumoRequest(
            itens, "INVALIDA", null, "PIX", 1, "BRONZE", "SUDESTE"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("Validação: Cupom inválido")
    void erroCupomInvalido() {
        List<ItemRequest> itens = List.of(new ItemRequest("Item", 10.00, 1, 0.1));
        ResumoRequest request = new ResumoRequest(
            itens, "EXPRESSA", "INVALIDO", "PIX", 1, "BRONZE", "SUDESTE"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("Validação: Cupom não aplicável (MENOS50 abaixo de R$ 300)")
    void erroCupomNaoAplicavel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Item", 10.00, 1, 0.1));
        ResumoRequest request = new ResumoRequest(
            itens, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("Validação: Forma de pagamento inválida")
    void erroFormaPagamentoInvalida() {
        List<ItemRequest> itens = List.of(new ItemRequest("Item", 10.00, 1, 0.1));
        ResumoRequest request = new ResumoRequest(
            itens, "EXPRESSA", null, "INVALIDA", 1, "BRONZE", "SUDESTE"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    @DisplayName("Validação: Parcelamento inválido (Pix não parcela)")
    void erroParcelamentoPix() {
        List<ItemRequest> itens = List.of(new ItemRequest("Item", 10.00, 1, 0.1));
        ResumoRequest request = new ResumoRequest(
            itens, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("Validação: Modalidade indisponível (Motoboy acima de 5kg)")
    void erroModalidadeIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Item", 10.00, 1, 6.0));
        ResumoRequest request = new ResumoRequest(
            itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    @DisplayName("Validação: Forma de pagamento indisponível (Boleto acima de R$ 1000)")
    void erroFormaPagamentoIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Item", 500.50, 3, 0.1));
        ResumoRequest request = new ResumoRequest(
            itens, "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE"
        );
        assertThrows(ErroCheckout.class, () -> service.calcular(request), "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
