package br.com.mare.domain;

/**
 * Situação da reserva. Cada situação diz para onde pode ir; o que ela não sobrescreve é proibido.
 */
public enum ReservationStatus {
    PENDING {
        @Override
        public ReservationStatus confirm() {
            return CONFIRMED;
        }

        @Override
        public ReservationStatus cancel() {
            return CANCELLED;
        }
    },
    CONFIRMED {
        @Override
        public ReservationStatus checkIn() {
            return CHECKED_IN;
        }

        @Override
        public ReservationStatus cancel() {
            return CANCELLED;
        }

        @Override
        public boolean isPaid() {
            return true;
        }
    },
    CHECKED_IN {
        @Override
        public ReservationStatus checkOut() {
            return CHECKED_OUT;
        }

        @Override
        public boolean isPaid() {
            return true;
        }
    },
    CHECKED_OUT {
        @Override
        public boolean isPaid() {
            return true;
        }
    },
    CANCELLED;

    public ReservationStatus confirm() {
        throw notAllowed("confirmar");
    }

    public ReservationStatus checkIn() {
        throw notAllowed("fazer check-in de");
    }

    public ReservationStatus checkOut() {
        throw notAllowed("fazer check-out de");
    }

    public ReservationStatus cancel() {
        throw notAllowed("cancelar");
    }

    /** Reserva paga: confirmada, com check-in ou com check-out. */
    public boolean isPaid() {
        return false;
    }

    private BusinessException notAllowed(String action) {
        return new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Não é possível " + action + " uma reserva " + name());
    }
}
