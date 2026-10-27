package com.example.qa

data class Alerta(
    val id: String = "",
    val dispositivo: String = "",
    val status: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val horario: String = "",
    val timestampMillis: Long = 0L
)