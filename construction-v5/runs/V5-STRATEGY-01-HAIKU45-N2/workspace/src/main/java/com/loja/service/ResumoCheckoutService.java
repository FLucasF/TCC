package com.loja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemCarrinho;
import com.loja.enums.FormaPagamento;
import com.loja.enums.ModalidadeEntrega;
import com.loja.enums.NivelClube;
import com.loja.enums.Regiao;
import com.loja.strategy.CalculadoraBeneficoClube;
import com.loja.strategy.CalculadoraCupom;
import com.loja.strategy.CalculadoraEntrega;
import com.loja.strategy.CalculadoraPagamento;
import com.loja.strategy.CalculadoraSeguro;
import com.loja.util.Arredondador;

@Service
public class ResumoCheckoutService {
  public CheckoutResponse calcularResumo(CheckoutRequest request) {
    validarPedidoBasico(request);
    validarNivelClube(request);
    validarRegiao(request);
    validarModalidade(request);
    validarFormaPagamento(request);

    List<ItemCarrinho> itens = request.itens;
    String modalidade = request.modalidadeEntrega;
    String cupomCodigo = request.cupom;
    String formaPagamento = request.formaPagamento;
    int parcelas = request.parcelas != null ? request.parcelas : 1;
    String nivelClube = request.nivelClube;
    String regiao = request.regiao;

    BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);
    BigDecimal pesoTotal = calcularPesoTotal(itens);

    CalculadoraEntrega calculadoraEntrega = obterCalculadoraEntrega(modalidade);
    calculadoraEntrega.validar(pesoTotal);

    BigDecimal frete = calculadoraEntrega.calcularFrete(pesoTotal);
    int prazoEntregaDias = calculadoraEntrega.obterPrazoEntregaDias();

    CalculadoraBeneficoClube beneficoClube = obterBeneficoClube(nivelClube);
    if (beneficoClube.temFreteGratis()) {
      frete = BigDecimal.ZERO;
    }

    BigDecimal descontoCupom = BigDecimal.ZERO;
    CalculadoraCupom calculadoraCupom = obterCalculadoraCupom(cupomCodigo);
    if (calculadoraCupom != null) {
      calculadoraCupom.validar(subtotalProdutos);
      BigDecimal freteParaCupom = beneficoClube.temFreteGratis() ? BigDecimal.ZERO : frete;
      descontoCupom = calculadoraCupom.calcularDesconto(subtotalProdutos, freteParaCupom, itens);
    }

    CalculadoraSeguro calculadoraSeguro = obterCalculadoraSeguro(regiao);
    BigDecimal seguro = calculadoraSeguro.calcularSeguro(subtotalProdutos);

    BigDecimal totalAntesAjuste = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);
    totalAntesAjuste = Arredondador.arredondar(totalAntesAjuste);

    CalculadoraPagamento calculadoraPagamento = obterCalculadoraPagamento(formaPagamento);
    calculadoraPagamento.validar(parcelas, totalAntesAjuste);

    BigDecimal ajustePagamento = calculadoraPagamento.calcularAjuste(totalAntesAjuste, parcelas);
    BigDecimal totalFinal = totalAntesAjuste.add(ajustePagamento);
    totalFinal = Arredondador.arredondar(totalFinal);

    BigDecimal valorParcela = Arredondador.arredondar(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));

    BigDecimal creditoProximaCompra = beneficoClube.calcularCredito(subtotalProdutos);
    boolean temBrinde = beneficoClube.temBrinde(subtotalProdutos);

    CheckoutResponse response = new CheckoutResponse();
    response.subtotalProdutos = subtotalProdutos;
    response.descontoCupom = Arredondador.arredondar(descontoCupom);
    response.frete = frete;
    response.prazoEntregaDias = prazoEntregaDias;
    response.seguro = seguro;
    response.ajustePagamento = Arredondador.arredondar(ajustePagamento);
    response.totalFinal = totalFinal;
    response.parcelas = parcelas;
    response.valorParcela = valorParcela;
    response.creditoProximaCompra = creditoProximaCompra;
    response.brinde = temBrinde;

    return response;
  }

  private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
    BigDecimal subtotal = BigDecimal.ZERO;
    for (ItemCarrinho item : itens) {
      BigDecimal precoItem = BigDecimal.valueOf(item.precoUnitario).multiply(new BigDecimal(item.quantidade));
      subtotal = subtotal.add(precoItem);
    }
    return Arredondador.arredondar(subtotal);
  }

  private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
    BigDecimal pesoTotal = BigDecimal.ZERO;
    for (ItemCarrinho item : itens) {
      BigDecimal pesoItem = BigDecimal.valueOf(item.pesoKg).multiply(new BigDecimal(item.quantidade));
      pesoTotal = pesoTotal.add(pesoItem);
    }
    return pesoTotal;
  }

  private void validarPedidoBasico(CheckoutRequest request) {
    if (request.itens == null || request.itens.isEmpty()) {
      throw new IllegalArgumentException("PEDIDO_INVALIDO");
    }

    for (ItemCarrinho item : request.itens) {
      if (item.precoUnitario == null || item.precoUnitario <= 0 || item.quantidade == null || item.quantidade <= 0
          || item.pesoKg == null || item.pesoKg < 0) {
        throw new IllegalArgumentException("PEDIDO_INVALIDO");
      }
    }
  }

  private void validarNivelClube(CheckoutRequest request) {
    if (request.nivelClube == null || request.nivelClube.isEmpty()) {
      throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
    }

    try {
      NivelClube.valueOf(request.nivelClube);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
    }
  }

  private void validarRegiao(CheckoutRequest request) {
    if (request.regiao == null || request.regiao.isEmpty()) {
      throw new IllegalArgumentException("REGIAO_INVALIDA");
    }

    try {
      Regiao.valueOf(request.regiao);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("REGIAO_INVALIDA");
    }
  }

  private void validarModalidade(CheckoutRequest request) {
    if (request.modalidadeEntrega == null || request.modalidadeEntrega.isEmpty()) {
      throw new IllegalArgumentException("MODALIDADE_INVALIDA");
    }

    try {
      ModalidadeEntrega.valueOf(request.modalidadeEntrega);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("MODALIDADE_INVALIDA");
    }
  }

  private void validarFormaPagamento(CheckoutRequest request) {
    if (request.formaPagamento == null || request.formaPagamento.isEmpty()) {
      throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
    }

    try {
      FormaPagamento.valueOf(request.formaPagamento);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
    }
  }

  private CalculadoraEntrega obterCalculadoraEntrega(String modalidade) {
    return switch (modalidade) {
      case "ECONOMICA" -> new com.loja.strategy.EntregaEconomica();
      case "EXPRESSA" -> new com.loja.strategy.EntregaExpressa();
      case "RETIRADA_LOJA" -> new com.loja.strategy.EntregaRetiradaLoja();
      case "MOTOBOY" -> new com.loja.strategy.EntregaMotoboy();
      default -> throw new IllegalArgumentException("MODALIDADE_INVALIDA");
    };
  }

  private CalculadoraCupom obterCalculadoraCupom(String cupom) {
    if (cupom == null || cupom.isEmpty()) {
      return null;
    }

    return switch (cupom) {
      case "BEMVINDO10" -> new com.loja.strategy.CupomBemvindo10();
      case "MENOS50" -> new com.loja.strategy.CupomMenos50();
      case "FRETEGRATIS" -> new com.loja.strategy.CupomFreteGratis();
      case "LEVE3PAGUE2" -> new com.loja.strategy.CupomLeve3Pague2();
      default -> throw new IllegalArgumentException("CUPOM_INVALIDO");
    };
  }

  private CalculadoraBeneficoClube obterBeneficoClube(String nivel) {
    return switch (nivel) {
      case "BRONZE" -> new com.loja.strategy.ClubeBronze();
      case "PRATA" -> new com.loja.strategy.ClubePrata();
      case "OURO" -> new com.loja.strategy.ClubeOuro();
      default -> throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
    };
  }

  private CalculadoraSeguro obterCalculadoraSeguro(String regiao) {
    return switch (regiao) {
      case "SUDESTE" -> new com.loja.strategy.SeguroSudeste();
      case "SUL" -> new com.loja.strategy.SeguroSul();
      case "CENTRO_OESTE" -> new com.loja.strategy.SeguroCentroOeste();
      case "NORTE" -> new com.loja.strategy.SeguroNorte();
      case "NORDESTE" -> new com.loja.strategy.SeguroNordeste();
      default -> throw new IllegalArgumentException("REGIAO_INVALIDA");
    };
  }

  private CalculadoraPagamento obterCalculadoraPagamento(String formaPagamento) {
    return switch (formaPagamento) {
      case "PIX" -> new com.loja.strategy.PagamentoPix();
      case "CARTAO" -> new com.loja.strategy.PagamentoCartao();
      case "BOLETO" -> new com.loja.strategy.PagamentoBoleto();
      default -> throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
    };
  }
}
