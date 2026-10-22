package com.pousadamarealta.service.event;

import com.pousadamarealta.domain.events.ReservationEvent;

public interface EventListener {
    void handle(ReservationEvent event);
}
