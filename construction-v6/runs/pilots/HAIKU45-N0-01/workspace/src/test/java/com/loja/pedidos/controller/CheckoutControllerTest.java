package com.loja.pedidos.controller;

import com.loja.pedidos.dto.Resumo;
import com.loja.pedidos.service.CalculoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {
    @Mock
    private CalculoService calculoService;

    @InjectMocks
    private CheckoutController checkoutController;

    @Test
    void testCalcularResumoComSucesso() {
        var resumo = Resumo.builder()
            .subtotalProdutos(new BigDecimal("159.80"))
            .descontoCupom(BigDecimal.ZERO)
            .frete(BigDecimal.ZERO)
            .prazoEntregaDias(1)
            .seguro(new BigDecimal("1.60"))
            .ajustePagamento(new BigDecimal("-7.99"))
            .totalFinal(new BigDecimal("152.41"))
            .parcelas(1)
            .valorParcela(new BigDecimal("152.41"))
            .creditoProximaCompra(BigDecimal.ZERO)
            .brinde(false)
            .build();

        when(calculoService.calcularResumo(any())).thenReturn(resumo);

        var response = checkoutController.calcularResumo(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(resumo, response.getBody());
    }
}
