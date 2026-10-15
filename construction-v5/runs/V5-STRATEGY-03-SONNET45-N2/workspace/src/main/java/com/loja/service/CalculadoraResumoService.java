package com.loja.service;

import com.loja.clube.NivelClube;
import com.loja.clube.NivelClubeFactory;
import com.loja.cupom.Cupom;
import com.loja.cupom.CupomFactory;
import com.loja.domain.Item;
import com.loja.domain.Pedido;
import com.loja.domain.Resumo;
import com.loja.dto.ItemRequest;
import com.loja.dto.PedidoRequest;
import com.loja.exception.PedidoException;
import com.loja.modalidade.ModalidadeEntrega;
import com.loja.modalidade.ModalidadeEntregaFactory;
import com.loja.pagamento.FormaPagamento;
import com.loja.pagamento.FormaPagamentoFactory;
import com.loja.regiao.Regiao;
import com.loja.regiao.RegiaoFactory;
import com.loja.util.Arredondador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CalculadoraResumoService {

    public Resumo calcular(PedidoRequest request) {
        validarPedido(request);

        NivelClube nivelClube = NivelClubeFactory.criar(request.nivelClube());
        Regiao regiao = RegiaoFactory.criar(request.regiao());

        Pedido pedido = converterPedido(request);
        BigDecimal subtotalProdutos = Arredondador.arredondar(pedido.subtotalProdutos());

        ModalidadeEntrega modalidade = ModalidadeEntregaFactory.criar(request.modalidadeEntrega());
        validarModalidadeDisponivel(modalidade, pedido);

        BigDecimal freteCalculado = modalidade.calcularFrete(pedido);
        BigDecimal frete = nivelClube.ganhaFreteGratis() ? BigDecimal.ZERO.setScale(2) : freteCalculado;

        Cupom cupom = CupomFactory.criar(request.cupom());
        validarCupomAplicavel(cupom, pedido, frete);

        BigDecimal descontoCupom = cupom.calcularDesconto(pedido, frete);

        BigDecimal seguro = Arredondador.arredondar(
            subtotalProdutos.multiply(regiao.percentualSeguro())
        );

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(request.formaPagamento());

        validarParcelamento(formaPagamento, parcelas);
        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = Arredondador.arredondar(totalPedido.add(ajustePagamento));

        int numeroParcelasFinal = formaPagamento.numeroParcelas(parcelas);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalFinal, numeroParcelasFinal);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        return new Resumo(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.prazoEmDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            numeroParcelasFinal,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private void validarPedido(PedidoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PedidoException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, Pedido pedido) {
        if (!modalidade.aceita(pedido)) {
            throw new PedidoException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupomAplicavel(Cupom cupom, Pedido pedido, BigDecimal frete) {
        if (!cupom.aplicavel(pedido, frete)) {
            throw new PedidoException("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new PedidoException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.aceita(totalPedido)) {
            throw new PedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private Pedido converterPedido(PedidoRequest request) {
        List<Item> itens = request.itens().stream()
            .map(item -> new Item(
                item.nome(),
                item.precoUnitario(),
                item.quantidade(),
                item.pesoKg()
            ))
            .toList();
        return new Pedido(itens);
    }
}
