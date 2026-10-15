package com.loja.resumo.servico;

import com.loja.resumo.clube.NivelClube;
import com.loja.resumo.cupom.Cupom;
import com.loja.resumo.dinheiro.Dinheiro;
import com.loja.resumo.dominio.Item;
import com.loja.resumo.dominio.Pedido;
import com.loja.resumo.dto.ItemRequest;
import com.loja.resumo.dto.ResumoRequest;
import com.loja.resumo.dto.ResumoResponse;
import com.loja.resumo.entrega.ModalidadeEntrega;
import com.loja.resumo.erro.CodigoErro;
import com.loja.resumo.erro.PedidoException;
import com.loja.resumo.pagamento.FormaPagamento;
import com.loja.resumo.pagamento.ResultadoPagamento;
import com.loja.resumo.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = montarPedido(request);

        NivelClube nivelClube = NivelClube.encontrar(request.nivelClube())
                .orElseThrow(() -> new PedidoException(CodigoErro.NIVEL_CLUBE_INVALIDO));

        Regiao regiao = Regiao.encontrar(request.regiao())
                .orElseThrow(() -> new PedidoException(CodigoErro.REGIAO_INVALIDA));

        ModalidadeEntrega modalidadeEntrega = ModalidadeEntrega.encontrar(request.modalidadeEntrega())
                .orElseThrow(() -> new PedidoException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidadeEntrega.disponivelPara(pedido)) {
            throw new PedidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        Optional<Cupom> cupom = encontrarCupom(request.cupom());

        FormaPagamento formaPagamento = FormaPagamento.encontrar(request.formaPagamento())
                .orElseThrow(() -> new PedidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new PedidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal freteNominal = modalidadeEntrega.calcularFrete(pedido);
        BigDecimal freteExibido = nivelClube.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteNominal;

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom.isPresent()) {
            if (!cupom.get().aplicavel(pedido, freteExibido)) {
                throw new PedidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.get().desconto(pedido, freteExibido);
        }

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(freteExibido).add(seguro));

        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new PedidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = nivelClube.credito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                freteExibido,
                modalidadeEntrega.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private Optional<Cupom> encontrarCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Cupom.encontrar(codigo)
                .orElseThrow(() -> new PedidoException(CodigoErro.CUPOM_INVALIDO)));
    }

    private Pedido montarPedido(ResumoRequest request) {
        List<ItemRequest> itensRequest = request.itens();
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new PedidoException(CodigoErro.PEDIDO_INVALIDO);
        }

        List<Item> itens = itensRequest.stream()
                .map(this::validarEConverter)
                .toList();

        return new Pedido(itens);
    }

    private Item validarEConverter(ItemRequest itemRequest) {
        if (itemRequest.precoUnitario() == null || itemRequest.precoUnitario().signum() <= 0
                || itemRequest.quantidade() == null || itemRequest.quantidade() <= 0
                || itemRequest.pesoKg() == null || itemRequest.pesoKg().signum() <= 0) {
            throw new PedidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(itemRequest.nome(), itemRequest.precoUnitario(), itemRequest.quantidade(),
                itemRequest.pesoKg());
    }
}
