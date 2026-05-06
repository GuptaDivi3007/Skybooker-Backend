package com.skybooker.seat.scheduler;

import com.skybooker.seat.service.SeatService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SeatHoldScheduler {

    private final SeatService seatService;

    public SeatHoldScheduler(SeatService seatService) {
        this.seatService = seatService;
    }

    @Scheduled(fixedRate = 120000)
    public void releaseExpiredSeatHolds() {
        seatService.releaseExpiredHolds();
    }
}