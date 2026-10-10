package com.loja.checkout.service;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResumoServiceTest {

  private ResumoService resumoService;

  @BeforeEach
  public void setUp() {
    resumoService = new ResumoService();
  }

  private ResumoRequest criarRequisicao(List<ItemCarrinho> itens, String modalidade, String cupom,
                                         String formaPagamento, Integer parcelas, String nivelClube, String regiao) {
    ResumoRequest request = new ResumoRequest();
    request.setItens(itens);
    request.setModalidadeEntrega(modalidade);
    request.setCupom(cupom);
    request.setFormaPagamento(formaPagamento);
    request.setParcelas(parcelas);
    request.setNivelClube(nivelClube);
    request.setRegiao(regiao);
    return request;
  }

  @Test
  public void testExemplo1() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
    itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));

    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ResumoResponse);

    ResumoResponse response = (ResumoResponse) resultado;
    assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
    assertEquals(40.97, response.getDescontoCupom(), 0.01);
    assertEquals(33.10, response.getFrete(), 0.01);
    assertEquals(2, response.getPrazoEntregaDias());
    assertEquals(10.24, response.getSeguro(), 0.01);
    assertEquals(-20.60, response.getAjustePagamento(), 0.01);
    assertEquals(391.47, response.getTotalFinal(), 0.01);
    assertEquals(1, response.getParcelas());
    assertEquals(391.47, response.getValorParcela(), 0.01);
    assertEquals(0.00, response.getCreditoProximaCompra(), 0.01);
    assertFalse(response.getBrinde());
  }

  @Test
  public void testExemplo2() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
    itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));

    ResumoRequest request = criarRequisicao(itens, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ResumoResponse);

    ResumoResponse response = (ResumoResponse) resultado;
    assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
    assertEquals(0.00, response.getDescontoCupom(), 0.01);
    assertEquals(15.60, response.getFrete(), 0.01);
    assertEquals(7, response.getPrazoEntregaDias());
    assertEquals(6.15, response.getSeguro(), 0.01);
    assertEquals(30.55, response.getAjustePagamento(), 0.01);
    assertEquals(462.00, response.getTotalFinal(), 0.01);
    assertEquals(6, response.getParcelas());
    assertEquals(77.00, response.getValorParcela(), 0.01);
    assertEquals(8.19, response.getCreditoProximaCompra(), 0.01);
    assertFalse(response.getBrinde());
  }

  @Test
  public void testExemplo3() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Fone", 199.90, 2, 0.25));

    ResumoRequest request = criarRequisicao(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ResumoResponse);

    ResumoResponse response = (ResumoResponse) resultado;
    assertEquals(399.80, response.getSubtotalProdutos(), 0.01);
    assertEquals(50.00, response.getDescontoCupom(), 0.01);
    assertEquals(18.00, response.getFrete(), 0.01);
    assertEquals(0, response.getPrazoEntregaDias());
    assertEquals(8.00, response.getSeguro(), 0.01);
    assertEquals(3.49, response.getAjustePagamento(), 0.01);
    assertEquals(379.29, response.getTotalFinal(), 0.01);
    assertEquals(1, response.getParcelas());
    assertEquals(379.29, response.getValorParcela(), 0.01);
    assertEquals(0.00, response.getCreditoProximaCompra(), 0.01);
    assertFalse(response.getBrinde());
  }

  @Test
  public void testExemplo4() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Meia", 19.90, 7, 0.10));
    itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));

    ResumoRequest request = criarRequisicao(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ResumoResponse);

    ResumoResponse response = (ResumoResponse) resultado;

    assertEquals(299.10, response.getSubtotalProdutos(), 0.01);
    assertEquals(39.80, response.getDescontoCupom(), 0.01);
    assertEquals(0.00, response.getFrete(), 0.01);
    assertEquals(1, response.getPrazoEntregaDias());
    assertEquals(2.99, response.getSeguro(), 0.01);
    assertEquals(0.00, response.getAjustePagamento(), 0.01);
    assertEquals(262.29, response.getTotalFinal(), 0.01);
    assertEquals(3, response.getParcelas());
    assertEquals(87.43, response.getValorParcela(), 0.01);
    assertEquals(5.98, response.getCreditoProximaCompra(), 0.01);
    assertFalse(response.getBrinde());
  }

  @Test
  public void testExemplo5() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
    itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));

    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ResumoResponse);

    ResumoResponse response = (ResumoResponse) resultado;
    assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
    assertEquals(0.00, response.getDescontoCupom(), 0.01);
    assertEquals(0.00, response.getFrete(), 0.01);
    assertEquals(2, response.getPrazoEntregaDias());
    assertEquals(4.10, response.getSeguro(), 0.01);
    assertEquals(-20.69, response.getAjustePagamento(), 0.01);
    assertEquals(393.11, response.getTotalFinal(), 0.01);
    assertEquals(1, response.getParcelas());
    assertEquals(393.11, response.getValorParcela(), 0.01);
    assertEquals(20.48, response.getCreditoProximaCompra(), 0.01);
    assertFalse(response.getBrinde());
  }

  @Test
  public void testCarrinhoVazio() {
    List<ItemCarrinho> itens = new ArrayList<>();
    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("PEDIDO_INVALIDO", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testItemComPrecoNegativo() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", -10.0, 1, 0.5));

    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("PEDIDO_INVALIDO", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testNivelClubeInvalido() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 10.0, 1, 0.5));

    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1, "INVALIDO", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("NIVEL_CLUBE_INVALIDO", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testRegiaoInvalida() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 10.0, 1, 0.5));

    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "INVALIDA");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("REGIAO_INVALIDA", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testModalidadeInvalida() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 10.0, 1, 0.5));

    ResumoRequest request = criarRequisicao(itens, "INVALIDA", null, "PIX", 1, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("MODALIDADE_INVALIDA", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testMotoboySobrepesoIndisponivel() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 100.0, 1, 6.0));

    ResumoRequest request = criarRequisicao(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("MODALIDADE_INDISPONIVEL", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testCupomInvalido() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 10.0, 1, 0.5));

    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", "INVALIDO", "PIX", 1, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("CUPOM_INVALIDO", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testMenos50NaoAplicavel() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 100.0, 1, 0.5));

    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("CUPOM_NAO_APLICAVEL", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testFormaPagamentoInvalida() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 10.0, 1, 0.5));

    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", null, "INVALIDA", 1, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("FORMA_PAGAMENTO_INVALIDA", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testParcelamentoInvalidoPix() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 10.0, 1, 0.5));

    ResumoRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 2, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("PARCELAMENTO_INVALIDO", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testBoletoIndisponivelAcimaLimite() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 2000.0, 1, 0.5));

    ResumoRequest request = criarRequisicao(itens, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ErroResponse);
    assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ((ErroResponse) resultado).getErro());
  }

  @Test
  public void testParcelasDefault() {
    List<ItemCarrinho> itens = new ArrayList<>();
    itens.add(new ItemCarrinho("Produto", 100.0, 1, 0.5));

    ResumoRequest request = new ResumoRequest();
    request.setItens(itens);
    request.setModalidadeEntrega("EXPRESSA");
    request.setCupom(null);
    request.setFormaPagamento("PIX");
    request.setParcelas(null);
    request.setNivelClube("BRONZE");
    request.setRegiao("NORTE");

    Object resultado = resumoService.calcularResumo(request);
    assertTrue(resultado instanceof ResumoResponse);

    ResumoResponse response = (ResumoResponse) resultado;
    assertEquals(1, response.getParcelas());
  }
}
