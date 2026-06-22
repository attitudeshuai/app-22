package com.umbrellapoint.scheduler;

import com.umbrellapoint.service.DailyStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DailyStatsScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DailyStatsScheduler.class);

    private final DailyStatsService dailyStatsService;

    public DailyStatsScheduler(DailyStatsService dailyStatsService) {
        this.dailyStatsService = dailyStatsService;
    }

    @Scheduled(cron = "0 30 1 * * ?")
    public void generateYesterdayDailyReport() {
        logger.info("开始执行每日日报生成任务...");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            dailyStatsService.generateDailyReport(yesterday);
            logger.info("每日日报生成任务执行完成，日期: {}", yesterday);
        } catch (Exception e) {
            logger.error("每日日报生成任务执行失败", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void generateLast7DaysReports() {
        logger.info("开始执行补全最近7天日报任务...");
        try {
            for (int i = 1; i <= 7; i++) {
                LocalDate date = LocalDate.now().minusDays(i);
                dailyStatsService.generateDailyReport(date);
            }
            logger.info("补全最近7天日报任务执行完成");
        } catch (Exception e) {
            logger.error("补全最近7天日报任务执行失败", e);
        }
    }
}
