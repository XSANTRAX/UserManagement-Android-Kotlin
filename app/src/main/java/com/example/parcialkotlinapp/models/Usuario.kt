package com.example.parcialkotlinapp.models

data class Usuario(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val edad: Int,
    val activo: Boolean,
    val fechaCreacion: String,
    val fechaNacimiento: String? = null,
    val universidad: String? = null,
    val semestre: Int? = null
)