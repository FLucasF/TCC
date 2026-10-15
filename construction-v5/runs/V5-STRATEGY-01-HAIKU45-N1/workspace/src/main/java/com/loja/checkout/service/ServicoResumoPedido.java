package com.loja.checkout.service;

import com.loja.checkout.coupon.Cupom;
import com.loja.checkout.coupon.CupomFreteGratis;
import com.loja.checkout.coupon.ResolvedorCupom;
import com.loja.checkout.delivery.ModalidadeEntrega;
import com.loja.checkout.delivery.ResolvedorModalidadeEntrega;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.RequisicaoPedido;
import com.loja.checkout.dto.RespostaPedido;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.util.ArredondadorFinanceiro;
import com.loja.checkout.validation.ValidadorPedido;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicoResumoPedido {
    private final ValidadorPedido validador;

    public ServicoResumoPedido() {
        this.validador = new ValidadorPedido();
    }

    public Object calcularResumo(RequisicaoPedido req) {
        String erro = validador.validar(req);
        if (erro != null) {
            return new com.loja.checkout.dto.RespostaErro(erro);
        }

        double subtotalProdutos = calcularSubtotal(req.getItens());
        subtotalProdutos = ArredondadorFinanceiro.arredondarCentavos(subtotalProdutos);

        Cupom cupom = ResolvedorCupom.resolver(req.getCupom());
        double descontoCupom = 0.0;
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, req.getItens());
            descontoCupom = ArredondadorFinanceiro.arredondarCentavos(descontoCupom);
        }

        ModalidadeEntrega entrega = ResolvedorModalidadeEntrega.resolver(req.getModalidadeEntrega());
        double pesoTotal = calcularPesoTotal(req.getItens());
        double frete = entrega.calcularFrete(pesoTotal);
        frete = ArredondadorFinanceiro.arredondarCentavos(frete);
        int prazoEntregaDias = entrega.getPrazoEntregaDias();

        NivelClube nivel = NivelClube.de(req.getNivelClube());
        if (nivel.isIsentoFrete()) {
            frete = 0.0;
        }

        if (cupom instanceof CupomFreteGratis) {
            ((CupomFreteGratis) cupom).setFreteValue(frete);
            descontoCupom = frete;
            descontoCupom = ArredondadorFinanceiro.arredondarCentavos(descontoCupom);
        }

        Regiao regiao = Regiao.de(req.getRegiao());
        double seguro = subtotalProdutos * regiao.getPercentualSeguro();
        seguro = ArredondadorFinanceiro.arredondarCentavos(seguro);

        double totalPedido = subtotalProdutos - descontoCupom + frete + seguro;
        totalPedido = ArredondadorFinanceiro.arredondarCentavos(totalPedido);

        FormaPagamento forma = FormaPagamento.de(req.getFormaPagamento());
        int parcelas = req.getParcelas() != null ? req.getParcelas() : 1;

        double ajustePagamento = 0.0;
        double totalFinal = totalPedido;
        double valorParcela = totalFinal;

        if (forma == FormaPagamento.PIX) {
            ajustePagamento = -totalPedido * 0.05;
            ajustePagamento = ArredondadorFinanceiro.arredondarCentavos(ajustePagamento);
            totalFinal = totalPedido + ajustePagamento;
            totalFinal = ArredondadorFinanceiro.arredondarCentavos(totalFinal);
            valorParcela = totalFinal;
        } else if (forma == FormaPagamento.BOLETO) {
            ajustePagamento = 3.49;
            ajustePagamento = ArredondadorFinanceiro.arredondarCentavos(ajustePagamento);
            totalFinal = totalPedido + ajustePagamento;
            totalFinal = ArredondadorFinanceiro.arredondarCentavos(totalFinal);
            valorParcela = totalFinal;
        } else if (forma == FormaPagamento.CARTAO) {
            if (parcelas <= 3) {
                totalFinal = totalPedido;
                valorParcela = totalFinal / parcelas;
                valorParcela = ArredondadorFinanceiro.arredondarCentavos(valorParcela);
                totalFinal = valorParcela * parcelas;
                totalFinal = ArredondadorFinanceiro.arredondarCentavos(totalFinal);
            } else {
                double taxa = 0.0199;
                double denominador = 1.0 - Math.pow(1.0 + taxa, -parcelas);
                valorParcela = totalPedido * taxa / denominador;
                valorParcela = ArredondadorFinanceiro.arredondarCentavos(valorParcela);
                totalFinal = valorParcela * parcelas;
                totalFinal = ArredondadorFinanceiro.arredondarCentavos(totalFinal);
                ajustePagamento = totalFinal - totalPedido;
                ajustePagamento = ArredondadorFinanceiro.arredondarCentavos(ajustePagamento);
            }
        }

        double creditoProximaCompra = subtotalProdutos * nivel.getPercentualCredito();
        creditoProximaCompra = ArredondadorFinanceiro.arredondarCentavos(creditoProximaCompra);

        boolean brinde = false;
        if (nivel == NivelClube.OURO && subtotalProdutos > 500.00) {
            brinde = true;
        }

        return new RespostaPedido(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazoEntregaDias,
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private double calcularSubtotal(List<ItemPedido> itens) {
        return itens.stream()
            .mapToDouble(item -> item.getPrecoUnitario() * item.getQuantidade())
            .sum();
    }

    private double calcularPesoTotal(List<ItemPedido> itens) {
        return itens.stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();
    }
}
