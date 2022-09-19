package com.example.seatreservations

enum class SeatReservationState(val index: Int) {
    SELECTED(1), BOOKED(2), FREE(3), EMPTY(0)
}