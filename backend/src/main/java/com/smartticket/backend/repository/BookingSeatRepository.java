package com.smartticket.backend.repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.smartticket.backend.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingSeatRepository
        extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBookingId(Long bookingId);
    @Query("""
    SELECT COUNT(bs)
    FROM BookingSeat bs
    WHERE bs.bookingId IN (
        SELECT b.id
        FROM Booking b
        WHERE b.showId = :showId
        AND b.status = 'CONFIRMED'
    )
""")
    long countConfirmedSeatsByShowId(@Param("showId") Long showId);
}
