package com.loja.service;

import com.loja.dto.ItemCarrinho;
import com.loja.dto.PedidoRequest;
import com.loja.dto.ResumoResponse;
import com.loja.exception.CheckoutException;
import com.loja.model.Regiao;
import com.loja.model.clube.*;
import com.loja.model.cupom.*;
import com.loja.model.entrega.*;
import com.loja.model.pagamento.*;
import com.loja.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CalculadoraResumo {

    private final Map<String, ModalidadeEntrega> modalidades = Map.of(
        "ECONOMICA", new EntregaEconomica(),
        "EXPRESSA", new EntregaExpressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    private final Map<String, Cupom> cupons = Map.of(
        "BEMVINDO10", new BemVindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    private final Map<String, NivelClube> niveisClube = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    private final Map<String, FormaPagamento> formasPagamento = Map.of(
        "PIX", new Pix(),
        "BOLETO", new Boleto(),
        "CARTAO", new Cartao()
    );

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarPedido(pedido);

        NivelClube nivelClube = obterNivelClube(pedido.nivelClube());
        Regiao regiao = obterRegiao(pedido.regiao());
        ModalidadeEntrega modalidade = obterModalidadeEntrega(pedido.modalidadeEntrega());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.itens());
        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());

        validarModalidadeDisponivel(modalidade, pesoTotal);

        BigDecimal freteCalculado = modalidade.calcularFrete(pesoTotal);
        BigDecimal frete = nivelClube.temFreteGratis() ? new BigDecimal("0.00") : freteCalculado;

        Cupom cupom = obterCupom(pedido.cupom());
        BigDecimal descontoCupom = new BigDecimal("0.00");

        if (cupom != null) {
            validarCupomAplicavel(cupom, subtotalProdutos);
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, pedido.itens(), frete);
        }

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = obterFormaPagamento(pedido.formaPagamento());
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

        validarParcelamento(formaPagamento, parcelas);
        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        BigDecimal totalFinal = formaPagamento.calcularTotalFinal(totalPedido, parcelas);
        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);
        BigDecimal valorParcela = Dinheiro.arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazoDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private void validarPedido(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : pedido.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube obterNivelClube(String codigo) {
        if (codigo == null || !niveisClube.containsKey(codigo)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return niveisClube.get(codigo);
    }

    private Regiao obterRegiao(String codigo) {
        if (codigo == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega obterModalidadeEntrega(String codigo) {
        if (codigo == null || !modalidades.containsKey(codigo)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return modalidades.get(codigo);
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.aceitaPedido(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom obterCupom(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }
        if (!cupons.containsKey(codigo)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        return cupons.get(codigo);
    }

    private void validarCupomAplicavel(Cupom cupom, BigDecimal subtotalProdutos) {
        if (!cupom.aplicavel(subtotalProdutos)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null || !formasPagamento.containsKey(codigo)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        return formasPagamento.get(codigo);
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.aceitaPedido(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = new BigDecimal("0.00");
        for (ItemCarrinho item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = new BigDecimal("0.00");
        for (ItemCarrinho item : itens) {
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return pesoTotal;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));
    }
}
