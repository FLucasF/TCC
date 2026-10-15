package com.loja.checkout.service;

import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.RegistroPorCodigo;
import com.loja.checkout.domain.clube.Clube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.domain.regiao.Regiao;
import com.loja.checkout.util.Dinheiro;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.PedidoException;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoCompraService {

    private final RegistroPorCodigo<ModalidadeEntrega> modalidades;
    private final RegistroPorCodigo<Cupom> cupons;
    private final RegistroPorCodigo<Clube> clubes;
    private final RegistroPorCodigo<FormaPagamento> formasPagamento;

    public ResumoCompraService(
            RegistroPorCodigo<ModalidadeEntrega> modalidades,
            RegistroPorCodigo<Cupom> cupons,
            RegistroPorCodigo<Clube> clubes,
            RegistroPorCodigo<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.clubes = clubes;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        List<Item> itens = validarItens(requisicao.itens());

        Clube clube = clubes.buscar(requisicao.nivelClube())
                .orElseThrow(() -> new PedidoException("NIVEL_CLUBE_INVALIDO"));

        Regiao regiao = validarRegiao(requisicao.regiao());

        ModalidadeEntrega modalidade = modalidades.buscar(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new PedidoException("MODALIDADE_INVALIDA"));

        BigDecimal subtotalProdutos = Dinheiro.arredondar(
                itens.stream().map(Item::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal pesoTotalKg = itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!modalidade.disponivelPara(pesoTotalKg)) {
            throw new PedidoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal freteDaModalidade = Dinheiro.arredondar(modalidade.calcularFrete(pesoTotalKg));
        BigDecimal frete = clube.isentaFrete() ? BigDecimal.ZERO.setScale(2) : freteDaModalidade;

        Cupom cupom = null;
        if (requisicao.cupom() != null) {
            cupom = cupons.buscar(requisicao.cupom())
                    .orElseThrow(() -> new PedidoException("CUPOM_INVALIDO"));
            if (!cupom.aplicavel(itens, subtotalProdutos)) {
                throw new PedidoException("CUPOM_NAO_APLICAVEL");
            }
        }
        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : Dinheiro.arredondar(cupom.calcularDesconto(itens, subtotalProdutos, frete));

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = formasPagamento.buscar(requisicao.formaPagamento())
                .orElseThrow(() -> new PedidoException("FORMA_PAGAMENTO_INVALIDA"));

        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new PedidoException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new PedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(clube.calcularCredito(subtotalProdutos));
        boolean brinde = clube.concedeBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private List<Item> validarItens(List<ItemRequest> itensRequisicao) {
        if (itensRequisicao == null || itensRequisicao.isEmpty()) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }
        return itensRequisicao.stream().map(this::validarItem).toList();
    }

    private Item validarItem(ItemRequest itemRequisicao) {
        if (itemRequisicao.precoUnitario() == null || itemRequisicao.precoUnitario().signum() <= 0
                || itemRequisicao.quantidade() == null || itemRequisicao.quantidade() <= 0
                || itemRequisicao.pesoKg() == null || itemRequisicao.pesoKg().signum() <= 0) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }
        return new Item(itemRequisicao.nome(), itemRequisicao.precoUnitario(), itemRequisicao.quantidade(),
                itemRequisicao.pesoKg());
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null) {
            throw new PedidoException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException excecao) {
            throw new PedidoException("REGIAO_INVALIDA");
        }
    }
}
