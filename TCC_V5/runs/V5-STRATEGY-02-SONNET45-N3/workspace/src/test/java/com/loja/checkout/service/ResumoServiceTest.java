package com.loja.checkout.service;

import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    @Test
    void exemplo1() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg)
        // EXPRESSA, cupom BEMVINDO10, PIX, clube BRONZE, região NORTE
        // → subtotal 409,70 · cupom 40,97 · frete 33,10 · prazo 2 · seguro 10,24
        // · ajuste −20,60 · total final 391,47 · 1× de 391,47 · crédito 0,00 · brinde não

        List<ItemPedido> itens = List.of(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1,
            "BRONZE",
            "NORTE"
        );

        ResumoResponse resumo = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resumo.descontoCupom());
        assertEquals(new BigDecimal("33.10"), resumo.frete());
        assertEquals(2, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resumo.seguro());
        assertEquals(new BigDecimal("-20.60"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(new BigDecimal("391.47"), resumo.valorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo2() {
        // mesmos itens, ECONOMICA, sem cupom, CARTAO em 6×, clube PRATA, região CENTRO_OESTE
        // → subtotal 409,70 · cupom 0,00 · frete 15,60 · prazo 7 · seguro 6,15
        // · ajuste 30,55 · total final 462,00 · 6× de 77,00 · crédito 8,19 · brinde não

        List<ItemPedido> itens = List.of(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        ResumoResponse resumo = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("15.60"), resumo.frete());
        assertEquals(7, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resumo.seguro());
        assertEquals(new BigDecimal("30.55"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), resumo.totalFinal());
        assertEquals(6, resumo.parcelas());
        assertEquals(new BigDecimal("77.00"), resumo.valorParcela());
        assertEquals(new BigDecimal("8.19"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo3() {
        // Fone 199,90 × 2 (0,25 kg), MOTOBOY, cupom MENOS50, BOLETO, clube BRONZE, região NORDESTE
        // → subtotal 399,80 · cupom 50,00 · frete 18,00 · prazo 0 · seguro 8,00
        // · ajuste 3,49 · total final 379,29 · 1× de 379,29 · crédito 0,00 · brinde não

        List<ItemPedido> itens = List.of(
            new ItemPedido("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1,
            "BRONZE",
            "NORDESTE"
        );

        ResumoResponse resumo = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("18.00"), resumo.frete());
        assertEquals(0, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resumo.seguro());
        assertEquals(new BigDecimal("3.49"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(new BigDecimal("379.29"), resumo.valorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo4() {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg)
        // RETIRADA_LOJA, cupom LEVE3PAGUE2, CARTAO em 3×, clube PRATA, região SUL
        // → subtotal 299,10 · cupom 39,80 · frete 0,00 · prazo 1 · seguro 2,99
        // · ajuste 0,00 · total final 262,29 · 3× de 87,43 · crédito 5,98 · brinde não

        List<ItemPedido> itens = List.of(
            new ItemPedido("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        ResumoResponse resumo = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resumo.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.frete());
        assertEquals(1, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resumo.seguro());
        assertEquals(new BigDecimal("0.00"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), resumo.totalFinal());
        assertEquals(3, resumo.parcelas());
        assertEquals(new BigDecimal("87.43"), resumo.valorParcela());
        assertEquals(new BigDecimal("5.98"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo5() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg)
        // EXPRESSA, sem cupom, PIX, clube OURO, região SUDESTE
        // → subtotal 409,70 · cupom 0,00 · frete 0,00 (OURO não paga) · prazo 2 · seguro 4,10
        // · ajuste −20,69 · total final 393,11 · 1× de 393,11 · crédito 20,48 · brinde não

        List<ItemPedido> itens = List.of(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "OURO",
            "SUDESTE"
        );

        ResumoResponse resumo = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.frete()); // OURO não paga frete
        assertEquals(2, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resumo.seguro());
        assertEquals(new BigDecimal("-20.69"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(new BigDecimal("393.11"), resumo.valorParcela());
        assertEquals(new BigDecimal("20.48"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void cupomFreteGratis() {
        // Testando cupom FRETEGRATIS
        List<ItemPedido> itens = List.of(
            new ItemPedido("Camiseta", new BigDecimal("100.00"), 1, new BigDecimal("0.50"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            "FRETEGRATIS",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        ResumoResponse resumo = service.calcularResumo(request);

        // O desconto do cupom deve ser igual ao frete
        assertEquals(resumo.frete(), resumo.descontoCupom());
    }

    @Test
    void validacao1_PedidoInvalido_CarrinhoVazio() {
        PedidoRequest request = new PedidoRequest(
            List.of(),
            "ECONOMICA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigoErro());
    }

    @Test
    void validacao1_PedidoInvalido_PrecoZero() {
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item", BigDecimal.ZERO, 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigoErro());
    }

    @Test
    void validacao2_NivelClubeInvalido() {
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            1,
            "PLATINA",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", ex.getCodigoErro());
    }

    @Test
    void validacao3_RegiaoInvalida() {
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            1,
            "BRONZE",
            "EXTERIOR"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("REGIAO_INVALIDA", ex.getCodigoErro());
    }

    @Test
    void validacao4_ModalidadeInvalida() {
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "DRONE",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("MODALIDADE_INVALIDA", ex.getCodigoErro());
    }

    @Test
    void validacao5_ModalidadeIndisponivel() {
        // MOTOBOY só aceita até 5 kg
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item Pesado", new BigDecimal("100.00"), 1, new BigDecimal("6.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "MOTOBOY",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigoErro());
    }

    @Test
    void validacao6_CupomInvalido() {
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            "CUPOMINEXISTENTE",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("CUPOM_INVALIDO", ex.getCodigoErro());
    }

    @Test
    void validacao7_CupomNaoAplicavel() {
        // MENOS50 só vale para compras >= R$ 300
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            "MENOS50",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigoErro());
    }

    @Test
    void validacao8_FormaPagamentoInvalida() {
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "DINHEIRO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ex.getCodigoErro());
    }

    @Test
    void validacao9_ParcelamentoInvalido_PixMaisDeUmaParcela() {
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            3,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigoErro());
    }

    @Test
    void validacao9_ParcelamentoInvalido_CartaoMaisDe12Parcelas() {
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "CARTAO",
            15,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigoErro());
    }

    @Test
    void validacao10_FormaPagamentoIndisponivel() {
        // BOLETO não aceita valores acima de R$ 1.000
        List<ItemPedido> itens = List.of(
            new ItemPedido("Item Caro", new BigDecimal("1500.00"), 1, new BigDecimal("1.0"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "BOLETO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigoErro());
    }
}
