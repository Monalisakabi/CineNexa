package com.smartticket.backend.service;
import java.util.List;
import com.smartticket.backend.dto.BookingResponse;
import com.smartticket.backend.dto.CreateBookingRequest;
import com.smartticket.backend.movie.entity.Movie;
import com.smartticket.backend.movie.entity.Show;
import com.smartticket.backend.movie.repository.MovieRepository;
import com.smartticket.backend.movie.repository.ShowRepository;
import com.smartticket.backend.entity.Booking;
import com.smartticket.backend.entity.BookingSeat;
import com.smartticket.backend.entity.Seat;
import org.springframework.transaction.annotation.Transactional;
import com.smartticket.backend.movie.entity.Show;
import com.smartticket.backend.movie.repository.ShowRepository;
import com.smartticket.backend.dto.RefundRequest;
import com.smartticket.backend.dto.TicketVerificationResponse;
import com.smartticket.backend.repository.BookingRepository;
import com.smartticket.backend.repository.BookingSeatRepository;
import com.smartticket.backend.repository.SeatRepository;
import com.smartticket.backend.repository.ShowSeatReservationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final MovieRepository movieRepository;
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatReservationRepository reservationRepository;
    private final SeatLockService seatLockService;

    // Module 2 integration
    private final ShowRepository showRepository;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Booking request is required");
        }

        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException(
                    "Valid user ID is required");
        }

        if (request.getShowId() == null || request.getShowId() <= 0) {
            throw new IllegalArgumentException(
                    "Valid show ID is required");
        }

        List<Long> seatIds = request.getSeatIds();

        if (seatIds == null || seatIds.size() != 1
                || seatIds.get(0) == null) {
            throw new IllegalArgumentException(
                    "Currently, exactly one seat must be booked");
        }

        Long showId = request.getShowId();
        Long seatId = seatIds.get(0);

        // Verify that the show exists in Module 2.
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Show does not exist"));

        // Verify the Redis lock token.
        boolean lockValid = seatLockService.isSeatLockedByToken(
                showId, seatId, request.getLockToken());

        if (!lockValid) {
            throw new IllegalArgumentException(
                    "Seat lock is missing, expired, or invalid");
        }

        // Verify the seat exists.
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seat does not exist"));

        // Verify that the seat belongs to the show's screen.
        if (!seat.getScreenId().equals(show.getScreenId())) {
            throw new IllegalArgumentException(
                    "Selected seat does not belong to this show's screen");
        }

        // Verify the reservation exists for this show.
        boolean reservationExists =
                reservationRepository
                        .findByShowIdAndSeatId(showId, seatId)
                        .isPresent();

        if (!reservationExists) {
            throw new IllegalArgumentException(
                    "Seat is not configured for this show");
        }

        BigDecimal totalAmount = seat.getPrice();

        // Create pending booking.
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .showId(showId)
                .totalAmount(totalAmount)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Atomically reserve the seat in the database.
        int updatedRows = reservationRepository.reserveSeat(
                showId, seatId, savedBooking.getId());

        if (updatedRows != 1) {
            throw new IllegalArgumentException(
                    "Seat is already reserved for this show");
        }

        // Save booking-seat relationship.
        BookingSeat bookingSeat = BookingSeat.builder()
                .bookingId(savedBooking.getId())
                .seatId(seat.getId())
                .price(seat.getPrice())
                .build();

        bookingSeatRepository.save(bookingSeat);

        // Release Redis lock after successful database commit.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        seatLockService.unlockSeat(
                                showId,
                                seatId,
                                request.getLockToken());
                    }
                });

        return toResponse(savedBooking, seatIds);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {

        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException(
                    "Valid booking ID is required");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found"));

        return toResponse(booking, getSeatIds(booking.getId()));
    }
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByUserId(Long userId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid user ID is required");
        }

        return bookingRepository.findByUserId(userId)
                .stream()
                .map(booking -> toResponse(
                        booking,
                        getSeatIds(booking.getId())
                ))
                .toList();
    }
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {

        return bookingRepository.findAll()
                .stream()
                .map(booking -> toResponse(
                        booking,
                        getSeatIds(booking.getId())
                ))
                .toList();
    }
    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {

        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Valid booking ID is required");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only confirmed bookings can be cancelled");
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setBookingId(bookingId);

        paymentService.processRefund(refundRequest);

        return toResponse(booking, getSeatIds(bookingId));
    }

    private List<Long> getSeatIds(Long bookingId) {

        return bookingSeatRepository.findByBookingId(bookingId)
                .stream()
                .map(BookingSeat::getSeatId)
                .toList();
    }

    private BookingResponse toResponse(Booking booking, List<Long> seatIds) {

        Show show = showRepository.findById(booking.getShowId())
                .orElse(null);

        Movie movie = show != null
                ? movieRepository.findById(show.getMovieId()).orElse(null)
                : null;
        List<String> seatNumbers = seatIds.stream()
                .map(seatId -> seatRepository.findById(seatId)
                        .map(Seat::getSeatNumber)
                        .orElse("Unknown"))
                .toList();
        return new BookingResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getShowId(),
                seatIds,
                seatNumbers,
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt(),
                movie != null ? movie.getTitle() : "Movie unavailable",
                movie != null ? movie.getPosterUrl() : null,
                show != null ? show.getShowDate() : null,
                show != null ? show.getShowTime() : null,
                booking.getCancelledAt(),
                booking.getCancellationPenalty(),
                booking.getRefundAmount(),
                booking.getTicketToken()
        );
    }
    @Transactional
    public TicketVerificationResponse verifyTicket(String token) {

        if (token == null || token.isBlank()) {
            return new TicketVerificationResponse(
                    false, "Invalid QR code", null, null, "INVALID"
            );
        }

        Booking booking = bookingRepository
                .findByTicketTokenForUpdate(token)
                .orElse(null);

        if (booking == null) {
            return new TicketVerificationResponse(
                    false, "Ticket not found", null, null, "INVALID"
            );
        }

        if (!"CONFIRMED".equals(booking.getStatus())) {
            return new TicketVerificationResponse(
                    false, "Booking is cancelled or not confirmed",
                    booking.getId(), null, booking.getStatus()
            );
        }

        if (Boolean.TRUE.equals(booking.getTicketUsed())) {
            return new TicketVerificationResponse(
                    false, "Ticket has already been used",
                    booking.getId(), null, "ALREADY_USED"
            );
        }

        booking.setTicketUsed(true);
        bookingRepository.save(booking);

        return new TicketVerificationResponse(
                true,
                "Ticket verified successfully",
                booking.getId(),
                null,
                "VALID"
        );
    }
}