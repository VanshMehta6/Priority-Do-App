package com.example.vanshmehta.prioritydo.data.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val createdAt: Long = 0L
) {
    val firstName: String get() = displayName.trim().substringBefore(" ")
}
