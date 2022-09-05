package com.example.seatreservations


fun interface OnItemClickListener {
    /**
     * @param newState New state after clicking on the seat
     * @param seatNumber The number of the seat, the first parameter is a row, the second is a number in a row
     */
    fun onItemClick(newState: SeatReservationState, seatNumber: Pair<Int, Int>)
}