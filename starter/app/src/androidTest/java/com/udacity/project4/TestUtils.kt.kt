package com.udacity.project4

import kotlin.random.Random

    fun randomLatOrLong(): Double {
        val min = -180.0 // Minimum allowed value (longitude)
        val max = 180.0  // Maximum allowed value (longitude, can be adjusted for latitude)

        // Generate a random double between min and max (exclusive)
        return Random.nextDouble(min, max)
    }