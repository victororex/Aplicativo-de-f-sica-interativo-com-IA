package com.example.testes.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val privateAccount: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val profileImageUrl: String? = null
)
