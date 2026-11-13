package com.smartticket.backend.repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.smartticket.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    Optional<Booking> findByTicketToken(String ticketToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.ticketToken = :token")
    Optional<Booking> findByTicketTokenForUpdate(
            @Param("token") String token);

    List<Booking> findByStatusAndCreatedAtBefore(
            String status,
            LocalDateTime createdAt);

    // Get all bookings belonging to a customer
    List<Booking> findByUserId(Long userId);
    @Query("""
    SELECT COUNT(b)
    FROM Booking b
    WHERE b.showId = :showId
    AND b.status = 'CONFIRMED'
""")
    long countConfirmedBookingsByShowId(@Param("showId") Long showId);
}