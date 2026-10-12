package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.enums.FormaPagamento;
import com.loja.enums.ModalidadeEntrega;
import com.loja.enums.NivelClube;
import com.loja.enums.Regiao;
import com.loja.strategy.cupom.CalculadoraCupom;
import com.loja.strategy.cupom.CupomBemvindo10;
import com.loja.strategy.cupom.CupomFreteGratis;
import com.loja.strategy.cupom.CupomLeve3Pague2;
import com.loja.strategy.cupom.CupomMenos50;
import com.loja.strategy.entrega.CalculadoraEntrega;
import com.loja.strategy.entrega.EntregaEconomica;
import com.loja.strategy.entrega.EntregaExpressa;
import com.loja.strategy.entrega.EntregaMotoboy;
import com.loja.strategy.entrega.EntregaRetiradaLoja;
import com.loja.strategy.pagamento.CalculadoraPagamento;
import com.loja.strategy.pagamento.PagamentoBoleto;
import com.loja.strategy.pagamento.PagamentoCartao;
import com.loja.strategy.pagamento.PagamentoPix;
import com.loja.strategy.pagamento.PagamentoResult;
import com.loja.util.RoundingUtil;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
  private static final Map<String, CalculadoraEntrega> ENTREGA_MAP = new HashMap<>();
  private static final Map<String, CalculadoraCupom> CUPOM_MAP = new HashMap<>();
  private static final Map<String, CalculadoraPagamento> PAGAMENTO_MAP = new HashMap<>();

  static {
    ENTREGA_MAP.put("ECONOMICA", new EntregaEconomica());
    ENTREGA_MAP.put("EXPRESSA", new EntregaExpressa());
    ENTREGA_MAP.put("RETIRADA_LOJA", new EntregaRetiradaLoja());
    ENTREGA_MAP.put("MOTOBOY", new EntregaMotoboy());

    CUPOM_MAP.put("BEMVINDO10", new CupomBemvindo10());
    CUPOM_MAP.put("MENOS50", new CupomMenos50());
    CUPOM_MAP.put("FRETEGRATIS", new CupomFreteGratis());
    CUPOM_MAP.put("LEVE3PAGUE2", new CupomLeve3Pague2());

    PAGAMENTO_MAP.put("PIX", new PagamentoPix());
    PAGAMENTO_MAP.put("CARTAO", new PagamentoCartao());
    PAGAMENTO_MAP.put("BOLETO", new PagamentoBoleto());
  }

  public CheckoutResponse calcular(CheckoutRequest request) {
    CheckoutRequest.Item[] itens = request.getItens() != null ? request.getItens().toArray(new CheckoutRequest.Item[0]) : new CheckoutRequest.Item[0];

    if (!validarPedido(itens)) {
      return new CheckoutResponse("PEDIDO_INVALIDO");
    }

    if (!validarNivelClube(request.getNivelClube())) {
      return new CheckoutResponse("NIVEL_CLUBE_INVALIDO");
    }

    if (!validarRegiao(request.getRegiao())) {
      return new CheckoutResponse("REGIAO_INVALIDA");
    }

    if (!validarModalidade(request.getModalidadeEntrega())) {
      return new CheckoutResponse("MODALIDADE_INVALIDA");
    }

    BigDecimal pesoTotal = calcularPesoTotal(itens);
    CalculadoraEntrega calculadoraEntrega = ENTREGA_MAP.get(request.getModalidadeEntrega());
    if (!calculadoraEntrega.podeAplicar(pesoTotal)) {
      return new CheckoutResponse("MODALIDADE_INDISPONIVEL");
    }

    BigDecimal subtotalProdutos = RoundingUtil.round(calcularSubtotalProdutos(itens));

    BigDecimal frete = RoundingUtil.round(calculadoraEntrega.calcularFrete(pesoTotal));
    Integer prazoEntrega = calculadoraEntrega.getPrazo();

    NivelClube nivelClube = NivelClube.valueOf(request.getNivelClube());
    if (nivelClube == NivelClube.OURO) {
      frete = BigDecimal.ZERO;
    }

    String cupomCodigo = request.getCupom();
    BigDecimal descontoCupom = BigDecimal.ZERO;
    if (cupomCodigo != null && !cupomCodigo.isEmpty()) {
      CalculadoraCupom calculadoraCupom = CUPOM_MAP.get(cupomCodigo);
      if (calculadoraCupom == null) {
        return new CheckoutResponse("CUPOM_INVALIDO");
      }

      if (!calculadoraCupom.podeAplicar(itens, subtotalProdutos, frete)) {
        return new CheckoutResponse("CUPOM_NAO_APLICAVEL");
      }

      descontoCupom = RoundingUtil.round(calculadoraCupom.calcular(itens, subtotalProdutos, frete));
      if (cupomCodigo.equals("FRETEGRATIS")) {
        frete = BigDecimal.ZERO;
      }
    }

    if (!validarFormaPagamento(request.getFormaPagamento())) {
      return new CheckoutResponse("FORMA_PAGAMENTO_INVALIDA");
    }

    Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

    if (!validarParcelamento(request.getFormaPagamento(), parcelas)) {
      return new CheckoutResponse("PARCELAMENTO_INVALIDO");
    }

    Regiao regiao = Regiao.valueOf(request.getRegiao());
    BigDecimal porcentagemSeguro = obterPorcentagemSeguro(regiao);
    BigDecimal seguro = RoundingUtil.round(subtotalProdutos.multiply(porcentagemSeguro));

    BigDecimal totalPedido = RoundingUtil.round(
        subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro)
    );

    CalculadoraPagamento calculadoraPagamento = PAGAMENTO_MAP.get(request.getFormaPagamento());
    if (!calculadoraPagamento.podeAplicar(totalPedido, parcelas)) {
      return new CheckoutResponse("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    PagamentoResult pagamentoResult = calculadoraPagamento.calcular(totalPedido, parcelas);

    BigDecimal creditoProximaCompra = BigDecimal.ZERO;
    if (nivelClube == NivelClube.PRATA) {
      creditoProximaCompra = RoundingUtil.round(subtotalProdutos.multiply(new BigDecimal("0.02")));
    } else if (nivelClube == NivelClube.OURO) {
      creditoProximaCompra = RoundingUtil.round(subtotalProdutos.multiply(new BigDecimal("0.05")));
    }

    Boolean brinde = nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;

    CheckoutResponse response = new CheckoutResponse();
    response.setSubtotalProdutos(RoundingUtil.round(subtotalProdutos));
    response.setDescontoCupom(RoundingUtil.round(descontoCupom));
    response.setFrete(RoundingUtil.round(frete));
    response.setPrazoEntregaDias(prazoEntrega);
    response.setSeguro(RoundingUtil.round(seguro));
    response.setAjustePagamento(RoundingUtil.round(pagamentoResult.getAjuste()));
    response.setTotalFinal(RoundingUtil.round(pagamentoResult.getTotalFinal()));
    response.setParcelas(pagamentoResult.getParcelas());
    response.setValorParcela(RoundingUtil.round(pagamentoResult.getValorParcela()));
    response.setCreditoProximaCompra(RoundingUtil.round(creditoProximaCompra));
    response.setBrinde(brinde);

    return response;
  }

  private boolean validarPedido(CheckoutRequest.Item[] itens) {
    if (itens == null || itens.length == 0) {
      return false;
    }

    for (CheckoutRequest.Item item : itens) {
      if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
        return false;
      }
      if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
        return false;
      }
      if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
        return false;
      }
    }
    return true;
  }

  private boolean validarNivelClube(String nivelClube) {
    if (nivelClube == null || nivelClube.isEmpty()) {
      return false;
    }
    try {
      NivelClube.valueOf(nivelClube);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private boolean validarRegiao(String regiao) {
    if (regiao == null || regiao.isEmpty()) {
      return false;
    }
    try {
      Regiao.valueOf(regiao);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private boolean validarModalidade(String modalidade) {
    if (modalidade == null || modalidade.isEmpty()) {
      return false;
    }
    try {
      ModalidadeEntrega.valueOf(modalidade);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private boolean validarFormaPagamento(String formaPagamento) {
    if (formaPagamento == null || formaPagamento.isEmpty()) {
      return false;
    }
    try {
      FormaPagamento.valueOf(formaPagamento);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private boolean validarParcelamento(String formaPagamento, Integer parcelas) {
    if (parcelas == null || parcelas < 1) {
      return false;
    }

    if (formaPagamento.equals("PIX") || formaPagamento.equals("BOLETO")) {
      return parcelas == 1;
    }

    if (formaPagamento.equals("CARTAO")) {
      return parcelas >= 1 && parcelas <= 12;
    }

    return false;
  }

  private BigDecimal calcularPesoTotal(CheckoutRequest.Item[] itens) {
    BigDecimal pesoTotal = BigDecimal.ZERO;
    for (CheckoutRequest.Item item : itens) {
      pesoTotal = pesoTotal.add(item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())));
    }
    return pesoTotal;
  }

  private BigDecimal calcularSubtotalProdutos(CheckoutRequest.Item[] itens) {
    BigDecimal subtotal = BigDecimal.ZERO;
    for (CheckoutRequest.Item item : itens) {
      subtotal = subtotal.add(item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())));
    }
    return subtotal;
  }

  private BigDecimal obterPorcentagemSeguro(Regiao regiao) {
    return switch (regiao) {
      case SUDESTE -> new BigDecimal("0.01");
      case SUL -> new BigDecimal("0.01");
      case CENTRO_OESTE -> new BigDecimal("0.015");
      case NORTE -> new BigDecimal("0.025");
      case NORDESTE -> new BigDecimal("0.02");
    };
  }
}
