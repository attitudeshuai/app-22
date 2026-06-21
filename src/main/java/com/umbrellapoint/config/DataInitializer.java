package com.umbrellapoint.config;

import com.umbrellapoint.entity.*;
import com.umbrellapoint.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final UmbrellaRepository umbrellaRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserCreditRepository userCreditRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           StationRepository stationRepository,
                           UmbrellaRepository umbrellaRepository,
                           BorrowRecordRepository borrowRecordRepository,
                           UserCreditRepository userCreditRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.umbrellaRepository = umbrellaRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.userCreditRepository = userCreditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initUsers();
        initStations();
        initUmbrellas();
        initBorrowRecords();
        initUserCredits();
    }

    private void initUsers() {
        if (userRepository.count() > 0) {
            logger.info("用户数据已存在，跳过初始化");
            return;
        }
        String[] usernames = {"admin", "zhangsan", "lisi", "wangwu", "zhaoliu", "sunqi"};
        String[] emails = {"admin@example.com", "zhangsan@example.com", "lisi@example.com",
                "wangwu@example.com", "zhaoliu@example.com", "sunqi@example.com"};

        for (int i = 0; i < usernames.length; i++) {
            User user = new User();
            user.setUsername(usernames[i]);
            user.setEmail(emails[i]);
            user.setPasswordHash(passwordEncoder.encode("123456"));
            user.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + usernames[i]);
            userRepository.save(user);
        }
        logger.info("用户数据初始化完成，共创建 {} 个用户", usernames.length);
    }

    private void initStations() {
        if (stationRepository.count() > 0) {
            logger.info("站点数据已存在，跳过初始化");
            return;
        }
        String[][] stations = {
                {"中关村地铁站A口", "北京市海淀区中关村地铁站A出口", "39.984120", "116.307490"},
                {"国贸大厦1号门", "北京市朝阳区建国门外大街1号", "39.908760", "116.459630"},
                {"西二旗地铁B口", "北京市海淀区西二旗地铁站B出口", "40.049920", "116.305810"},
                {"望京SOHO", "北京市朝阳区望京街10号", "39.993060", "116.477270"},
                {"五道口地铁站", "北京市海淀区成府路28号", "39.992890", "116.339800"},
                {"北京南站北广场", "北京市丰台区永外大街车站路12号", "39.865340", "116.378400"}
        };
        User admin = userRepository.findByUsername("admin").orElse(null);
        for (int i = 0; i < stations.length; i++) {
            Station station = new Station();
            station.setName(stations[i][0]);
            station.setAddress(stations[i][1]);
            station.setManagerId(admin != null ? admin.getId() : 1L);
            station.setLatitude(new BigDecimal(stations[i][2]));
            station.setLongitude(new BigDecimal(stations[i][3]));
            station.setCapacity(20 + i * 5);
            station.setIsActive(true);
            stationRepository.save(station);
        }
        logger.info("站点数据初始化完成，共创建 {} 个站点", stations.length);
    }

    private void initUmbrellas() {
        if (umbrellaRepository.count() > 0) {
            logger.info("雨伞数据已存在，跳过初始化");
            return;
        }
        String[] colors = {"黑色", "蓝色", "红色", "绿色", "黄色", "紫色", "橙色", "灰色"};
        long stationCount = stationRepository.count();
        int umbrellaCount = 0;
        for (int stationIdx = 1; stationIdx <= stationCount; stationIdx++) {
            for (int i = 1; i <= 5; i++) {
                Umbrella umbrella = new Umbrella();
                umbrella.setCode(String.format("UM-%03d-%02d", stationIdx, i));
                umbrella.setStationId((long) stationIdx);
                umbrella.setColor(colors[(stationIdx + i) % colors.length]);
                umbrella.setStatus(Umbrella.UmbrellaStatus.Available);
                umbrellaRepository.save(umbrella);
                umbrellaCount++;
            }
        }
        logger.info("雨伞数据初始化完成，共创建 {} 把雨伞", umbrellaCount);
    }

    private void initBorrowRecords() {
        if (borrowRecordRepository.count() > 0) {
            logger.info("借还记录数据已存在，跳过初始化");
            return;
        }
        User zhangsan = userRepository.findByUsername("zhangsan").orElse(null);
        if (zhangsan != null && stationRepository.count() >= 2 && umbrellaRepository.count() >= 2) {
            BorrowRecord record1 = new BorrowRecord();
            record1.setUmbrellaId(1L);
            record1.setUserId(zhangsan.getId());
            record1.setBorrowStationId(1L);
            record1.setReturnStationId(2L);
            record1.setBorrowTime(LocalDateTime.now().minusDays(3));
            record1.setReturnTime(LocalDateTime.now().minusDays(2));
            record1.setStatus(BorrowRecord.BorrowStatus.Returned);
            record1.setDeposit(new BigDecimal("29.90"));
            borrowRecordRepository.save(record1);

            BorrowRecord record2 = new BorrowRecord();
            record2.setUmbrellaId(6L);
            record2.setUserId(zhangsan.getId());
            record2.setBorrowStationId(2L);
            record2.setBorrowTime(LocalDateTime.now().minusHours(5));
            record2.setStatus(BorrowRecord.BorrowStatus.Ongoing);
            record2.setDeposit(new BigDecimal("29.90"));
            borrowRecordRepository.save(record2);
        }
        logger.info("借还记录数据初始化完成");
    }

    private void initUserCredits() {
        if (userCreditRepository.count() > 0) {
            logger.info("用户信用数据已存在，跳过初始化");
            return;
        }
        userRepository.findAll().forEach(user -> {
            UserCredit credit = new UserCredit();
            credit.setUserId(user.getId());
            credit.setScore("admin".equals(user.getUsername()) ? 100 : 95);
            credit.setOverdueCount(0);
            userCreditRepository.save(credit);
        });
        logger.info("用户信用数据初始化完成");
    }
}
