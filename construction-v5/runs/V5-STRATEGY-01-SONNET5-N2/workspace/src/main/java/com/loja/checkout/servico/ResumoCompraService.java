package com.loja.checkout.servico;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.clube.NivelClubeRegistry;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.CupomRegistry;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.dominio.regiao.Regiao;
import com.loja.checkout.erro.PedidoException;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ResumoCompraService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final NivelClubeRegistry nivelClubeRegistry;
    private final FormaPagamentoRegistry formaPagamentoRegistry;

    public ResumoCompraService(
            ModalidadeEntregaRegistry modalidadeEntregaRegistry,
            CupomRegistry cupomRegistry,
            NivelClubeRegistry nivelClubeRegistry,
            FormaPagamentoRegistry formaPagamentoRegistry
    ) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.nivelClubeRegistry = nivelClubeRegistry;
        this.formaPagamentoRegistry = formaPagamentoRegistry;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<Item> itens = converterItens(request.itens());
        if (itens.isEmpty() || itens.stream().anyMatch(item -> !item.valido())) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }

        NivelClube nivelClube = nivelClubeRegistry.buscar(request.nivelClube())
                .orElseThrow(() -> new PedidoException("NIVEL_CLUBE_INVALIDO"));

        Regiao regiao = buscarRegiao(request.regiao());

        ModalidadeEntrega modalidadeEntrega = modalidadeEntregaRegistry.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new PedidoException("MODALIDADE_INVALIDA"));

        double pesoTotalKg = itens.stream().mapToDouble(Item::pesoTotal).sum();
        if (!modalidadeEntrega.disponivel(pesoTotalKg)) {
            throw new PedidoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(
                itens.stream().map(Item::totalItem).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal freteBase = modalidadeEntrega.calcularFrete(pesoTotalKg);

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            cupom = cupomRegistry.buscar(request.cupom())
                    .orElseThrow(() -> new PedidoException("CUPOM_INVALIDO"));
            ContextoCupom contextoCupom = new ContextoCupom(itens, subtotalProdutos, freteBase);
            if (!cupom.aplicavel(contextoCupom)) {
                throw new PedidoException("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento formaPagamento = formaPagamentoRegistry.buscar(request.formaPagamento())
                .orElseThrow(() -> new PedidoException("FORMA_PAGAMENTO_INVALIDA"));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new PedidoException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : Dinheiro.arredondar(cupom.calcularDesconto(new ContextoCupom(itens, subtotalProdutos, freteBase)));

        BigDecimal frete = nivelClube.isentaFrete() ? BigDecimal.ZERO.setScale(2) : freteBase;
        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivel(totalPedido, parcelas)) {
            throw new PedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidadeEntrega.prazoEntregaDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<Item> converterItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null) {
            return List.of();
        }
        return itensRequest.stream()
                .map(i -> new Item(
                        i.nome(),
                        i.precoUnitario(),
                        i.quantidade() == null ? 0 : i.quantidade(),
                        i.pesoKg() == null ? 0 : i.pesoKg()))
                .toList();
    }

    private Regiao buscarRegiao(String codigo) {
        if (codigo == null) {
            throw new PedidoException("REGIAO_INVALIDA");
        }
        return Optional.of(codigo)
                .map(c -> {
                    try {
                        return Regiao.valueOf(c);
                    } catch (IllegalArgumentException e) {
                        throw new PedidoException("REGIAO_INVALIDA");
                    }
                })
                .orElseThrow(() -> new PedidoException("REGIAO_INVALIDA"));
    }
}
