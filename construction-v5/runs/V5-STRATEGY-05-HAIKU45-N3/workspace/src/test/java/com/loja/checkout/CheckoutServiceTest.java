package com.loja.checkout;

import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ErrorResponse;
import com.loja.checkout.model.Item;
import com.loja.checkout.service.CheckoutService;
import com.loja.checkout.service.RoundingService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
  private CheckoutService service;
  private RoundingService rounding;

  @BeforeEach
  void setup() {
    rounding = new RoundingService();
    service = new CheckoutService(rounding);
  }

  private CheckoutRequest criarRequest(List<Item> itens, String modalidade, String cupom,
      String forma, Integer parcelas, String nivel, String regiao) {
    CheckoutRequest req = new CheckoutRequest();
    req.itens = itens;
    req.modalidadeEntrega = modalidade;
    req.cupom = cupom;
    req.formaPagamento = forma;
    req.parcelas = parcelas;
    req.nivelClube = nivel;
    req.regiao = regiao;
    return req;
  }

  private Item criarItem(String nome, double preco, int qty, double peso) {
    Item item = new Item();
    item.nome = nome;
    item.precoUnitario = preco;
    item.quantidade = qty;
    item.pesoKg = peso;
    return item;
  }

  @Test
  void exemplo1() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 2, 0.30));
    itens.add(criarItem("Tênis", 249.90, 1, 1.20));

    CheckoutRequest req = criarRequest(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof CheckoutResponse);
    CheckoutResponse res = (CheckoutResponse) resp;

    assertEquals(409.70, res.subtotalProdutos, 0.01);
    assertEquals(40.97, res.descontoCupom, 0.01);
    assertEquals(33.10, res.frete, 0.01);
    assertEquals(2, res.prazoEntregaDias);
    assertEquals(10.24, res.seguro, 0.01);
    assertEquals(-20.60, res.ajustePagamento, 0.01);
    assertEquals(391.47, res.totalFinal, 0.01);
    assertEquals(1, res.parcelas);
    assertEquals(391.47, res.valorParcela, 0.01);
    assertEquals(0.0, res.creditoProximaCompra, 0.01);
    assertFalse(res.brinde);
  }

  @Test
  void exemplo2() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 2, 0.30));
    itens.add(criarItem("Tênis", 249.90, 1, 1.20));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof CheckoutResponse);
    CheckoutResponse res = (CheckoutResponse) resp;

    assertEquals(409.70, res.subtotalProdutos, 0.01);
    assertEquals(0.0, res.descontoCupom, 0.01);
    assertEquals(15.60, res.frete, 0.01);
    assertEquals(7, res.prazoEntregaDias);
    assertEquals(6.15, res.seguro, 0.01);
    assertEquals(30.55, res.ajustePagamento, 0.02);
    assertEquals(462.00, res.totalFinal, 0.02);
    assertEquals(6, res.parcelas);
    assertEquals(77.00, res.valorParcela, 0.01);
    assertEquals(8.19, res.creditoProximaCompra, 0.01);
    assertFalse(res.brinde);
  }

  @Test
  void exemplo3() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Fone", 199.90, 2, 0.25));

    CheckoutRequest req = criarRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof CheckoutResponse);
    CheckoutResponse res = (CheckoutResponse) resp;

    assertEquals(399.80, res.subtotalProdutos, 0.01);
    assertEquals(50.0, res.descontoCupom, 0.01);
    assertEquals(18.0, res.frete, 0.01);
    assertEquals(0, res.prazoEntregaDias);
    assertEquals(8.0, res.seguro, 0.01);
    assertEquals(3.49, res.ajustePagamento, 0.01);
    assertEquals(379.29, res.totalFinal, 0.01);
    assertEquals(1, res.parcelas);
    assertEquals(379.29, res.valorParcela, 0.01);
    assertEquals(0.0, res.creditoProximaCompra, 0.01);
    assertFalse(res.brinde);
  }

  @Test
  void exemplo4() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Meia", 19.90, 7, 0.10));
    itens.add(criarItem("Camiseta", 79.90, 2, 0.30));

    CheckoutRequest req = criarRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
    Object resp = service.processar(req);

    assertTrue(resp instanceof CheckoutResponse);
    CheckoutResponse res = (CheckoutResponse) resp;

    assertEquals(299.10, res.subtotalProdutos, 0.01);
    assertEquals(39.80, res.descontoCupom, 0.01);
    assertEquals(0.0, res.frete, 0.01);
    assertEquals(1, res.prazoEntregaDias);
    assertEquals(2.99, res.seguro, 0.01);
    assertEquals(0.0, res.ajustePagamento, 0.01);
    assertEquals(262.29, res.totalFinal, 0.01);
    assertEquals(3, res.parcelas);
    assertEquals(87.43, res.valorParcela, 0.01);
    assertEquals(5.98, res.creditoProximaCompra, 0.01);
    assertFalse(res.brinde);
  }

  @Test
  void exemplo5() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 2, 0.30));
    itens.add(criarItem("Tênis", 249.90, 1, 1.20));

    CheckoutRequest req = criarRequest(itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof CheckoutResponse);
    CheckoutResponse res = (CheckoutResponse) resp;

    assertEquals(409.70, res.subtotalProdutos, 0.01);
    assertEquals(0.0, res.descontoCupom, 0.01);
    assertEquals(0.0, res.frete, 0.01);
    assertEquals(2, res.prazoEntregaDias);
    assertEquals(4.10, res.seguro, 0.01);
    assertEquals(-20.69, res.ajustePagamento, 0.01);
    assertEquals(393.11, res.totalFinal, 0.01);
    assertEquals(1, res.parcelas);
    assertEquals(393.11, res.valorParcela, 0.01);
    assertEquals(20.48, res.creditoProximaCompra, 0.01);
    assertFalse(res.brinde);
  }

  @Test
  void pedidoInvalido_carrinhoVazio() {
    CheckoutRequest req = criarRequest(new ArrayList<>(), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("PEDIDO_INVALIDO", err.erro);
  }

  @Test
  void pedidoInvalido_precoZero() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 0, 1, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("PEDIDO_INVALIDO", err.erro);
  }

  @Test
  void nivelClubeInvalido() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 1, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", null, "PIX", 1, "INEXISTENTE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("NIVEL_CLUBE_INVALIDO", err.erro);
  }

  @Test
  void regiaoInvalida() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 1, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", null, "PIX", 1, "BRONZE", "MARTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("REGIAO_INVALIDA", err.erro);
  }

  @Test
  void modalidadeInvalida() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 1, 0.30));

    CheckoutRequest req = criarRequest(itens, "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("MODALIDADE_INVALIDA", err.erro);
  }

  @Test
  void modalidadeIndisponivel_motoboyAcima5kg() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 20, 0.30));

    CheckoutRequest req = criarRequest(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("MODALIDADE_INDISPONIVEL", err.erro);
  }

  @Test
  void cupomInvalido() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 1, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", "NAOSEXISTE", "PIX", 1, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("CUPOM_INVALIDO", err.erro);
  }

  @Test
  void cupomNaoAplicavel_menos50AbaixoMinimo() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 1, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("CUPOM_NAO_APLICAVEL", err.erro);
  }

  @Test
  void formaPagamentoInvalida() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 1, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", null, "BITCOIN", 1, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("FORMA_PAGAMENTO_INVALIDA", err.erro);
  }

  @Test
  void parcelamentoInvalido_pixComParcelas() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 1, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", null, "PIX", 3, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("PARCELAMENTO_INVALIDO", err.erro);
  }

  @Test
  void formaPagamentoIndisponivel_boletoAcima1000() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 500.0, 3, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", err.erro);
  }

  @Test
  void pedidoInvalido_quantidadeZero() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 79.90, 0, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof ErrorResponse);
    ErrorResponse err = (ErrorResponse) resp;
    assertEquals("PEDIDO_INVALIDO", err.erro);
  }

  @Test
  void clienteOuroComBrinde() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Tênis", 300.0, 2, 0.50));

    CheckoutRequest req = criarRequest(itens, "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof CheckoutResponse);
    CheckoutResponse res = (CheckoutResponse) resp;
    assertEquals(600.0, res.subtotalProdutos, 0.01);
    assertTrue(res.brinde);
  }

  @Test
  void clienteOuroComFreteGratisPorNivel() {
    List<Item> itens = new ArrayList<>();
    itens.add(criarItem("Camiseta", 100.0, 1, 0.30));

    CheckoutRequest req = criarRequest(itens, "ECONOMICA", null, "PIX", 1, "OURO", "SUDESTE");
    Object resp = service.processar(req);

    assertTrue(resp instanceof CheckoutResponse);
    CheckoutResponse res = (CheckoutResponse) resp;
    assertEquals(0.0, res.frete, 0.01);
  }
}
