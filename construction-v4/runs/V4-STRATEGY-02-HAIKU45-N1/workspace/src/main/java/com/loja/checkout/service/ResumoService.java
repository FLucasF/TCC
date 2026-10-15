package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.util.Arredondador;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumoService {

  private static final Map<String, Double> TAXAS_SEGURO = new HashMap<>();

  static {
    TAXAS_SEGURO.put("SUDESTE", 0.01);
    TAXAS_SEGURO.put("SUL", 0.01);
    TAXAS_SEGURO.put("CENTRO_OESTE", 0.015);
    TAXAS_SEGURO.put("NORTE", 0.025);
    TAXAS_SEGURO.put("NORDESTE", 0.02);
  }

  public Object calcularResumo(ResumoRequest request) {
    String validacao = validarRequisicao(request);
    if (validacao != null) {
      return new com.loja.checkout.dto.ErroResponse(validacao);
    }

    ResumoResponse response = new ResumoResponse();

    List<ItemCarrinho> itens = request.getItens();
    String cupom = request.getCupom();
    String modalidadeEntrega = request.getModalidadeEntrega();
    String formaPagamento = request.getFormaPagamento();
    Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
    String nivelClube = request.getNivelClube();
    String regiao = request.getRegiao();

    double subtotalProdutos = calcularSubtotalProdutos(itens);
    response.setSubtotalProdutos(Arredondador.arredondar(subtotalProdutos));

    double descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, modalidadeEntrega, itens);
    response.setDescontoCupom(Arredondador.arredondar(descontoCupom));

    double peso = calcularPeso(itens);
    double frete = calcularFrete(modalidadeEntrega, peso, nivelClube);
    response.setFrete(Arredondador.arredondar(frete));

    response.setPrazoEntregaDias(ModalidadeEntrega.valueOf(modalidadeEntrega).getPrazo());

    double seguro = calcularSeguro(subtotalProdutos, regiao);
    response.setSeguro(Arredondador.arredondar(seguro));

    double totalPedido = subtotalProdutos - descontoCupom + frete + seguro;
    totalPedido = Arredondador.arredondar(totalPedido);

    double[] ajusteETotal = calcularAjusteETotal(totalPedido, formaPagamento, parcelas);
    double ajustePagamento = ajusteETotal[0];
    double totalFinal = ajusteETotal[1];

    response.setAjustePagamento(Arredondador.arredondar(ajustePagamento));
    response.setTotalFinal(Arredondador.arredondar(totalFinal));

    response.setParcelas(parcelas);

    double valorParcela = calcularValorParcela(totalFinal, parcelas);
    response.setValorParcela(Arredondador.arredondar(valorParcela));

    double creditoProximaCompra = calcularCreditoClube(subtotalProdutos, nivelClube);
    response.setCreditoProximaCompra(Arredondador.arredondar(creditoProximaCompra));

    boolean brinde = NivelClube.OURO.name().equals(nivelClube) && subtotalProdutos > 500;
    response.setBrinde(brinde);

    return response;
  }

  private String validarRequisicao(ResumoRequest request) {
    if (request.getItens() == null || request.getItens().isEmpty()) {
      return "PEDIDO_INVALIDO";
    }

    for (ItemCarrinho item : request.getItens()) {
      if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
          item.getQuantidade() == null || item.getQuantidade() <= 0 ||
          item.getPesoKg() == null || item.getPesoKg() < 0) {
        return "PEDIDO_INVALIDO";
      }
    }

    try {
      NivelClube.valueOf(request.getNivelClube());
    } catch (IllegalArgumentException | NullPointerException e) {
      return "NIVEL_CLUBE_INVALIDO";
    }

    try {
      Regiao.valueOf(request.getRegiao());
    } catch (IllegalArgumentException | NullPointerException e) {
      return "REGIAO_INVALIDA";
    }

    String modalidadeStr = request.getModalidadeEntrega();
    ModalidadeEntrega modalidade;
    try {
      modalidade = ModalidadeEntrega.valueOf(modalidadeStr);
    } catch (IllegalArgumentException | NullPointerException e) {
      return "MODALIDADE_INVALIDA";
    }

    double peso = calcularPeso(request.getItens());
    if (modalidade == ModalidadeEntrega.MOTOBOY && peso > 5) {
      return "MODALIDADE_INDISPONIVEL";
    }

    String cupom = request.getCupom();
    if (cupom != null) {
      if (!isCupomValido(cupom)) {
        return "CUPOM_INVALIDO";
      }

      double subtotal = calcularSubtotalProdutos(request.getItens());
      if (!isCupomAplicavel(cupom, subtotal)) {
        return "CUPOM_NAO_APLICAVEL";
      }
    }

    try {
      FormaPagamento.valueOf(request.getFormaPagamento());
    } catch (IllegalArgumentException | NullPointerException e) {
      return "FORMA_PAGAMENTO_INVALIDA";
    }

    Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
    if (parcelas < 1 || parcelas > 12) {
      return "PARCELAMENTO_INVALIDO";
    }

    FormaPagamento formaPagamento = FormaPagamento.valueOf(request.getFormaPagamento());
    if ((formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) && parcelas != 1) {
      return "PARCELAMENTO_INVALIDO";
    }

    if (formaPagamento == FormaPagamento.CARTAO && (parcelas < 1 || parcelas > 12)) {
      return "PARCELAMENTO_INVALIDO";
    }

    double subtotalProdutos = calcularSubtotalProdutos(request.getItens());
    double descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, modalidadeStr, request.getItens());
    double frete = calcularFrete(modalidadeStr, peso, request.getNivelClube());
    double seguro = calcularSeguro(subtotalProdutos, request.getRegiao());
    double totalPedido = subtotalProdutos - descontoCupom + frete + seguro;
    totalPedido = Arredondador.arredondar(totalPedido);

    if (formaPagamento == FormaPagamento.BOLETO && totalPedido > 1000) {
      return "FORMA_PAGAMENTO_INDISPONIVEL";
    }

    return null;
  }

  private boolean isCupomValido(String cupom) {
    return cupom.equals("BEMVINDO10") || cupom.equals("MENOS50") ||
           cupom.equals("FRETEGRATIS") || cupom.equals("LEVE3PAGUE2");
  }

  private boolean isCupomAplicavel(String cupom, double subtotal) {
    if (cupom.equals("MENOS50")) {
      return subtotal >= 300;
    }
    return true;
  }

  private double calcularSubtotalProdutos(List<ItemCarrinho> itens) {
    double subtotal = 0;
    for (ItemCarrinho item : itens) {
      if (item.getNome().isEmpty() || item.getPrecoUnitario() == null || item.getQuantidade() == null) {
        continue;
      }
      subtotal += item.getPrecoUnitario() * item.getQuantidade();
    }
    return subtotal;
  }

  private double calcularPeso(List<ItemCarrinho> itens) {
    double peso = 0;
    for (ItemCarrinho item : itens) {
      peso += item.getPesoKg() * item.getQuantidade();
    }
    return peso;
  }

  private double calcularDescontoCupom(String cupom, double subtotal, String modalidadeEntrega) {
    return calcularDescontoCupom(cupom, subtotal, modalidadeEntrega, null);
  }

  private double calcularDescontoCupom(String cupom, double subtotal, String modalidadeEntrega, List<ItemCarrinho> itens) {
    if (cupom == null) {
      return 0;
    }

    switch (cupom) {
      case "BEMVINDO10":
        return Arredondador.arredondar(subtotal * 0.10);
      case "MENOS50":
        return 50.0;
      case "FRETEGRATIS":
        return calcularFrete(modalidadeEntrega, 0, null);
      case "LEVE3PAGUE2":
        if (itens == null) {
          return 0;
        }
        return calcularDescontoLeve3Pague2(itens);
      default:
        return 0;
    }
  }

  private double calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
    double desconto = 0;
    for (ItemCarrinho item : itens) {
      int quantidadeGratuita = item.getQuantidade() / 3;
      desconto += item.getPrecoUnitario() * quantidadeGratuita;
    }
    return Arredondador.arredondar(desconto);
  }

  private double calcularFrete(String modalidadeStr, double peso, String nivelClube) {
    if (nivelClube != null && NivelClube.OURO.name().equals(nivelClube)) {
      return 0;
    }

    ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(modalidadeStr);

    switch (modalidade) {
      case ECONOMICA:
        return 12 + (2 * peso);
      case EXPRESSA:
        return 25 + (4.5 * peso);
      case RETIRADA_LOJA:
        return 0;
      case MOTOBOY:
        return 18;
      default:
        return 0;
    }
  }

  private double calcularSeguro(double subtotal, String regiaoStr) {
    double taxa = TAXAS_SEGURO.getOrDefault(regiaoStr, 0.0);
    return subtotal * taxa;
  }

  private double[] calcularAjusteETotal(double totalPedido, String formaPagamentoStr, Integer parcelas) {
    FormaPagamento formaPagamento = FormaPagamento.valueOf(formaPagamentoStr);

    switch (formaPagamento) {
      case PIX:
        double descontoPix = totalPedido * 0.05;
        return new double[]{-descontoPix, totalPedido - descontoPix};

      case CARTAO:
        if (parcelas <= 3) {
          return new double[]{0, totalPedido};
        } else {
          double taxa = 0.0199;
          double parcela = (totalPedido * taxa) / (1 - Math.pow(1 + taxa, -parcelas));
          parcela = Arredondador.arredondar(parcela);
          double totalFinal = parcela * parcelas;
          double ajuste = totalFinal - totalPedido;
          return new double[]{ajuste, totalFinal};
        }

      case BOLETO:
        double tarifa = 3.49;
        return new double[]{tarifa, totalPedido + tarifa};

      default:
        return new double[]{0, totalPedido};
    }
  }

  private double calcularValorParcela(double totalFinal, Integer parcelas) {
    return totalFinal / parcelas;
  }

  private double calcularCreditoClube(double subtotal, String nivelClube) {
    switch (nivelClube) {
      case "PRATA":
        return subtotal * 0.02;
      case "OURO":
        return subtotal * 0.05;
      default:
        return 0;
    }
  }
}
