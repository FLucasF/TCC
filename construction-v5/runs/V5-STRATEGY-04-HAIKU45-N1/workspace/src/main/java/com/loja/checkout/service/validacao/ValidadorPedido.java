package com.loja.checkout.service.validacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.service.cupom.EstrategiaCupom;
import com.loja.checkout.service.cupom.FactoryCupom;
import com.loja.checkout.service.entrega.EstrategiaEntrega;
import com.loja.checkout.service.entrega.FactoryEntrega;

@Service
public class ValidadorPedido {
    public Optional<ErroValidacao> validar(PedidoRequest pedido) {
        // 1. Validar pedido (carrinho vazio ou itens inválidos)
        if (!pedidoValido(pedido.itens())) {
            return Optional.of(ErroValidacao.PEDIDO_INVALIDO);
        }

        // 2. Validar nível do clube
        if (pedido.nivelClube() == null || !nivelClubeValido(pedido.nivelClube())) {
            return Optional.of(ErroValidacao.NIVEL_CLUBE_INVALIDO);
        }

        // 3. Validar região
        if (pedido.regiao() == null || !regiaoValida(pedido.regiao())) {
            return Optional.of(ErroValidacao.REGIAO_INVALIDA);
        }

        // 4. Validar modalidade de entrega
        if (pedido.modalidadeEntrega() == null || !modalidadeValida(pedido.modalidadeEntrega())) {
            return Optional.of(ErroValidacao.MODALIDADE_INVALIDA);
        }

        // 5. Validar se a modalidade é disponível para o pedido
        try {
            ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(pedido.modalidadeEntrega());
            BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());
            EstrategiaEntrega estrategia = FactoryEntrega.criar(modalidade);
            if (!estrategia.valida(pesoTotal)) {
                return Optional.of(ErroValidacao.MODALIDADE_INDISPONIVEL);
            }
        } catch (IllegalArgumentException e) {
            return Optional.of(ErroValidacao.MODALIDADE_INVALIDA);
        }

        // 6. Validar cupom (se fornecido)
        if (pedido.cupom() != null && !pedido.cupom().isEmpty()) {
            if (!cupomValido(pedido.cupom())) {
                return Optional.of(ErroValidacao.CUPOM_INVALIDO);
            }

            // 7. Validar se o cupom é aplicável
            try {
                Cupom cupom = Cupom.valueOf(pedido.cupom());
                BigDecimal subtotal = calcularSubtotal(pedido.itens());
                EstrategiaCupom estrategia = FactoryCupom.criar(cupom);
                if (!estrategia.valido(subtotal)) {
                    return Optional.of(ErroValidacao.CUPOM_NAO_APLICAVEL);
                }
            } catch (IllegalArgumentException e) {
                return Optional.of(ErroValidacao.CUPOM_INVALIDO);
            }
        }

        // 8. Validar forma de pagamento
        if (pedido.formaPagamento() == null || !formaPagamentoValida(pedido.formaPagamento())) {
            return Optional.of(ErroValidacao.FORMA_PAGAMENTO_INVALIDA);
        }

        // 9. Validar parcelamento
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!parcelamentoValido(pedido.formaPagamento(), parcelas)) {
            return Optional.of(ErroValidacao.PARCELAMENTO_INVALIDO);
        }

        // 10. Validar se a forma de pagamento é disponível para o pedido
        // (ex.: boleto acima de 1000)
        // Isso será verificado depois no serviço, após calcular o total

        return Optional.empty();
    }

    private boolean pedidoValido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            return false;
        }

        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0) {
                return false;
            }
            if (item.quantidade() <= 0) {
                return false;
            }
            if (item.pesoKg() == null || item.pesoKg().signum() < 0) {
                return false;
            }
        }

        return true;
    }

    private boolean nivelClubeValido(String nivel) {
        try {
            NivelClube.valueOf(nivel);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean regiaoValida(String regiao) {
        try {
            Regiao.valueOf(regiao);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean modalidadeValida(String modalidade) {
        try {
            ModalidadeEntrega.valueOf(modalidade);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean cupomValido(String cupom) {
        try {
            Cupom.valueOf(cupom);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean formaPagamentoValida(String forma) {
        try {
            FormaPagamento.valueOf(forma);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean parcelamentoValido(String formaPagamento, int parcelas) {
        try {
            FormaPagamento forma = FormaPagamento.valueOf(formaPagamento);
            return switch (forma) {
                case PIX, BOLETO -> parcelas == 1;
                case CARTAO -> parcelas >= 1 && parcelas <= 12;
            };
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        return itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
