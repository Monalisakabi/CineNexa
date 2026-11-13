package com.smartticket.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandInputsResponse {

    private Long showId;
    private Integer totalSeats;
    private Integer currentBookings;
    private Double hoursUntilShow;
}