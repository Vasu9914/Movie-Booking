package com.vasu.movie_service.config;

import com.vasu.movie_service.entity.Movie;
import com.vasu.movie_service.entity.Screen;
import com.vasu.movie_service.entity.ScreenSeat;
import com.vasu.movie_service.entity.Show;
import com.vasu.movie_service.entity.Theatre;
import com.vasu.movie_service.enums.EntityStatus;
import com.vasu.movie_service.enums.SeatType;
import com.vasu.movie_service.enums.ShowStatus;
import com.vasu.movie_service.repository.MovieRepository;
import com.vasu.movie_service.repository.ScreenRepository;
import com.vasu.movie_service.repository.ScreenSeatRepository;
import com.vasu.movie_service.repository.ShowRepository;
import com.vasu.movie_service.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DevDataSeederConfig {

    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final ScreenSeatRepository screenSeatRepository;
    private final ShowRepository showRepository;

    @Bean
    public CommandLineRunner seedMovieData() {
        return args -> {
            log.info("=== Seeding Movie Service development data ===");

            // Seed movies first
            List<Movie> movies = seedMovies();

            // Seed theatres
            List<Theatre> theatres = seedTheatres();

            // Seed screens for each theatre
            List<Screen> screens = new ArrayList<>();
            for (Theatre theatre : theatres) {
                screens.addAll(seedScreens(theatre));
            }

            // Seed seats for each screen
            for (Screen screen : screens) {
                seedSeats(screen);
            }

            // Seed shows
            seedShows(movies, screens);

            log.info("=== Movie Service seeding complete ===");
        };
    }

    @Transactional
    protected List<Movie> seedMovies() {
        List<Movie> movies = new ArrayList<>();

        Movie movie1 = seedMovie("Oppenheimer", "A dramatic thriller about the creation of the atomic bomb.",
                "English", 180, LocalDate.of(2023, 7, 21));
        Movie movie2 = seedMovie("Pathaan", "An action thriller about an Indian spy.",
                "Hindi", 145, LocalDate.of(2023, 1, 25));
        Movie movie3 = seedMovie("Barbie", "A comedy about a doll coming to life.",
                "English", 114, LocalDate.of(2023, 7, 21));

        if (movie1 != null) movies.add(movie1);
        if (movie2 != null) movies.add(movie2);
        if (movie3 != null) movies.add(movie3);

        log.info("Seeded {} movies", movies.size());
        return movies;
    }

    @Transactional
    protected List<Theatre> seedTheatres() {
        List<Theatre> theatres = new ArrayList<>();

        Theatre t1 = seedTheatre("PVR Cinemas", "Mumbai", "Andheri West, Mumbai, Maharashtra 400053");
        Theatre t2 = seedTheatre("INOX Multiplex", "Delhi", "Connaught Place, New Delhi 110001");

        if (t1 != null) theatres.add(t1);
        if (t2 != null) theatres.add(t2);

        log.info("Seeded {} theatres", theatres.size());
        return theatres;
    }

    @Transactional
    protected List<Screen> seedScreens(Theatre theatre) {
        List<Screen> screens = new ArrayList<>();

        Screen s1 = seedScreen(theatre, "Screen 1", 40);
        Screen s2 = seedScreen(theatre, "Screen 2", 40);

        if (s1 != null) screens.add(s1);
        if (s2 != null) screens.add(s2);

        log.info("Seeded {} screens for theatre: {}", screens.size(), theatre.getName());
        return screens;
    }

    @Transactional
    protected void seedSeats(Screen screen) {
        long existingCount = screenSeatRepository.countByScreenId(screen.getId());
        if (existingCount > 0) {
            log.info("Skipping seats for screen {} ({}) — {} seats already exist",
                    screen.getName(), screen.getId(), existingCount);
            return;
        }

        List<String> rowLabels = List.of("A", "B", "C", "D", "E");
        int columnsPerRow = 8;
        List<ScreenSeat> seats = new ArrayList<>();

        for (String row : rowLabels) {
            for (int col = 1; col <= columnsPerRow; col++) {
                String seatNumber = row + String.valueOf(col);
                SeatType seatType;

                if (row.equals("A") || row.equals("B")) {
                    seatType = SeatType.VIP;
                } else if (row.equals("C")) {
                    seatType = SeatType.PREMIUM;
                } else {
                    seatType = SeatType.STANDARD;
                }

                ScreenSeat seat = ScreenSeat.builder()
                        .screen(screen)
                        .seatNumber(seatNumber)
                        .rowLabel(row)
                        .columnNumber(col)
                        .seatType(seatType)
                        .build();
                seats.add(seat);
            }
        }

        screenSeatRepository.saveAll(seats);
        log.info("Seeded {} seats for screen: {}", seats.size(), screen.getName());
    }

    @Transactional
    protected void seedShows(List<Movie> movies, List<Screen> screens) {
        if (movies.isEmpty() || screens.isEmpty()) {
            log.warn("Cannot seed shows: missing movies or screens");
            return;
        }

        LocalDateTime base = LocalDateTime.of(2025, 10, 10, 10, 0);

        seedShow("Show 1", movies.get(0), screens.get(0), base, base.plusHours(3));
        seedShow("Show 2", movies.get(0), screens.get(0), base.plusHours(4), base.plusHours(7));
        seedShow("Show 3", movies.get(1), screens.get(1), base, base.plusHours(2));
        seedShow("Show 4", movies.get(2), screens.get(1), base.plusHours(3), base.plusHours(5));
        seedShow("Show 5", movies.get(1), screens.get(2), base.plusHours(1), base.plusHours(3));
        seedShow("Show 6", movies.get(2), screens.get(3), base, base.plusHours(2));
    }

    private Movie seedMovie(String title, String description, String language, int durationMinutes, LocalDate releaseDate) {
        if (movieRepository.existsByTitle(title)) {
            log.info("Skipping existing movie: {}", title);
            return movieRepository.findAll().stream()
                    .filter(m -> m.getTitle().equals(title))
                    .findFirst()
                    .orElse(null);
        }

        Movie movie = Movie.builder()
                .title(title)
                .description(description)
                .language(language)
                .durationMinutes(durationMinutes)
                .releaseDate(releaseDate)
                .status(EntityStatus.ACTIVE)
                .build();

        return movieRepository.save(movie);
    }

    private Theatre seedTheatre(String name, String city, String address) {
        boolean exists = theatreRepository.findAll().stream()
                .anyMatch(t -> t.getName().equals(name) && t.getCity().equals(city));
        if (exists) {
            log.info("Skipping existing theatre: {} ({})", name, city);
            return theatreRepository.findAll().stream()
                    .filter(t -> t.getName().equals(name) && t.getCity().equals(city))
                    .findFirst()
                    .orElse(null);
        }

        Theatre theatre = Theatre.builder()
                .name(name)
                .city(city)
                .address(address)
                .status(EntityStatus.ACTIVE)
                .build();

        return theatreRepository.save(theatre);
    }

    private Screen seedScreen(Theatre theatre, String name, int totalSeats) {
        boolean exists = screenRepository.findByTheatreIdAndStatus(theatre.getId(), EntityStatus.ACTIVE).stream()
                .anyMatch(s -> s.getName().equals(name));
        if (exists) {
            log.info("Skipping existing screen: {} (theatre: {})", name, theatre.getName());
            return screenRepository.findByTheatreIdAndStatus(theatre.getId(), EntityStatus.ACTIVE).stream()
                    .filter(s -> s.getName().equals(name))
                    .findFirst()
                    .orElse(null);
        }

        Screen screen = Screen.builder()
                .theatre(theatre)
                .name(name)
                .totalSeats(totalSeats)
                .status(EntityStatus.ACTIVE)
                .build();

        return screenRepository.save(screen);
    }

    @Transactional
    private Show seedShow(String label, Movie movie, Screen screen, LocalDateTime startTime, LocalDateTime endTime) {
        List<Show> allShows = showRepository.findAll();
        boolean exists = false;
        for (Show s : allShows) {
            // Force lazy loading within transaction
            long movieId = s.getMovie().getId();
            long screenId = s.getScreen().getId();
            if (movieId == movie.getId() && screenId == screen.getId() && s.getStartTime().equals(startTime)) {
                exists = true;
                break;
            }
        }

        if (exists) {
            log.info("Skipping existing show: {}", label);
            return null;
        }

        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .startTime(startTime)
                .endTime(endTime)
                .status(ShowStatus.SCHEDULED)
                .build();

        showRepository.save(show);
        log.info("Seeded show: {} (movie: {}, screen: {}, time: {})", label, movie.getTitle(), screen.getName(), startTime);
        return show;
    }
}
