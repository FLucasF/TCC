package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

  private static final BigDecimal SCALE_2 = new BigDecimal("0.01");
  private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

  public CheckoutResponse calcularResumo(CheckoutRequest request) {
    validar(request);

    List<ItemRequest> itens = request.getItens();
    String cupomCode = request.getCupom();
    String modalidadeCode = request.getModalidadeEntrega();
    String formaPagamentoCode = request.getFormaPagamento();
    int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
    NivelClube nivelClube = NivelClube.valueOf(request.getNivelClube());
    Regiao regiao = request.getRegiao() != null ? Regiao.valueOf(request.getRegiao()) : null;
    ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(modalidadeCode);
    FormaPagamento formaPagamento = FormaPagamento.valueOf(formaPagamentoCode);

    // 1. Calcular subtotal dos produtos
    BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

    // 2. Calcular desconto do cupom
    BigDecimal descontoCupom = cupomCode != null ? calcularDescontoCupom(cupomCode, subtotalProdutos, itens) : BigDecimal.ZERO;
    descontoCupom = arredondar(descontoCupom);

    // 3. Calcular frete
    BigDecimal pesoTotal = calcularPesoTotal(itens);
    BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivelClube);
    frete = arredondar(frete);

    // 4. Calcular imposto (sobre produtos com desconto do cupom)
    BigDecimal baseImposto = subtotalProdutos.subtract(descontoCupom);
    BigDecimal imposto = regiao != null ? baseImposto.multiply(regiao.getImpostoPercentual()) : BigDecimal.ZERO;
    imposto = arredondar(imposto);

    // 5. Calcular total antes do ajuste de pagamento
    BigDecimal totalAntesPagamento = subtotalProdutos.subtract(descontoCupom).add(frete).add(imposto);

    // 6. Calcular ajuste de pagamento e total final
    BigDecimal[] pagamento = calcularAjustePagamento(formaPagamento, totalAntesPagamento, parcelas);
    BigDecimal ajustePagamento = pagamento[0];
    BigDecimal totalFinal = pagamento[1];
    BigDecimal valorParcela = pagamento[2];

    // 7. Calcular crédito para próxima compra (sobre produtos sem desconto, sem frete)
    BigDecimal creditoProximaCompra = subtotalProdutos.multiply(nivelClube.getCreditoPercentual());
    creditoProximaCompra = arredondar(creditoProximaCompra);

    // 8. Verificar se ganhou brinde (OURO com > 500 em produtos)
    Boolean brinde = nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;

    // Montar resposta
    CheckoutResponse response = new CheckoutResponse();
    response.setSubtotalProdutos(subtotalProdutos);
    response.setDescontoCupom(descontoCupom);
    response.setFrete(frete);
    response.setPrazoEntregaDias(modalidade.getPrazo());
    response.setImposto(imposto);
    response.setAjustePagamento(ajustePagamento);
    response.setTotalFinal(totalFinal);
    response.setParcelas(parcelas);
    response.setValorParcela(valorParcela);
    response.setCreditoProximaCompra(creditoProximaCompra);
    response.setBrinde(brinde);

    return response;
  }

  private void validar(CheckoutRequest request) {
    // 1. Carrinho e itens válidos
    if (request.getItens() == null || request.getItens().isEmpty()) {
      throw new CheckoutException("PEDIDO_INVALIDO");
    }

    for (ItemRequest item : request.getItens()) {
      if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
          item.getQuantidade() == null || item.getQuantidade() <= 0 ||
          item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
        throw new CheckoutException("PEDIDO_INVALIDO");
      }
    }

    // 2. Nível do clube válido
    if (request.getNivelClube() == null || !NivelClube.isValid(request.getNivelClube())) {
      throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
    }

    // 3. Região válida (se fornecida)
    if (request.getRegiao() != null && !Regiao.isValid(request.getRegiao())) {
      throw new CheckoutException("REGIAO_INVALIDA");
    }

    // 4. Modalidade de entrega válida
    if (request.getModalidadeEntrega() == null || !ModalidadeEntrega.isValid(request.getModalidadeEntrega())) {
      throw new CheckoutException("MODALIDADE_INVALIDA");
    }

    // 5. Modalidade de entrega disponível para o pedido
    ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
    BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
    if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
      throw new CheckoutException("MODALIDADE_INDISPONIVEL");
    }

    // 6. Cupom válido
    if (request.getCupom() != null && !Cupom.isValid(request.getCupom())) {
      throw new CheckoutException("CUPOM_INVALIDO");
    }

    // 7. Cupom aplicável ao pedido
    if (request.getCupom() != null) {
      Cupom cupom = Cupom.get(request.getCupom());
      BigDecimal subtotal = calcularSubtotalProdutos(request.getItens());

      if (cupom == Cupom.MENOS50 && subtotal.compareTo(new BigDecimal("300.00")) < 0) {
        throw new CheckoutException("CUPOM_NAO_APLICAVEL");
      }
    }

    // 8. Forma de pagamento válida
    if (request.getFormaPagamento() == null || !FormaPagamento.isValid(request.getFormaPagamento())) {
      throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
    }

    // 9. Parcelamento válido para a forma de pagamento
    int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
    FormaPagamento formaPagamento = FormaPagamento.valueOf(request.getFormaPagamento());

    if (formaPagamento == FormaPagamento.PIX && parcelas != 1) {
      throw new CheckoutException("PARCELAMENTO_INVALIDO");
    }
    if (formaPagamento == FormaPagamento.BOLETO && parcelas != 1) {
      throw new CheckoutException("PARCELAMENTO_INVALIDO");
    }
    if (formaPagamento == FormaPagamento.CARTAO && (parcelas < 1 || parcelas > 12)) {
      throw new CheckoutException("PARCELAMENTO_INVALIDO");
    }

    // 10. Forma de pagamento disponível para o pedido
    BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());
    BigDecimal descontoCupom = request.getCupom() != null ?
        calcularDescontoCupom(request.getCupom(), subtotalProdutos, request.getItens()) : BigDecimal.ZERO;
    descontoCupom = arredondar(descontoCupom);
    BigDecimal frete = calcularFrete(modalidade, pesoTotal, NivelClube.valueOf(request.getNivelClube()));
    frete = arredondar(frete);
    BigDecimal totalAntesPagamento = subtotalProdutos.subtract(descontoCupom).add(frete);

    if (formaPagamento == FormaPagamento.BOLETO && totalAntesPagamento.compareTo(new BigDecimal("1000.00")) > 0) {
      throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
    }
  }

  private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
    BigDecimal subtotal = BigDecimal.ZERO;
    for (ItemRequest item : itens) {
      BigDecimal itemTotal = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
      subtotal = subtotal.add(itemTotal);
    }
    return arredondar(subtotal);
  }

  private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
    BigDecimal peso = BigDecimal.ZERO;
    for (ItemRequest item : itens) {
      BigDecimal itemPeso = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
      peso = peso.add(itemPeso);
    }
    return peso;
  }

  private BigDecimal calcularDescontoCupom(String cupomCode, BigDecimal subtotal, List<ItemRequest> itens) {
    Cupom cupom = Cupom.get(cupomCode);

    if (cupom == Cupom.BEMVINDO10) {
      return subtotal.multiply(new BigDecimal("0.10"));
    } else if (cupom == Cupom.MENOS50) {
      return new BigDecimal("50.00");
    } else if (cupom == Cupom.FRETEGRATIS) {
      return BigDecimal.ZERO;
    } else if (cupom == Cupom.LEVE3PAGUE2) {
      BigDecimal desconto = BigDecimal.ZERO;
      for (ItemRequest item : itens) {
        int quantidade = item.getQuantidade();
        int gratuitos = quantidade / 3;
        BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(gratuitos));
        desconto = desconto.add(descontoItem);
      }
      return desconto;
    }

    return BigDecimal.ZERO;
  }

  private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, NivelClube nivelClube) {
    if (nivelClube == NivelClube.OURO) {
      return BigDecimal.ZERO;
    }

    BigDecimal base = modalidade.getBasePrice();
    BigDecimal pesoPrice = modalidade.getPricePerKg().multiply(pesoTotal);
    return base.add(pesoPrice);
  }

  private BigDecimal[] calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal total, int parcelas) {
    BigDecimal ajuste;
    BigDecimal totalFinal;
    BigDecimal valorParcela;

    if (formaPagamento == FormaPagamento.PIX) {
      ajuste = total.multiply(new BigDecimal("0.05")).negate();
      ajuste = arredondar(ajuste);
      totalFinal = total.add(ajuste);
      totalFinal = arredondar(totalFinal);
      valorParcela = totalFinal;
    } else if (formaPagamento == FormaPagamento.BOLETO) {
      BigDecimal taxa = new BigDecimal("3.49");
      ajuste = taxa;
      totalFinal = total.add(ajuste);
      totalFinal = arredondar(totalFinal);
      valorParcela = totalFinal;
    } else { // CARTAO
      if (parcelas <= 3) {
        ajuste = arredondar(BigDecimal.ZERO);
        totalFinal = total;
        valorParcela = totalFinal.divide(new BigDecimal(parcelas), 2, ROUNDING_MODE);
      } else {
        BigDecimal taxaMensal = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 10, ROUNDING_MODE));
        BigDecimal parcela = total.multiply(taxaMensal).divide(divisor, 2, ROUNDING_MODE);
        valorParcela = parcela;
        totalFinal = parcela.multiply(new BigDecimal(parcelas));
        totalFinal = arredondar(totalFinal);
        ajuste = totalFinal.subtract(total);
        ajuste = arredondar(ajuste);
      }
    }

    return new BigDecimal[]{ajuste, totalFinal, valorParcela};
  }

  private BigDecimal arredondar(BigDecimal valor) {
    return valor.setScale(2, ROUNDING_MODE);
  }
}
