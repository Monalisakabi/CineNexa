package com.smartticket.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TicketVerificationResponse {

    private boolean valid;
    private String message;
    private Long bookingId;
    private String movieTitle;
    private String status;
}