package com.vasu.bookingservice.config;

import com.vasu.bookingservice.entity.ShowSeat;
import com.vasu.bookingservice.enums.SeatStatus;
import com.vasu.bookingservice.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DevDataSeederConfig {

    private final ShowSeatRepository showSeatRepository;

    // Stable IDs matching Movie Service seeder order
    // Shows: 1-6 (Oppenheimer Screen1, Oppenheimer Screen1, Pathaan Screen2, Barbie Screen2, Pathaan Screen1, Barbie Screen1)
    // Screen seats: 1-40 per screen (5 rows × 8 columns)
    // Screen IDs: 1, 2 (PVR), 3, 4 (INOX)
    private static final List<Long> SEED_SHOW_IDS = List.of(1L, 2L, 3L, 4L, 5L, 6L);

    // Seat type pricing
    private static final Map<String, BigDecimal> SEAT_PRICES = Map.of(
            "VIP", new BigDecimal("250.00"),
            "PREMIUM", new BigDecimal("200.00"),
            "STANDARD", new BigDecimal("150.00")
    );

    @Bean
    public CommandLineRunner seedShowSeats() {
        return args -> {
            log.info("=== Seeding Booking Service development data ===");

            int totalSeeded = 0;
            for (Long showId : SEED_SHOW_IDS) {
                int count = seedShowSeatInventory(showId);
                totalSeeded += count;
            }

            log.info("=== Booking Service seeding complete: {} total show-seats ===", totalSeeded);
        };
    }

    @Transactional
    protected int seedShowSeatInventory(Long showId) {
        long existingCount = showSeatRepository.findByShowId(showId).size();
        if (existingCount > 0) {
            log.info("Skipping show {} — {} show-seats already exist", showId, existingCount);
            return 0;
        }

        // Generate 40 seats matching the Movie Service seat layout
        // Rows A-E, Columns 1-8
        // A, B = VIP; C = PREMIUM; D, E = STANDARD
        List<String> rowLabels = List.of("A", "B", "C", "D", "E");
        int columnsPerRow = 8;

        List<ShowSeat> showSeats = new ArrayList<>();
        long screenSeatIdCounter = 1;

        for (String row : rowLabels) {
            for (int col = 1; col <= columnsPerRow; col++) {
                String seatType = determineSeatType(row);
                BigDecimal price = SEAT_PRICES.get(seatType);

                ShowSeat showSeat = ShowSeat.builder()
                        .showId(showId)
                        .screenSeatId(screenSeatIdCounter)
                        .price(price)
                        .status(SeatStatus.AVAILABLE)
                        .reservedUntil(null)
                        .bookingId(null)
                        .version(0L)
                        .build();

                showSeats.add(showSeat);
                screenSeatIdCounter++;
            }
        }

        showSeatRepository.saveAll(showSeats);
        log.info("Seeded {} show-seats for show {}", showSeats.size(), showId);
        return showSeats.size();
    }

    private String determineSeatType(String rowLabel) {
        if (rowLabel.equals("A") || rowLabel.equals("B")) {
            return "VIP";
        } else if (rowLabel.equals("C")) {
            return "PREMIUM";
        } else {
            return "STANDARD";
        }
    }
}
