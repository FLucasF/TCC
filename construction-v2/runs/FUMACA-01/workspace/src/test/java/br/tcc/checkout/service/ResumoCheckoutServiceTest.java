package br.tcc.checkout.service;

import static org.junit.jupiter.api.Assertions.*;

import br.tcc.checkout.api.dto.ItemDto;
import br.tcc.checkout.api.dto.ResumoCheckoutRequest;
import br.tcc.checkout.api.dto.ResumoCheckoutResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoCheckoutServiceTest {
    private final ResumoCheckoutService service = new ResumoCheckoutService();

    @Test
    void exemplo1_CamisetaTenisExpressaBemvindo10Pix() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(
                new ItemDto("Camiseta", 79.90, 2, 0.30),
                new ItemDto("Tênis", 249.90, 1, 1.20)
            ),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1
        );

        ResumoCheckoutResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.subtotalProdutos(), 0.001);
        assertEquals(40.97, response.descontoCupom(), 0.001);
        assertEquals(33.10, response.frete(), 0.001);
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(-20.09, response.ajustePagamento(), 0.001);
        assertEquals(381.74, response.totalFinal(), 0.001);
        assertEquals(1, response.parcelas());
        assertEquals(381.74, response.valorParcela(), 0.001);
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaCartao6x() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(
                new ItemDto("Camiseta", 79.90, 2, 0.30),
                new ItemDto("Tênis", 249.90, 1, 1.20)
            ),
            "ECONOMICA",
            null,
            "CARTAO",
            6
        );

        ResumoCheckoutResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.subtotalProdutos(), 0.001);
        assertEquals(0.00, response.descontoCupom(), 0.001);
        assertEquals(15.60, response.frete(), 0.001);
        assertEquals(7, response.prazoEntregaDias());
        assertEquals(30.10, response.ajustePagamento(), 0.001);
        assertEquals(455.40, response.totalFinal(), 0.001);
        assertEquals(6, response.parcelas());
        assertEquals(75.90, response.valorParcela(), 0.001);
    }

    @Test
    void exemplo3_FoneMotoboySembug50Boleto() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Fone", 199.90, 2, 0.25)),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1
        );

        ResumoCheckoutResponse response = service.calcularResumo(request);

        assertEquals(399.80, response.subtotalProdutos(), 0.001);
        assertEquals(50.00, response.descontoCupom(), 0.001);
        assertEquals(18.00, response.frete(), 0.001);
        assertEquals(0, response.prazoEntregaDias());
        assertEquals(3.49, response.ajustePagamento(), 0.001);
        assertEquals(371.29, response.totalFinal(), 0.001);
        assertEquals(1, response.parcelas());
        assertEquals(371.29, response.valorParcela(), 0.001);
    }

    @Test
    void exemplo4_MeiaECamisetaRetiradaLojeLeve3Pague2Cartao3x() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(
                new ItemDto("Meia", 19.90, 7, 0.10),
                new ItemDto("Camiseta", 79.90, 2, 0.30)
            ),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3
        );

        ResumoCheckoutResponse response = service.calcularResumo(request);

        assertEquals(299.10, response.subtotalProdutos(), 0.001);
        assertEquals(39.80, response.descontoCupom(), 0.001);
        assertEquals(0.00, response.frete(), 0.001);
        assertEquals(1, response.prazoEntregaDias());
        assertEquals(0.00, response.ajustePagamento(), 0.001);
        assertEquals(259.30, response.totalFinal(), 0.001);
        assertEquals(3, response.parcelas());
        assertEquals(86.43, response.valorParcela(), 0.001);
    }
}
