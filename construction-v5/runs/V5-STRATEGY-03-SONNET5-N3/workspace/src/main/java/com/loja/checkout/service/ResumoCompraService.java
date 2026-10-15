package com.loja.checkout.service;

import com.loja.checkout.dominio.ContextoPedido;
import com.loja.checkout.dominio.Registro;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.AjustePagamento;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dto.ItemDTO;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ResumoCompraService {

    private static final Map<String, BigDecimal> PERCENTUAL_SEGURO_POR_REGIAO = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    private final Registro<ModalidadeEntrega> modalidades;
    private final Registro<Cupom> cupons;
    private final Registro<NivelClube> niveisClube;
    private final Registro<FormaPagamento> formasPagamento;

    public ResumoCompraService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                                List<NivelClube> niveisClube, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Registro<>(modalidades, ModalidadeEntrega::codigo,
                () -> new PedidoInvalidoException(CodigoErro.MODALIDADE_INVALIDA));
        this.cupons = new Registro<>(cupons, Cupom::codigo,
                () -> new PedidoInvalidoException(CodigoErro.CUPOM_INVALIDO));
        this.niveisClube = new Registro<>(niveisClube, NivelClube::codigo,
                () -> new PedidoInvalidoException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        this.formasPagamento = new Registro<>(formasPagamento, FormaPagamento::codigo,
                () -> new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());
        NivelClube nivelClube = niveisClube.buscar(pedido.nivelClube());
        BigDecimal percentualSeguro = buscarPercentualSeguro(pedido.regiao());
        ModalidadeEntrega modalidadeEntrega = modalidades.buscar(pedido.modalidadeEntrega());

        BigDecimal subtotalProdutos = calcularSubtotal(pedido.itens());
        BigDecimal pesoTotalKg = calcularPesoTotal(pedido.itens());
        ContextoPedido contexto = new ContextoPedido(pedido.itens(), subtotalProdutos);

        modalidadeEntrega.validarDisponibilidade(pesoTotalKg);
        BigDecimal freteBase = modalidadeEntrega.calcularFrete(pesoTotalKg);
        BigDecimal frete = Dinheiro.arredondar(nivelClube.freteGratis() ? BigDecimal.ZERO : freteBase);

        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (pedido.cupom() != null) {
            Cupom cupom = cupons.buscar(pedido.cupom());
            cupom.validarAplicavel(contexto);
            descontoCupom = Dinheiro.arredondar(cupom.calcularDesconto(contexto, frete));
        }

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(percentualSeguro));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = formasPagamento.buscar(pedido.formaPagamento());
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        formaPagamento.validarParcelas(parcelas);
        formaPagamento.validarDisponibilidade(totalPedido);
        AjustePagamento ajustePagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(
                subtotalProdutos.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.concedeBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidadeEntrega.prazoDias(),
                seguro,
                Dinheiro.arredondar(ajustePagamento.ajuste()),
                Dinheiro.arredondar(ajustePagamento.totalFinal()),
                parcelas,
                Dinheiro.arredondar(ajustePagamento.valorParcela()),
                creditoProximaCompra,
                brinde
        );
    }

    private void validarItens(List<ItemDTO> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemDTO item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private BigDecimal buscarPercentualSeguro(String regiao) {
        BigDecimal percentual = regiao == null ? null : PERCENTUAL_SEGURO_POR_REGIAO.get(regiao);
        if (percentual == null) {
            throw new PedidoInvalidoException(CodigoErro.REGIAO_INVALIDA);
        }
        return percentual;
    }

    private BigDecimal calcularSubtotal(List<ItemDTO> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemDTO> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
