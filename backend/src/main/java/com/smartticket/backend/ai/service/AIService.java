package com.smartticket.backend.ai.service;
import com.smartticket.backend.movie.entity.Movie;
import com.smartticket.backend.movie.repository.MovieRepository;
import java.util.List;
import java.util.stream.Collectors;
import com.smartticket.backend.repository.UserRepository;
import com.smartticket.backend.movie.entity.Show;
import com.smartticket.backend.movie.entity.Screen;
import com.smartticket.backend.movie.repository.ShowRepository;
import com.smartticket.backend.movie.repository.ScreenRepository;
import com.smartticket.backend.repository.BookingRepository;
import com.smartticket.backend.repository.BookingSeatRepository;
import com.smartticket.backend.ai.dto.DemandInputsResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import com.smartticket.backend.ai.dto.DemandPredictionRequest;
import com.smartticket.backend.ai.dto.PythonDemandPredictionRequest;
import com.smartticket.backend.ai.dto.PythonRecommendationRequest;
import com.smartticket.backend.ai.dto.RecommendationRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.smartticket.backend.ai.dto.FraudDetectionRequest;
import com.smartticket.backend.ai.dto.PythonFraudDetectionRequest;

@Service
public class AIService {

    private final RestTemplate restTemplate;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    public AIService(
            MovieRepository movieRepository,
            UserRepository userRepository,
            ShowRepository showRepository,
            ScreenRepository screenRepository,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository) {

        this.restTemplate = new RestTemplate();
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
        this.showRepository = showRepository;
        this.screenRepository = screenRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
    }
    private static final String AI_BASE_URL =
            "http://127.0.0.1:8000";


    public String healthCheck() {
        return restTemplate.getForObject(
                AI_BASE_URL + "/health",
                String.class
        );
    }

    public String getRecommendations(RecommendationRequest request) {

        List<PythonRecommendationRequest.MovieData> movies =
                movieRepository.findAll().stream()
                        .map(movie -> new PythonRecommendationRequest.MovieData(
                                movie.getId(),
                                movie.getTitle(),
                                movie.getDescription(),
                                movie.getGenre(),
                                movie.getLanguage(),
                                movie.getDuration(),
                                movie.getReleaseDate(),
                                movie.getPosterUrl()
                        ))
                        .collect(Collectors.toList());

        String favoriteGenres = userRepository.findById(request.getUserId())
                .map(user -> user.getFavoriteGenres())
                .orElse("");

        List<String> preferredGenres = favoriteGenres == null
                || favoriteGenres.isBlank()
                ? List.of()
                : List.of(favoriteGenres.split(","));

        PythonRecommendationRequest pythonRequest =
                new PythonRecommendationRequest(
                        request.getUserId(),
                        preferredGenres,
                        request.getLimit(),
                        movies
                );

        return restTemplate.postForObject(
                AI_BASE_URL + "/api/ai/recommendations",
                pythonRequest,
                String.class
        );
    }

    public String predictDemand(DemandPredictionRequest request) {

        DemandInputsResponse inputs =
                getDemandInputs(request.getShowId());

        PythonDemandPredictionRequest pythonRequest =
                new PythonDemandPredictionRequest(
                        inputs.getShowId(),
                        inputs.getCurrentBookings(),
                        inputs.getTotalSeats(),
                        inputs.getHoursUntilShow()
                );

        return restTemplate.postForObject(
                AI_BASE_URL + "/api/ai/demand-prediction",
                pythonRequest,
                String.class
        );
    }
    public String detectFraud(
            FraudDetectionRequest request) {

        PythonFraudDetectionRequest pythonRequest =
                new PythonFraudDetectionRequest(
                        request.getBookingId(),
                        request.getUserId(),
                        request.getBookingAmount(),
                        request.getBookingFrequency(),
                        request.getFailedPaymentAttempts(),
                        request.getBookingsInLastMinute()
                );

        return restTemplate.postForObject(
                AI_BASE_URL + "/api/ai/fraud-detection",
                pythonRequest,
                String.class
        );
    }
    public DemandInputsResponse getDemandInputs(Long showId) {

        // 1. Fetch show
        Show show = showRepository.findById(showId)
                .orElseThrow(() ->
                        new RuntimeException("Show not found"));

        // 2. Fetch screen capacity
        Screen screen = screenRepository.findById(show.getScreenId())
                .orElseThrow(() ->
                        new RuntimeException("Screen not found"));

        int totalSeats = screen.getTotalSeats();

        // 3. Count confirmed booked seats
        int currentBookings = Math.toIntExact(
                bookingSeatRepository.countConfirmedSeatsByShowId(show.getId())
        );

        // 4. Calculate remaining hours
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        LocalDateTime showDateTime = LocalDateTime.parse(
                show.getShowDate() + " " + show.getShowTime(),
                formatter
        );

        double hoursUntilShow = Duration.between(
                LocalDateTime.now(),
                showDateTime
        ).toMinutes() / 60.0;

        // 5. Return database information
        return new DemandInputsResponse(
                show.getId(),
                totalSeats,
                currentBookings,
                hoursUntilShow
        );
    }
}