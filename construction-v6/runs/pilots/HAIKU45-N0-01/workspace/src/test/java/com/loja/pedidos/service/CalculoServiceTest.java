package com.loja.pedidos.service;

import com.loja.pedidos.client.TransLogClient;
import com.loja.pedidos.dto.DadosCompra;
import com.loja.pedidos.entity.Produto;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.repository.PedidoRepository;
import com.loja.pedidos.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculoServiceTest {
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private TransLogClient transLogClient;

    private CalculoService calculoService;

    @BeforeEach
    void setUp() {
        calculoService = new CalculoService(produtoRepository, pedidoRepository, transLogClient);
    }

    private DadosCompra criarDadosCompraBasicos() {
        return DadosCompra.builder()
            .cliente(new DadosCompra.Cliente("12345678901", "teste@exemplo.com"))
            .itens(Arrays.asList(
                new DadosCompra.Item("CAM-001", 2)
            ))
            .cep("01310100")
            .regiao(DadosCompra.Regiao.SUDESTE)
            .modalidadeEntrega(DadosCompra.ModalidadeEntrega.RETIRADA_LOJA)
            .formaPagamento(DadosCompra.FormaPagamento.PIX)
            .parcelas(1)
            .nivelClube(DadosCompra.NivelClube.BRONZE)
            .build();
    }

    @Test
    void testCarrinhoVazioLancaErro() {
        var dados = criarDadosCompraBasicos();
        dados.setItens(Arrays.asList());

        assertThrows(ErroNegocio.class, () -> calculoService.calcularResumo(dados));
    }

    @Test
    void testProdutoInexistenteLancaErro() {
        when(produtoRepository.findById("INVALIDO")).thenReturn(Optional.empty());

        var dados = criarDadosCompraBasicos();
        dados.setItens(Arrays.asList(new DadosCompra.Item("INVALIDO", 1)));

        assertThrows(ErroNegocio.class, () -> calculoService.calcularResumo(dados));
    }

    @Test
    void testCalculoSubtotal() {
        var produto = Produto.builder()
            .sku("CAM-001")
            .nome("Camiseta")
            .preco(new BigDecimal("79.90"))
            .pesoKg(new BigDecimal("0.30"))
            .estoque(50)
            .foraDeLinha(false)
            .build();

        when(produtoRepository.findById("CAM-001")).thenReturn(Optional.of(produto));

        var dados = criarDadosCompraBasicos();

        var resumo = calculoService.calcularResumo(dados);

        assertEquals(new BigDecimal("159.80"), resumo.getSubtotalProdutos());
    }

    @Test
    void testFreteRetiradaLoja() {
        var produto = Produto.builder()
            .sku("CAM-001")
            .nome("Camiseta")
            .preco(new BigDecimal("79.90"))
            .pesoKg(new BigDecimal("0.30"))
            .estoque(50)
            .foraDeLinha(false)
            .build();

        when(produtoRepository.findById("CAM-001")).thenReturn(Optional.of(produto));

        var dados = criarDadosCompraBasicos();
        dados.setModalidadeEntrega(DadosCompra.ModalidadeEntrega.RETIRADA_LOJA);

        var resumo = calculoService.calcularResumo(dados);

        assertEquals(new BigDecimal("0.00"), resumo.getFrete());
    }

    @Test
    void testCupomBemVindo10() {
        var produto = Produto.builder()
            .sku("CAM-001")
            .nome("Camiseta")
            .preco(new BigDecimal("79.90"))
            .pesoKg(new BigDecimal("0.30"))
            .estoque(50)
            .foraDeLinha(false)
            .build();

        when(produtoRepository.findById("CAM-001")).thenReturn(Optional.of(produto));
        when(pedidoRepository.findByCpfCliente("12345678901")).thenReturn(Arrays.asList());

        var dados = criarDadosCompraBasicos();
        dados.setCupom("BEMVINDO10");

        var resumo = calculoService.calcularResumo(dados);

        assertEquals(new BigDecimal("15.98"), resumo.getDescontoCupom());
    }

    @Test
    void testArredondamento() {
        var produto = Produto.builder()
            .sku("CAM-001")
            .nome("Camiseta")
            .preco(new BigDecimal("79.99"))
            .pesoKg(new BigDecimal("0.30"))
            .estoque(50)
            .foraDeLinha(false)
            .build();

        when(produtoRepository.findById("CAM-001")).thenReturn(Optional.of(produto));

        var dados = criarDadosCompraBasicos();

        var resumo = calculoService.calcularResumo(dados);

        assertEquals(new BigDecimal("159.98"), resumo.getSubtotalProdutos());
    }
}
