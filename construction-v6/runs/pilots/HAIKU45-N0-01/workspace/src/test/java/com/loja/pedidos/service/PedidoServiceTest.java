package com.loja.pedidos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.pedidos.dto.DadosCompra;
import com.loja.pedidos.dto.Resumo;
import com.loja.pedidos.entity.Pedido;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.repository.AvisoRepository;
import com.loja.pedidos.repository.ClienteRepository;
import com.loja.pedidos.repository.PedidoRepository;
import com.loja.pedidos.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AvisoRepository avisoRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private CalculoService calculoService;

    private PedidoService pedidoService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        pedidoService = new PedidoService(pedidoRepository, clienteRepository, avisoRepository, produtoRepository, calculoService, objectMapper);
    }

    @Test
    void testObterPedidoInexistenteRetornaErro() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ErroNegocio.class, () -> pedidoService.obterPedido(999L));
    }

    @Test
    void testCancelarPedidoNaoEnviadoComSucesso() {
        var pedido = Pedido.builder()
            .id(1L)
            .cpfCliente("12345678901")
            .situacao(Pedido.SituacaoPedido.AGUARDANDO_PAGAMENTO)
            .criadoEm(Instant.now())
            .itens(new ArrayList<>())
            .resumoJson("{}")
            .build();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenReturn(pedido);

        var resultado = pedidoService.cancelarPedido(1L);

        assertEquals("CANCELADO", resultado.getSituacao());
        verify(avisoRepository).save(any());
    }

    @Test
    void testCancelarPedidoEnviadoRetornaErro() {
        var pedido = Pedido.builder()
            .id(1L)
            .cpfCliente("12345678901")
            .situacao(Pedido.SituacaoPedido.ENVIADO)
            .criadoEm(Instant.now())
            .itens(new ArrayList<>())
            .resumoJson("{}")
            .build();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(ErroNegocio.class, () -> pedidoService.cancelarPedido(1L));
    }
}
