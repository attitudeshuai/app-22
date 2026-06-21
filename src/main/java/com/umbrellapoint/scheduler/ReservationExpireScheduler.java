package com.umbrellapoint.scheduler;

import com.umbrellapoint.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservationExpireScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpireScheduler.class);

    private final ReservationService reservationService;

    public ReservationExpireScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void checkExpiredReservations() {
        logger.info("开始执行预约过期检查定时任务...");
        try {
            reservationService.processExpiredReservations();
            logger.info("预约过期检查定时任务执行完成");
        } catch (Exception e) {
            logger.error("预约过期检查定时任务执行异常", e);
        }
    }
}
