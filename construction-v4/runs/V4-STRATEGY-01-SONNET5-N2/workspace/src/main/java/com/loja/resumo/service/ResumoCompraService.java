package com.loja.resumo.service;

import com.loja.resumo.clube.NivelClube;
import com.loja.resumo.clube.NivelClubeRegistry;
import com.loja.resumo.cupom.Cupom;
import com.loja.resumo.cupom.CupomRegistry;
import com.loja.resumo.dto.ItemRequest;
import com.loja.resumo.dto.ResumoRequest;
import com.loja.resumo.dto.ResumoResponse;
import com.loja.resumo.entrega.ModalidadeEntrega;
import com.loja.resumo.entrega.ModalidadeEntregaRegistry;
import com.loja.resumo.exception.ErroPedidoException;
import com.loja.resumo.model.ItemPedido;
import com.loja.resumo.pagamento.AjustePagamento;
import com.loja.resumo.pagamento.FormaPagamento;
import com.loja.resumo.pagamento.FormaPagamentoRegistry;
import com.loja.resumo.regiao.Regiao;
import com.loja.resumo.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    private final ModalidadeEntregaRegistry modalidades;
    private final CupomRegistry cupons;
    private final NivelClubeRegistry niveis;
    private final FormaPagamentoRegistry formasPagamento;

    public ResumoCompraService(ModalidadeEntregaRegistry modalidades, CupomRegistry cupons,
            NivelClubeRegistry niveis, FormaPagamentoRegistry formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemPedido> itens = validarItens(request.itens());
        BigDecimal subtotalProdutos = Arredondamento.paraCentavos(calcularSubtotal(itens));
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        NivelClube nivel = niveis.buscar(request.nivelClube())
                .orElseThrow(() -> new ErroPedidoException("NIVEL_CLUBE_INVALIDO"));

        BigDecimal percentualSeguro = buscarPercentualSeguro(request.regiao());

        ModalidadeEntrega modalidade = modalidades.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new ErroPedidoException("MODALIDADE_INVALIDA"));
        if (!modalidade.disponivelPara(pesoTotal)) {
            throw new ErroPedidoException("MODALIDADE_INDISPONIVEL");
        }
        BigDecimal frete = nivel.isentoFrete()
                ? BigDecimal.ZERO.setScale(2)
                : Arredondamento.paraCentavos(modalidade.calcularFrete(pesoTotal));

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = cupons.buscar(request.cupom())
                    .orElseThrow(() -> new ErroPedidoException("CUPOM_INVALIDO"));
            if (!cupom.aplicavel(itens, subtotalProdutos)) {
                throw new ErroPedidoException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = Arredondamento.paraCentavos(cupom.calcularDesconto(itens, subtotalProdutos, frete));
        }

        BigDecimal seguro = Arredondamento.paraCentavos(subtotalProdutos.multiply(percentualSeguro));

        BigDecimal totalPedido = Arredondamento.paraCentavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = formasPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> new ErroPedidoException("FORMA_PAGAMENTO_INVALIDA"));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new ErroPedidoException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new ErroPedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        AjustePagamento ajustePagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Arredondamento.paraCentavos(
                subtotalProdutos.multiply(nivel.percentualCredito()));
        boolean brinde = nivel.concedeBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento.ajuste(),
                ajustePagamento.totalFinal(),
                parcelas,
                ajustePagamento.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new ErroPedidoException("PEDIDO_INVALIDO");
        }
        return itensRequest.stream().map(this::validarItem).toList();
    }

    private ItemPedido validarItem(ItemRequest item) {
        if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ErroPedidoException("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        return itens.stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        return itens.stream()
                .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal buscarPercentualSeguro(String regiao) {
        if (regiao == null) {
            throw new ErroPedidoException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao).percentualSeguro();
        } catch (IllegalArgumentException e) {
            throw new ErroPedidoException("REGIAO_INVALIDA");
        }
    }
}
