package com.cbzvdani.deviceinfo.utils

class Methods{
    companion object {
        fun calculatePercentage(value: Double, total: Double): Int {
            val usage: Double = (value * 100.0f / total).toDouble()
            return usage.toInt()
        }
    }
}