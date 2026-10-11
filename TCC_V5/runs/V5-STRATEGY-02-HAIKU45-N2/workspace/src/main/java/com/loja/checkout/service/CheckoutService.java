package com.loja.checkout.service;

import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.model.request.CheckoutRequest;
import com.loja.checkout.model.request.Item;
import com.loja.checkout.model.response.CheckoutResponse;
import com.loja.checkout.util.RoundingUtil;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

  public CheckoutResponse calcular(CheckoutRequest request) {
    validar(request);

    List<Item> itens = request.getItens();
    ModalidadeEntrega modalidade = request.getModalidadeEntrega();
    String cupom = request.getCupom();
    FormaPagamento formaPagamento = request.getFormaPagamento();
    Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
    NivelClube nivelClube = request.getNivelClube();
    Regiao regiao = request.getRegiao();

    BigDecimal subtotalProdutos = calcularSubtotal(itens);
    BigDecimal frete = calcularFrete(modalidade, itens, nivelClube);
    BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, frete, itens);
    Integer prazoEntregaDias = obterPrazo(modalidade);
    BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

    BigDecimal totalAntesPagamento = subtotalProdutos
        .subtract(descontoCupom)
        .add(frete)
        .add(seguro);
    totalAntesPagamento = RoundingUtil.round(totalAntesPagamento);

    BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, totalAntesPagamento, parcelas);
    BigDecimal totalFinal = totalAntesPagamento.add(ajustePagamento);
    totalFinal = RoundingUtil.round(totalFinal);

    BigDecimal creditoProximaCompra = calcularCredit(nivelClube, subtotalProdutos);

    Boolean brinde = deveDarBrinde(nivelClube, subtotalProdutos);

    BigDecimal valorParcela = RoundingUtil.round(totalFinal.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));

    return new CheckoutResponse(
        RoundingUtil.round(subtotalProdutos),
        RoundingUtil.round(descontoCupom),
        RoundingUtil.round(frete),
        prazoEntregaDias,
        RoundingUtil.round(seguro),
        RoundingUtil.round(ajustePagamento),
        totalFinal,
        parcelas,
        valorParcela,
        creditoProximaCompra,
        brinde
    );
  }

  private void validar(CheckoutRequest request) {
    validarPedido(request);
    validarNivelClube(request.getNivelClube());
    validarRegiao(request.getRegiao());
    validarModalidade(request);
    validarCupom(request);
    validarFormaPagamento(request);
  }

  private void validarPedido(CheckoutRequest request) {
    List<Item> itens = request.getItens();
    if (itens == null || itens.isEmpty()) {
      throw new ValidationException("PEDIDO_INVALIDO");
    }
    for (Item item : itens) {
      if (item.getPrecoUnitario() == null || item.getPrecoUnitario().signum() <= 0 ||
          item.getQuantidade() == null || item.getQuantidade() <= 0 ||
          item.getPesoKg() == null || item.getPesoKg().signum() < 0) {
        throw new ValidationException("PEDIDO_INVALIDO");
      }
    }
  }

  private void validarNivelClube(NivelClube nivelClube) {
    if (nivelClube == null) {
      throw new ValidationException("NIVEL_CLUBE_INVALIDO");
    }
  }

  private void validarRegiao(Regiao regiao) {
    if (regiao == null) {
      throw new ValidationException("REGIAO_INVALIDA");
    }
  }

  private void validarModalidade(CheckoutRequest request) {
    ModalidadeEntrega modalidade = request.getModalidadeEntrega();
    if (modalidade == null) {
      throw new ValidationException("MODALIDADE_INVALIDA");
    }

    if (modalidade == ModalidadeEntrega.MOTOBOY) {
      BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
      if (pesoTotal.compareTo(BigDecimal.valueOf(5)) > 0) {
        throw new ValidationException("MODALIDADE_INDISPONIVEL");
      }
    }
  }

  private void validarCupom(CheckoutRequest request) {
    String cupom = request.getCupom();
    if (cupom == null || cupom.isEmpty()) {
      return;
    }

    String[] cuponsValidos = {"BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2"};
    boolean existe = false;
    for (String c : cuponsValidos) {
      if (c.equals(cupom)) {
        existe = true;
        break;
      }
    }
    if (!existe) {
      throw new ValidationException("CUPOM_INVALIDO");
    }

    if ("MENOS50".equals(cupom)) {
      BigDecimal subtotal = calcularSubtotal(request.getItens());
      if (subtotal.compareTo(BigDecimal.valueOf(300)) < 0) {
        throw new ValidationException("CUPOM_NAO_APLICAVEL");
      }
    }
  }

  private void validarFormaPagamento(CheckoutRequest request) {
    FormaPagamento formaPagamento = request.getFormaPagamento();
    if (formaPagamento == null) {
      throw new ValidationException("FORMA_PAGAMENTO_INVALIDA");
    }

    Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

    if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
      if (parcelas != 1) {
        throw new ValidationException("PARCELAMENTO_INVALIDO");
      }
    } else if (formaPagamento == FormaPagamento.CARTAO) {
      if (parcelas < 1 || parcelas > 12) {
        throw new ValidationException("PARCELAMENTO_INVALIDO");
      }
    }

    if (formaPagamento == FormaPagamento.BOLETO) {
      BigDecimal subtotalProdutos = calcularSubtotal(request.getItens());
      BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), request.getItens(), request.getNivelClube());
      BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, frete, request.getItens());
      BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());
      BigDecimal total = subtotalProdutos
          .subtract(descontoCupom)
          .add(frete)
          .add(seguro);
      total = RoundingUtil.round(total);
      if (total.compareTo(BigDecimal.valueOf(1000)) > 0) {
        throw new ValidationException("FORMA_PAGAMENTO_INDISPONIVEL");
      }
    }
  }

  private BigDecimal calcularSubtotal(List<Item> itens) {
    BigDecimal subtotal = BigDecimal.ZERO;
    for (Item item : itens) {
      BigDecimal itemTotal = item.getPrecoUnitario()
          .multiply(BigDecimal.valueOf(item.getQuantidade()));
      subtotal = subtotal.add(itemTotal);
    }
    return subtotal;
  }

  private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotalProdutos, BigDecimal frete, List<Item> itens) {
    if (cupom == null || cupom.isEmpty()) {
      return BigDecimal.ZERO;
    }

    switch (cupom) {
      case "BEMVINDO10":
        return RoundingUtil.round(subtotalProdutos.multiply(BigDecimal.valueOf(0.10)));
      case "MENOS50":
        return RoundingUtil.round(BigDecimal.valueOf(50));
      case "LEVE3PAGUE2":
        return calcularDescontoLeve3Pague2(itens);
      case "FRETEGRATIS":
        return frete;
      default:
        return BigDecimal.ZERO;
    }
  }

  private BigDecimal calcularDescontoLeve3Pague2(List<Item> itens) {
    if (itens == null || itens.isEmpty()) {
      return BigDecimal.ZERO;
    }

    BigDecimal desconto = BigDecimal.ZERO;
    for (Item item : itens) {
      int unidadesGratis = item.getQuantidade() / 3;
      BigDecimal descontoItem = item.getPrecoUnitario()
          .multiply(BigDecimal.valueOf(unidadesGratis));
      desconto = desconto.add(descontoItem);
    }
    return RoundingUtil.round(desconto);
  }

  private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<Item> itens, NivelClube nivelClube) {
    if (nivelClube == NivelClube.OURO) {
      return BigDecimal.ZERO;
    }

    switch (modalidade) {
      case RETIRADA_LOJA:
        return BigDecimal.ZERO;
      case MOTOBOY:
        return BigDecimal.valueOf(18);
      case ECONOMICA:
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        return BigDecimal.valueOf(12).add(pesoTotal.multiply(BigDecimal.valueOf(2)));
      case EXPRESSA:
        pesoTotal = calcularPesoTotal(itens);
        return BigDecimal.valueOf(25).add(pesoTotal.multiply(BigDecimal.valueOf(4.50)));
      default:
        return BigDecimal.ZERO;
    }
  }

  private BigDecimal calcularPesoTotal(List<Item> itens) {
    BigDecimal pesoTotal = BigDecimal.ZERO;
    for (Item item : itens) {
      BigDecimal itemPeso = item.getPesoKg()
          .multiply(BigDecimal.valueOf(item.getQuantidade()));
      pesoTotal = pesoTotal.add(itemPeso);
    }
    return pesoTotal;
  }

  private Integer obterPrazo(ModalidadeEntrega modalidade) {
    switch (modalidade) {
      case ECONOMICA:
        return 7;
      case EXPRESSA:
        return 2;
      case RETIRADA_LOJA:
        return 1;
      case MOTOBOY:
        return 0;
      default:
        return 0;
    }
  }

  private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
    BigDecimal percentual = obterPercentualSeguro(regiao);
    return RoundingUtil.round(subtotalProdutos.multiply(percentual));
  }

  private BigDecimal obterPercentualSeguro(Regiao regiao) {
    switch (regiao) {
      case SUDESTE:
        return BigDecimal.valueOf(0.01);
      case SUL:
        return BigDecimal.valueOf(0.01);
      case CENTRO_OESTE:
        return BigDecimal.valueOf(0.015);
      case NORTE:
        return BigDecimal.valueOf(0.025);
      case NORDESTE:
        return BigDecimal.valueOf(0.02);
      default:
        return BigDecimal.ZERO;
    }
  }

  private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal total, Integer parcelas) {
    switch (formaPagamento) {
      case PIX:
        return RoundingUtil.round(total.multiply(BigDecimal.valueOf(-0.05)));
      case BOLETO:
        return RoundingUtil.round(BigDecimal.valueOf(3.49));
      case CARTAO:
        return calcularAjusteCartao(total, parcelas);
      default:
        return BigDecimal.ZERO;
    }
  }

  private BigDecimal calcularAjusteCartao(BigDecimal total, Integer parcelas) {
    if (parcelas <= 3) {
      return BigDecimal.ZERO;
    }

    BigDecimal taxaMensal = BigDecimal.valueOf(0.0199);
    BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);

    double umMaisTaxaInvertido = Math.pow(umMaisTaxa.doubleValue(), -parcelas);
    BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.valueOf(umMaisTaxaInvertido));

    BigDecimal numerador = total.multiply(taxaMensal);
    BigDecimal parcelaComJuros = numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
    parcelaComJuros = RoundingUtil.round(parcelaComJuros);

    BigDecimal totalComJuros = parcelaComJuros.multiply(BigDecimal.valueOf(parcelas));
    BigDecimal ajuste = totalComJuros.subtract(total);
    return RoundingUtil.round(ajuste);
  }

  private BigDecimal calcularCredit(NivelClube nivelClube, BigDecimal subtotalProdutos) {
    switch (nivelClube) {
      case BRONZE:
        return BigDecimal.ZERO;
      case PRATA:
        return RoundingUtil.round(subtotalProdutos.multiply(BigDecimal.valueOf(0.02)));
      case OURO:
        return RoundingUtil.round(subtotalProdutos.multiply(BigDecimal.valueOf(0.05)));
      default:
        return BigDecimal.ZERO;
    }
  }

  private Boolean deveDarBrinde(NivelClube nivelClube, BigDecimal subtotalProdutos) {
    if (nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(BigDecimal.valueOf(500)) > 0) {
      return true;
    }
    return false;
  }

  public static class ValidationException extends RuntimeException {
    private final String codigo;

    public ValidationException(String codigo) {
      super(codigo);
      this.codigo = codigo;
    }

    public String getCodigo() {
      return codigo;
    }
  }
}
