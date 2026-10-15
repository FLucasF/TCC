package com.loja.checkout.service;

import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ErrorResponse;
import com.loja.checkout.model.Item;
import com.loja.checkout.strategy.cupom.BemVindo10;
import com.loja.checkout.strategy.cupom.EstrategiaCupom;
import com.loja.checkout.strategy.cupom.FreteGratis;
import com.loja.checkout.strategy.cupom.Leve3Pague2;
import com.loja.checkout.strategy.cupom.Menos50;
import com.loja.checkout.strategy.entrega.EconomicaEntrega;
import com.loja.checkout.strategy.entrega.EstrategiaEntrega;
import com.loja.checkout.strategy.entrega.ExpressaEntrega;
import com.loja.checkout.strategy.entrega.MotoboyEntrega;
import com.loja.checkout.strategy.entrega.RetiradaLojaEntrega;
import com.loja.checkout.strategy.pagamento.BoletoPagamento;
import com.loja.checkout.strategy.pagamento.CartaoPagamento;
import com.loja.checkout.strategy.pagamento.EstrategiaPagamento;
import com.loja.checkout.strategy.pagamento.PixPagamento;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
  private RoundingService rounding;

  public CheckoutService(RoundingService rounding) {
    this.rounding = rounding;
  }

  public Object processar(CheckoutRequest req) {
    ErrorResponse erro = validar(req);
    if (erro != null) return erro;

    EstrategiaEntrega entrega = obterEstrategiaEntrega(req.modalidadeEntrega);
    EstrategiaCupom cupom = obterEstrategiaCupom(req.cupom);
    EstrategiaPagamento pagamento = obterEstrategiaPagamento(req.formaPagamento);

    double subtotal = calcularSubtotal(req.itens);
    double desconto = 0.0;
    boolean freteGratis = false;
    if (cupom != null) {
      desconto = cupom.calcularDesconto(req.itens, subtotal);
      freteGratis = cupom.aplicaFreteGratis();
    }
    desconto = rounding.round(desconto);

    double frete = 0.0;
    if (freteGratis || isClienteOuro(req.nivelClube)) {
      frete = 0.0;
    } else {
      frete = entrega.calcularFrete(req.itens);
    }
    frete = rounding.round(frete);

    double seguro = calcularSeguro(subtotal, req.regiao);
    seguro = rounding.round(seguro);

    double totalPedido = rounding.round(subtotal - desconto + frete + seguro);

    int parcelas = req.parcelas != null ? req.parcelas : 1;
    double ajuste = pagamento.calcularAjuste(totalPedido, parcelas);
    ajuste = rounding.round(ajuste);

    double totalFinal = rounding.round(totalPedido + ajuste);

    double valorParcela = rounding.round(totalFinal / parcelas);

    double credito = calcularCredito(subtotal, req.nivelClube);
    credito = rounding.round(credito);

    boolean brinde = isClienteOuro(req.nivelClube) && subtotal > 500.0;

    CheckoutResponse resp = new CheckoutResponse();
    resp.subtotalProdutos = subtotal;
    resp.descontoCupom = desconto;
    resp.frete = frete;
    resp.prazoEntregaDias = entrega.prazoEntregaDias();
    resp.seguro = seguro;
    resp.ajustePagamento = ajuste;
    resp.totalFinal = totalFinal;
    resp.parcelas = parcelas;
    resp.valorParcela = valorParcela;
    resp.creditoProximaCompra = credito;
    resp.brinde = brinde;

    return resp;
  }

  private ErrorResponse validar(CheckoutRequest req) {
    if (req.itens == null || req.itens.isEmpty()) {
      return new ErrorResponse("PEDIDO_INVALIDO");
    }

    for (Item item : req.itens) {
      if (item.precoUnitario <= 0 || item.quantidade <= 0 || item.pesoKg < 0) {
        return new ErrorResponse("PEDIDO_INVALIDO");
      }
    }

    if (req.nivelClube == null || !validarNivelClube(req.nivelClube)) {
      return new ErrorResponse("NIVEL_CLUBE_INVALIDO");
    }

    if (req.regiao == null || !validarRegiao(req.regiao)) {
      return new ErrorResponse("REGIAO_INVALIDA");
    }

    if (req.modalidadeEntrega == null || !validarModalidade(req.modalidadeEntrega)) {
      return new ErrorResponse("MODALIDADE_INVALIDA");
    }

    EstrategiaEntrega entrega = obterEstrategiaEntrega(req.modalidadeEntrega);
    if (!entrega.disponivel(req.itens)) {
      return new ErrorResponse("MODALIDADE_INDISPONIVEL");
    }

    if (req.cupom != null && !validarCupom(req.cupom)) {
      return new ErrorResponse("CUPOM_INVALIDO");
    }

    if (req.cupom != null) {
      double subtotal = calcularSubtotal(req.itens);
      EstrategiaCupom estrategiaCupom = obterEstrategiaCupom(req.cupom);
      if (!estrategiaCupom.aplicavel(req.itens, subtotal)) {
        return new ErrorResponse("CUPOM_NAO_APLICAVEL");
      }
    }

    if (req.formaPagamento == null || !validarFormaPagamento(req.formaPagamento)) {
      return new ErrorResponse("FORMA_PAGAMENTO_INVALIDA");
    }

    int parcelas = req.parcelas != null ? req.parcelas : 1;
    EstrategiaPagamento pagamento = obterEstrategiaPagamento(req.formaPagamento);
    if (!pagamento.parcelasValidas(parcelas)) {
      return new ErrorResponse("PARCELAMENTO_INVALIDO");
    }

    double subtotal = calcularSubtotal(req.itens);
    double desconto = 0.0;
    if (req.cupom != null) {
      EstrategiaCupom cupom = obterEstrategiaCupom(req.cupom);
      desconto = cupom.calcularDesconto(req.itens, subtotal);
    }
    desconto = rounding.round(desconto);

    double frete = 0.0;
    if (req.cupom != null && obterEstrategiaCupom(req.cupom).aplicaFreteGratis()) {
      frete = 0.0;
    } else if (!isClienteOuro(req.nivelClube)) {
      EstrategiaEntrega e = obterEstrategiaEntrega(req.modalidadeEntrega);
      frete = e.calcularFrete(req.itens);
    }
    frete = rounding.round(frete);

    double seguro = calcularSeguro(subtotal, req.regiao);
    seguro = rounding.round(seguro);

    double totalPedido = rounding.round(subtotal - desconto + frete + seguro);

    if (!pagamento.disponivel(totalPedido)) {
      return new ErrorResponse("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    return null;
  }

  private boolean validarNivelClube(String nivel) {
    return nivel.equals("BRONZE") || nivel.equals("PRATA") || nivel.equals("OURO");
  }

  private boolean validarRegiao(String regiao) {
    return regiao.equals("SUDESTE") || regiao.equals("SUL") || regiao.equals("CENTRO_OESTE")
        || regiao.equals("NORTE") || regiao.equals("NORDESTE");
  }

  private boolean validarModalidade(String modalidade) {
    return modalidade.equals("ECONOMICA") || modalidade.equals("EXPRESSA")
        || modalidade.equals("RETIRADA_LOJA") || modalidade.equals("MOTOBOY");
  }

  private boolean validarCupom(String cupom) {
    return cupom.equals("BEMVINDO10") || cupom.equals("MENOS50") || cupom.equals("FRETEGRATIS")
        || cupom.equals("LEVE3PAGUE2");
  }

  private boolean validarFormaPagamento(String forma) {
    return forma.equals("PIX") || forma.equals("CARTAO") || forma.equals("BOLETO");
  }

  private EstrategiaEntrega obterEstrategiaEntrega(String modalidade) {
    return switch (modalidade) {
      case "ECONOMICA" -> new EconomicaEntrega(rounding);
      case "EXPRESSA" -> new ExpressaEntrega(rounding);
      case "RETIRADA_LOJA" -> new RetiradaLojaEntrega();
      case "MOTOBOY" -> new MotoboyEntrega();
      default -> null;
    };
  }

  private EstrategiaCupom obterEstrategiaCupom(String cupom) {
    if (cupom == null) return null;
    return switch (cupom) {
      case "BEMVINDO10" -> new BemVindo10(rounding);
      case "MENOS50" -> new Menos50();
      case "FRETEGRATIS" -> new FreteGratis();
      case "LEVE3PAGUE2" -> new Leve3Pague2(rounding);
      default -> null;
    };
  }

  private EstrategiaPagamento obterEstrategiaPagamento(String forma) {
    return switch (forma) {
      case "PIX" -> new PixPagamento(rounding);
      case "CARTAO" -> new CartaoPagamento(rounding);
      case "BOLETO" -> new BoletoPagamento();
      default -> null;
    };
  }

  private double calcularSubtotal(java.util.List<Item> itens) {
    double subtotal = 0.0;
    for (Item item : itens) {
      subtotal += item.precoUnitario * item.quantidade;
    }
    return rounding.round(subtotal);
  }

  private double calcularSeguro(double subtotal, String regiao) {
    double percentual = switch (regiao) {
      case "SUDESTE" -> 0.01;
      case "SUL" -> 0.01;
      case "CENTRO_OESTE" -> 0.015;
      case "NORTE" -> 0.025;
      case "NORDESTE" -> 0.02;
      default -> 0.0;
    };
    return subtotal * percentual;
  }

  private double calcularCredito(double subtotal, String nivelClube) {
    return switch (nivelClube) {
      case "BRONZE" -> 0.0;
      case "PRATA" -> subtotal * 0.02;
      case "OURO" -> subtotal * 0.05;
      default -> 0.0;
    };
  }

  private boolean isClienteOuro(String nivelClube) {
    return "OURO".equals(nivelClube);
  }
}
