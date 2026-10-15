package com.loja.pedidos.integration;

import com.loja.pedidos.PedidosApplication;
import com.loja.pedidos.domain.Cliente;
import com.loja.pedidos.domain.Pedido;
import com.loja.pedidos.domain.Produto;
import com.loja.pedidos.dto.CheckoutRequestDTO;
import com.loja.pedidos.dto.ItemDTO;
import com.loja.pedidos.dto.ClienteDTO;
import com.loja.pedidos.repository.ClienteRepository;
import com.loja.pedidos.repository.PedidoRepository;
import com.loja.pedidos.repository.ProdutoRepository;
import com.loja.pedidos.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = PedidosApplication.class)
@ActiveProfiles("test")
class PedidoIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    static {
        postgres.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
    }

    @Test
    void testCriarPedido() {
        CheckoutRequestDTO req = new CheckoutRequestDTO();
        ClienteDTO cliente = new ClienteDTO("12345678901", "test@example.com");
        ItemDTO item = new ItemDTO("CAM-001", 1);

        req.setCliente(cliente);
        req.setItens(Arrays.asList(item));
        req.setCep("01310100");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");
        req.setParcelas(1);
        req.setNivelClube("BRONZE");

        Pedido pedido = pedidoService.criarPedido(req);

        assertNotNull(pedido.getId());
        assertEquals("AGUARDANDO_PAGAMENTO", pedido.getSituacao());
        assertEquals(1, pedido.getItens().size());

        // Verify that the product has not had its stock reduced
        Produto produto = produtoRepository.findById("CAM-001").orElseThrow();
        assertEquals(50, produto.getEstoque());
    }

    @Test
    void testClienteEhCriadoAoRegistrarPedido() {
        CheckoutRequestDTO req = new CheckoutRequestDTO();
        ClienteDTO cliente = new ClienteDTO("98765432101", "novo@example.com");
        ItemDTO item = new ItemDTO("MEI-001", 2);

        req.setCliente(cliente);
        req.setItens(Arrays.asList(item));
        req.setCep("20040020");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("BOLETO");
        req.setParcelas(1);
        req.setNivelClube("BRONZE");

        Pedido pedido = pedidoService.criarPedido(req);

        Optional<Cliente> clienteOpt = clienteRepository.findByCpf("98765432101");
        assertTrue(clienteOpt.isPresent());
        assertEquals("novo@example.com", clienteOpt.get().getEmail());
    }
}
