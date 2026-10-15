package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.comum.Arredondamento;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomContexto;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.seguro.AliquotaSeguro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResumoService {

    private final Map<String, ModalidadeEntrega> modalidadesEntrega;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(List<ModalidadeEntrega> modalidadesEntrega,
                          List<Cupom> cupons,
                          List<NivelClube> niveisClube,
                          List<FormaPagamento> formasPagamento) {
        this.modalidadesEntrega = indexarPorCodigo(modalidadesEntrega, ModalidadeEntrega::codigo);
        this.cupons = indexarPorCodigo(cupons, Cupom::codigo);
        this.niveisClube = indexarPorCodigo(niveisClube, NivelClube::codigo);
        this.formasPagamento = indexarPorCodigo(formasPagamento, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> indexarPorCodigo(List<T> itens, Function<T, String> codigo) {
        return itens.stream().collect(Collectors.toMap(codigo, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest pedido) {
        validarItens(pedido.itens());
        NivelClube nivel = buscarNivelClube(pedido.nivelClube());
        BigDecimal aliquotaSeguro = buscarAliquotaSeguro(pedido.regiao());
        ModalidadeEntrega modalidade = buscarModalidadeEntrega(pedido.modalidadeEntrega());

        BigDecimal pesoTotal = somarPeso(pedido.itens());
        if (!modalidade.disponivel(pesoTotal)) {
            throw new PedidoInvalidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        Cupom cupom = buscarCupomSeInformado(pedido.cupom());

        BigDecimal subtotalProdutos = Arredondamento.paraCentavos(somarSubtotal(pedido.itens()));
        BigDecimal frete = nivel.freteGratis()
                ? Arredondamento.ZERO_CENTAVOS
                : Arredondamento.paraCentavos(modalidade.custo(pesoTotal));

        CupomContexto contextoCupom = new CupomContexto(subtotalProdutos, pedido.itens(), frete);
        if (cupom != null && !cupom.aplicavel(contextoCupom)) {
            throw new PedidoInvalidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        FormaPagamento formaPagamento = buscarFormaPagamento(pedido.formaPagamento());
        int parcelas = pedido.parcelasOuPadrao();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new PedidoInvalidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal descontoCupom = cupom == null
                ? Arredondamento.ZERO_CENTAVOS
                : Arredondamento.paraCentavos(cupom.desconto(contextoCupom));

        BigDecimal seguro = Arredondamento.paraCentavos(subtotalProdutos.multiply(aliquotaSeguro));

        BigDecimal totalPedido = Arredondamento.paraCentavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultadoPagamento.valorFinal().subtract(totalPedido);

        BigDecimal credito = Arredondamento.paraCentavos(nivel.credito(subtotalProdutos));
        boolean brinde = nivel.brinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                resultadoPagamento.valorFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(item -> !item.valido())) {
            throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
        }
    }

    private NivelClube buscarNivelClube(String codigo) {
        NivelClube nivel = codigo == null ? null : niveisClube.get(codigo);
        if (nivel == null) {
            throw new PedidoInvalidoException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        return nivel;
    }

    private BigDecimal buscarAliquotaSeguro(String regiao) {
        BigDecimal aliquota = regiao == null ? null : AliquotaSeguro.para(regiao);
        if (aliquota == null) {
            throw new PedidoInvalidoException(CodigoErro.REGIAO_INVALIDA);
        }
        return aliquota;
    }

    private ModalidadeEntrega buscarModalidadeEntrega(String codigo) {
        ModalidadeEntrega modalidade = codigo == null ? null : modalidadesEntrega.get(codigo);
        if (modalidade == null) {
            throw new PedidoInvalidoException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return modalidade;
    }

    private Cupom buscarCupomSeInformado(String codigo) {
        if (codigo == null) {
            return null;
        }
        Cupom cupom = cupons.get(codigo);
        if (cupom == null) {
            throw new PedidoInvalidoException(CodigoErro.CUPOM_INVALIDO);
        }
        return cupom;
    }

    private FormaPagamento buscarFormaPagamento(String codigo) {
        FormaPagamento forma = codigo == null ? null : formasPagamento.get(codigo);
        if (forma == null) {
            throw new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return forma;
    }

    private BigDecimal somarSubtotal(List<ItemRequest> itens) {
        return itens.stream().map(ItemRequest::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPeso(List<ItemRequest> itens) {
        return itens.stream().map(ItemRequest::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
