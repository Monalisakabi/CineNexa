package com.smartticket.backend.controller;
import com.smartticket.backend.dto.TicketVerificationResponse;
import org.springframework.web.bind.annotation.RequestParam;
import com.smartticket.backend.dto.BookingResponse;
import com.smartticket.backend.dto.CreateBookingRequest;
import com.smartticket.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;


@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:4202",
        "http://localhost:4201"
})
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody CreateBookingRequest request) {

        BookingResponse response =
                bookingService.createBooking(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(
                bookingService.getBookingById(bookingId)
        );
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                bookingService.getBookingsByUserId(userId)
        );
    }

    @PostMapping("/verify-ticket")
    public ResponseEntity<TicketVerificationResponse> verifyTicket(
            @RequestParam String token) {

        return ResponseEntity.ok(
                bookingService.verifyTicket(token)
        );
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long bookingId) {

        BookingResponse response =
                bookingService.cancelBooking(bookingId);

        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }
}