package com.example.seatreservations

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.seatreservations.SeatReservationState.*

class MainActivity : AppCompatActivity() {
    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val seatReservationsView = findViewById<SeatReservationsView>(R.id.element)

        val map = arrayOf(
            arrayOf(FREE, FREE, FREE, FREE, FREE, FREE, FREE),
            arrayOf(FREE, SELECTED, FREE, FREE, FREE),
            arrayOf(FREE, FREE, FREE, FREE, FREE, FREE, FREE),
            arrayOf(BOOKED, BOOKED, BOOKED, FREE, FREE, FREE, SELECTED),
        )

        seatReservationsView.updateMap(map)
        seatReservationsView.setOnClickListener { state, position ->
            Toast.makeText(this, "New state is $state in position: ${position.first}, ${position.second}", 100).show()
        }
    }
}