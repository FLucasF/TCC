package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.NiveisClubeRegistry;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CuponsRegistry;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.OpcaoEntrega;
import com.loja.checkout.entrega.OpcoesEntregaRegistry;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.ResumoCompraException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {

    private final OpcoesEntregaRegistry opcoesEntregaRegistry;
    private final CuponsRegistry cuponsRegistry;
    private final NiveisClubeRegistry niveisClubeRegistry;
    private final FormasPagamentoRegistry formasPagamentoRegistry;

    public CheckoutService(OpcoesEntregaRegistry opcoesEntregaRegistry,
                            CuponsRegistry cuponsRegistry,
                            NiveisClubeRegistry niveisClubeRegistry,
                            FormasPagamentoRegistry formasPagamentoRegistry) {
        this.opcoesEntregaRegistry = opcoesEntregaRegistry;
        this.cuponsRegistry = cuponsRegistry;
        this.niveisClubeRegistry = niveisClubeRegistry;
        this.formasPagamentoRegistry = formasPagamentoRegistry;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = validarPedido(requisicao.itens());

        NivelClube nivelClube = niveisClubeRegistry.buscar(requisicao.nivelClube())
                .orElseThrow(() -> new ResumoCompraException(CodigoErro.NIVEL_CLUBE_INVALIDO));

        Regiao regiao = parseRegiao(requisicao.regiao());

        OpcaoEntrega opcaoEntrega = opcoesEntregaRegistry.buscar(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new ResumoCompraException(CodigoErro.MODALIDADE_INVALIDA));
        if (!opcaoEntrega.disponivel(pedido)) {
            throw new ResumoCompraException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        Optional<Cupom> cupom = buscarCupom(requisicao.cupom());
        cupom.ifPresent(c -> {
            if (!c.aplicavel(pedido)) {
                throw new ResumoCompraException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
        });

        FormaPagamento formaPagamento = formasPagamentoRegistry.buscar(requisicao.formaPagamento())
                .orElseThrow(() -> new ResumoCompraException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new ResumoCompraException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(pedido.subtotalProdutos());

        BigDecimal freteBase = Dinheiro.arredondar(opcaoEntrega.custo(pedido));
        BigDecimal frete = nivelClube.freteGratis() ? BigDecimal.ZERO : freteBase;

        BigDecimal descontoCupom = cupom
                .map(c -> Dinheiro.arredondar(c.desconto(pedido, frete)))
                .orElse(BigDecimal.ZERO);

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new ResumoCompraException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(nivelClube.credito(subtotalProdutos));
        boolean brinde = nivelClube.brinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                opcaoEntrega.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private Pedido validarPedido(List<ItemRequest> itensRequisicao) {
        if (itensRequisicao == null || itensRequisicao.isEmpty()) {
            throw new ResumoCompraException(CodigoErro.PEDIDO_INVALIDO);
        }

        List<ItemPedido> itens = itensRequisicao.stream()
                .map(this::validarItem)
                .toList();

        return new Pedido(itens);
    }

    private ItemPedido validarItem(ItemRequest item) {
        if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
            throw new ResumoCompraException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private Regiao parseRegiao(String regiao) {
        if (regiao == null) {
            throw new ResumoCompraException(CodigoErro.REGIAO_INVALIDA);
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new ResumoCompraException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private Optional<Cupom> buscarCupom(String codigoCupom) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(cuponsRegistry.buscar(codigoCupom)
                .orElseThrow(() -> new ResumoCompraException(CodigoErro.CUPOM_INVALIDO)));
    }
}
