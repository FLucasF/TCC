package com.loja.checkout.service;

import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Pedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.clube.NivelClube;
import com.loja.checkout.service.cupom.Cupom;
import com.loja.checkout.service.entrega.Modalidade;
import com.loja.checkout.service.pagamento.FormaPagamento;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CalculadoraResumo {
    private final ValidadorPedido validador;
    private final List<Modalidade> modalidades;
    private final List<Cupom> cupons;
    private final List<FormaPagamento> formasPagamento;
    private final List<NivelClube> niveisClube;

    public CalculadoraResumo(ValidadorPedido validador,
                             List<Modalidade> modalidades,
                             List<Cupom> cupons,
                             List<FormaPagamento> formasPagamento,
                             List<NivelClube> niveisClube) {
        this.validador = validador;
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
        this.niveisClube = niveisClube;
    }

    public ResumoResponse calcular(PedidoRequest request) {
        validador.validarItens(request);

        var nivel = validador.validarNivelClube(request.nivelClube(), niveisClube);
        var regiao = validador.validarRegiao(request.regiao());

        Pedido pedido = validador.converterParaPedido(request, regiao);

        var modalidade = validador.validarModalidade(request.modalidadeEntrega(), pedido, modalidades);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.itens());

        BigDecimal frete = nivel.temFreteGratis()
            ? BigDecimal.ZERO.setScale(2)
            : modalidade.calcularFrete(pedido);

        var cupom = validador.validarCupom(pedido.codigoCupom(), subtotalProdutos, frete, cupons);

        BigDecimal descontoCupom = cupom != null
            ? cupom.calcularDesconto(pedido, subtotalProdutos, frete)
            : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        var formaPagamento = validador.validarFormaPagamento(
            request.formaPagamento(),
            pedido.parcelas(),
            totalPedido,
            formasPagamento
        );

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, pedido.parcelas());
        BigDecimal totalFinal = formaPagamento.calcularValorFinal(totalPedido, pedido.parcelas());
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, pedido.parcelas());

        BigDecimal credito = nivel.calcularCredito(subtotalProdutos);
        boolean brinde = nivel.ganhaBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazoDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            pedido.parcelas(),
            valorParcela,
            credito,
            brinde
        );
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, com.loja.checkout.domain.Regiao regiao) {
        return subtotalProdutos.multiply(regiao.getPercentualSeguro())
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
