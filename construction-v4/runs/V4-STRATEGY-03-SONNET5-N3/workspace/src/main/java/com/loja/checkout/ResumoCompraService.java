package com.loja.checkout;

import com.loja.checkout.clube.BeneficioClube;
import com.loja.checkout.clube.CatalogoClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.OpcaoEntrega;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import com.loja.checkout.pagamento.CatalogoPagamentos;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.CatalogoRegioes;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    private final CatalogoEntregas catalogoEntregas;
    private final CatalogoCupons catalogoCupons;
    private final CatalogoClube catalogoClube;
    private final CatalogoPagamentos catalogoPagamentos;
    private final CatalogoRegioes catalogoRegioes;

    public ResumoCompraService(
            CatalogoEntregas catalogoEntregas,
            CatalogoCupons catalogoCupons,
            CatalogoClube catalogoClube,
            CatalogoPagamentos catalogoPagamentos,
            CatalogoRegioes catalogoRegioes) {
        this.catalogoEntregas = catalogoEntregas;
        this.catalogoCupons = catalogoCupons;
        this.catalogoClube = catalogoClube;
        this.catalogoPagamentos = catalogoPagamentos;
        this.catalogoRegioes = catalogoRegioes;
    }

    public ResumoResponse calcular(ResumoRequest pedido) {
        validarItens(pedido.itens());
        BeneficioClube beneficioClube = validarClube(pedido.nivelClube());
        BigDecimal percentualSeguro = validarRegiao(pedido.regiao());
        OpcaoEntrega opcaoEntrega = validarModalidadeEntrega(pedido.modalidadeEntrega());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(subtotal(pedido.itens()));
        BigDecimal pesoPedido = pesoTotal(pedido.itens());

        validarDisponibilidadeEntrega(opcaoEntrega, pesoPedido);

        BigDecimal freteBase = Dinheiro.arredondar(opcaoEntrega.custo(pesoPedido));
        BigDecimal frete = Dinheiro.arredondar(beneficioClube.frete(freteBase));

        Cupom cupom = validarCupom(pedido.cupom(), pedido.itens(), subtotalProdutos);
        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : Dinheiro.arredondar(cupom.desconto(pedido.itens(), subtotalProdutos, frete));

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(percentualSeguro));

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = validarFormaPagamento(pedido.formaPagamento());
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        validarParcelas(formaPagamento, parcelas);
        validarDisponibilidadePagamento(formaPagamento, totalPedido);

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(beneficioClube.credito(subtotalProdutos));
        boolean brinde = beneficioClube.brinde(subtotalProdutos);

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

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private BeneficioClube validarClube(String nivelClube) {
        BeneficioClube beneficioClube = nivelClube == null ? null : catalogoClube.buscar(nivelClube);
        if (beneficioClube == null) {
            throw new PedidoInvalidoException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        return beneficioClube;
    }

    private BigDecimal validarRegiao(String regiao) {
        BigDecimal percentual = regiao == null ? null : catalogoRegioes.percentualSeguro(regiao);
        if (percentual == null) {
            throw new PedidoInvalidoException(CodigoErro.REGIAO_INVALIDA);
        }
        return percentual;
    }

    private OpcaoEntrega validarModalidadeEntrega(String modalidadeEntrega) {
        OpcaoEntrega opcaoEntrega = modalidadeEntrega == null ? null : catalogoEntregas.buscar(modalidadeEntrega);
        if (opcaoEntrega == null) {
            throw new PedidoInvalidoException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return opcaoEntrega;
    }

    private void validarDisponibilidadeEntrega(OpcaoEntrega opcaoEntrega, BigDecimal pesoPedido) {
        if (!opcaoEntrega.disponivel(pesoPedido)) {
            throw new PedidoInvalidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    private void validarParcelas(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new PedidoInvalidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    private void validarDisponibilidadePagamento(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    private Cupom validarCupom(String codigoCupom, List<ItemRequest> itens, BigDecimal subtotalProdutos) {
        if (codigoCupom == null) {
            return null;
        }
        Cupom cupom = catalogoCupons.buscar(codigoCupom);
        if (cupom == null) {
            throw new PedidoInvalidoException(CodigoErro.CUPOM_INVALIDO);
        }
        if (!cupom.aplicavel(itens, subtotalProdutos)) {
            throw new PedidoInvalidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom;
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        FormaPagamento forma = formaPagamento == null ? null : catalogoPagamentos.buscar(formaPagamento);
        if (forma == null) {
            throw new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return forma;
    }

    private BigDecimal subtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal pesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
