package com.loja.checkout.servico;

import com.loja.checkout.dto.Item;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.RespostaCheckout;
import com.loja.checkout.exception.ErroCheckout;
import com.loja.checkout.modelo.Cupom;
import com.loja.checkout.modelo.FormaPagamento;
import com.loja.checkout.modelo.ModalidadeEntrega;
import com.loja.checkout.modelo.NivelClube;
import com.loja.checkout.modelo.Regiao;
import com.loja.checkout.util.Arredondador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CalculadoraCheckout {

    public RespostaCheckout calcular(RequisicaoCheckout req) {
        validarRequisicao(req);

        List<Item> itens = req.getItens();
        NivelClube nivelClube = NivelClube.valueOf(req.getNivelClube());
        Regiao regiao = req.getRegiao() != null ? Regiao.valueOf(req.getRegiao()) : null;
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(req.getModalidadeEntrega());
        FormaPagamento formaPagamento = FormaPagamento.valueOf(req.getFormaPagamento());
        Integer parcelas = req.getParcelas() != null ? req.getParcelas() : 1;

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens, req.getCupom());
        BigDecimal freteTotal = calcularFrete(itens, modalidade, nivelClube);
        BigDecimal descontoCupom = calcularDescontoCupom(itens, subtotalProdutos, req.getCupom(), freteTotal);
        BigDecimal produtosComDesconto = subtotalProdutos.subtract(descontoCupom);

        int prazo = modalidade.getPrazo();

        BigDecimal imposto = calcularImposto(produtosComDesconto, regiao);

        BigDecimal totalAntesAjuste = produtosComDesconto.add(freteTotal).add(imposto);
        BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, totalAntesAjuste, parcelas);
        BigDecimal totalFinal = totalAntesAjuste.add(ajustePagamento);

        BigDecimal creditoProximaCompra = calcularCredito(subtotalProdutos, nivelClube);
        boolean brinde = verificarBrinde(subtotalProdutos, nivelClube);

        RespostaCheckout resposta = new RespostaCheckout();
        resposta.setSubtotalProdutos(subtotalProdutos);
        resposta.setDescontoCupom(descontoCupom);
        resposta.setFrete(freteTotal);
        resposta.setPrazoEntregaDias(prazo);
        resposta.setImposto(imposto);
        resposta.setAjustePagamento(ajustePagamento);
        resposta.setTotalFinal(totalFinal);
        resposta.setParcelas(parcelas);
        resposta.setValorParcela(calcularValorParcela(totalFinal, formaPagamento, parcelas));
        resposta.setCreditoProximaCompra(creditoProximaCompra);
        resposta.setBrinde(brinde);

        return resposta;
    }

    private void validarRequisicao(RequisicaoCheckout req) {
        validarItens(req.getItens());
        validarNivelClube(req.getNivelClube());
        validarRegiao(req.getRegiao());
        validarModalidadeEntrega(req.getModalidadeEntrega(), req.getItens());
        validarCupom(req.getCupom(), req.getItens());
        validarFormaPagamento(req.getFormaPagamento());
        validarParcelamento(req.getFormaPagamento(), req.getParcelas());
        validarDisponibilidadeFormaPagamento(req.getFormaPagamento(), req.getItens(), req.getModalidadeEntrega(), req.getCupom());
    }

    private void validarItens(List<Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        for (Item item : itens) {
            if (item.getPrecoUnitario() == null || item.getQuantidade() == null || item.getPesoKg() == null) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            if (item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() <= 0 ||
                item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarNivelClube(String nivelClube) {
        if (nivelClube == null) {
            throw new ErroCheckout("NIVEL_CLUBE_INVALIDO");
        }
        try {
            NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("NIVEL_CLUBE_INVALIDO");
        }
    }

    private void validarRegiao(String regiao) {
        if (regiao == null) {
            return;
        }
        try {
            Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("REGIAO_INVALIDA");
        }
    }

    private void validarModalidadeEntrega(String modalidade, List<Item> itens) {
        if (modalidade == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega mod = ModalidadeEntrega.fromString(modalidade);
        if (mod == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (!mod.eDisponivel(pesoTotal)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(String codigoCupom, List<Item> itens) {
        if (codigoCupom == null) {
            return;
        }
        Cupom cupom = Cupom.fromString(codigoCupom);
        if (cupom == null) {
            throw new ErroCheckout("CUPOM_INVALIDO");
        }
        BigDecimal subtotal = calcularSubtotalSemDesconto(itens);
        if (!cupom.podeAplicar(subtotal, itens)) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(String formaPagamento, Integer parcelas) {
        int numParcelas = parcelas != null ? parcelas : 1;
        if (numParcelas < 1 || numParcelas > 12) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        FormaPagamento forma = FormaPagamento.valueOf(formaPagamento);
        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (numParcelas != 1) {
                throw new ErroCheckout("PARCELAMENTO_INVALIDO");
            }
        } else if (forma == FormaPagamento.CARTAO) {
            if (numParcelas < 1 || numParcelas > 12) {
                throw new ErroCheckout("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarDisponibilidadeFormaPagamento(String formaPagamento, List<Item> itens,
                                                       String modalidade, String codigoCupom) {
        FormaPagamento forma = FormaPagamento.valueOf(formaPagamento);
        if (forma == FormaPagamento.BOLETO) {
            BigDecimal subtotal = calcularSubtotalSemDesconto(itens);
            BigDecimal desconto = codigoCupom != null ? Cupom.fromString(codigoCupom).calcularDesconto(subtotal, itens) : BigDecimal.ZERO;
            ModalidadeEntrega mod = ModalidadeEntrega.fromString(modalidade);
            BigDecimal frete = mod.calcularFrete(calcularPesoTotal(itens));
            BigDecimal total = subtotal.subtract(desconto).add(frete);
            total = Arredondador.arredondar(total);

            if (total.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularSubtotalSemDesconto(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal valorItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return Arredondador.arredondar(subtotal);
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens, String codigoCupom) {
        if (codigoCupom != null && codigoCupom.equals("LEVE3PAGUE2")) {
            return calcularSubtotalComLeve3Pague2(itens);
        }
        return calcularSubtotalSemDesconto(itens);
    }

    private BigDecimal calcularSubtotalComLeve3Pague2(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            int quantidade = item.getQuantidade();
            BigDecimal precoUnitario = item.getPrecoUnitario();

            int grupos = quantidade / 3;
            int itemsGratuitos = grupos;
            int itemsPagos = quantidade - itemsGratuitos;

            BigDecimal valorItem = precoUnitario.multiply(new BigDecimal(itemsPagos));
            subtotal = subtotal.add(valorItem);
        }
        return Arredondador.arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(List<Item> itens, BigDecimal subtotalProdutos, String codigoCupom, BigDecimal frete) {
        if (codigoCupom == null) {
            return Arredondador.arredondar(BigDecimal.ZERO);
        }
        if (codigoCupom.equals("FRETEGRATIS")) {
            return Arredondador.arredondar(frete);
        }
        Cupom cupom = Cupom.fromString(codigoCupom);
        if (cupom == null) {
            return Arredondador.arredondar(BigDecimal.ZERO);
        }
        BigDecimal desconto = cupom.calcularDesconto(subtotalProdutos, itens);
        return Arredondador.arredondar(desconto);
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal pesoItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            peso = peso.add(pesoItem);
        }
        return peso;
    }

    private BigDecimal calcularFrete(List<Item> itens, ModalidadeEntrega modalidade, NivelClube nivelClube) {
        if (nivelClube.isFreteGratis()) {
            return Arredondador.arredondar(BigDecimal.ZERO);
        }
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        return Arredondador.arredondar(frete);
    }

    private BigDecimal calcularImposto(BigDecimal produtosComDesconto, Regiao regiao) {
        if (regiao == null) {
            return Arredondador.arredondar(BigDecimal.ZERO);
        }
        BigDecimal imposto = produtosComDesconto.multiply(regiao.getAliquota());
        return Arredondador.arredondar(imposto);
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal total, Integer parcelas) {
        if (formaPagamento == FormaPagamento.PIX) {
            BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
            desconto = Arredondador.arredondar(desconto);
            return Arredondador.arredondar(desconto.negate());
        } else if (formaPagamento == FormaPagamento.BOLETO) {
            return Arredondador.arredondar(new BigDecimal("3.49"));
        } else if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas <= 3) {
                return Arredondador.arredondar(BigDecimal.ZERO);
            }
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal base = BigDecimal.ONE.add(taxaMensal);
            BigDecimal potencia = base;
            for (int i = 1; i < parcelas; i++) {
                potencia = potencia.multiply(base);
            }
            BigDecimal fator = BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN);
            BigDecimal divisor = BigDecimal.ONE.subtract(fator);
            BigDecimal numerador = BigDecimal.valueOf(parcelas).multiply(taxaMensal);
            BigDecimal taxaTotal = numerador.divide(divisor, 10, RoundingMode.HALF_EVEN);
            BigDecimal juros = total.multiply(taxaTotal.subtract(BigDecimal.ONE));
            juros = Arredondador.arredondar(juros);
            return juros;
        }
        return Arredondador.arredondar(BigDecimal.ZERO);
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, FormaPagamento formaPagamento, Integer parcelas) {
        if (parcelas == null || parcelas <= 0) {
            parcelas = 1;
        }
        BigDecimal valorParcela = totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN);
        return Arredondador.arredondar(valorParcela);
    }

    private BigDecimal calcularCredito(BigDecimal subtotalProdutos, NivelClube nivelClube) {
        if (nivelClube == null) {
            return Arredondador.arredondar(BigDecimal.ZERO);
        }
        BigDecimal credito = subtotalProdutos.multiply(nivelClube.getPercentualCredito());
        return Arredondador.arredondar(credito);
    }

    private boolean verificarBrinde(BigDecimal subtotalProdutos, NivelClube nivelClube) {
        if (nivelClube != NivelClube.OURO) {
            return false;
        }
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
