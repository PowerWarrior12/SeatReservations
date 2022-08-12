package com.example.seatreservations

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.seatreservations.SeatReservationsView.SeatReservationState.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val seatReservationsView = findViewById<SeatReservationsView>(R.id.element)

        val map = arrayOf(
            arrayOf(EMPTY, EMPTY, EMPTY, BOOKED, BOOKED, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, BOOKED, BOOKED, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, BOOKED, BOOKED, BOOKED, BOOKED, BOOKED, BOOKED, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, FREE, EMPTY, EMPTY),
            arrayOf(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
            arrayOf(FREE, FREE, FREE, FREE, BOOKED, BOOKED, FREE, BOOKED, BOOKED, BOOKED, FREE, FREE, FREE),
        )

        seatReservationsView.updateMap(map)
        seatReservationsView.setOnClickListener { state, position ->
            Toast.makeText(this, "New state is $state in position: ${position.first}, ${position.second}", 100).show()
        }
    }
}