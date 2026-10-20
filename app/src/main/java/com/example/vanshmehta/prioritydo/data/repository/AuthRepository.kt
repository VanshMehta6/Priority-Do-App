package com.example.vanshmehta.prioritydo.data.repository

import com.example.vanshmehta.prioritydo.data.model.UserProfile
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(l)
        awaitClose { auth.removeAuthStateListener(l) }
    }

    /** Creates the Auth account, then the /users/{uid} profile record (no password is stored). */
    suspend fun register(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Account created but no user returned")
        // Not awaited: with offline persistence the write is queued and must not block sign-up.
        db.getReference("users").child(user.uid).setValue(
            mapOf(
                "displayName" to name,
                "email" to email,
                "createdAt" to System.currentTimeMillis()
            )
        )
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun sendReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun logout() = auth.signOut()

    fun updateDisplayName(user: FirebaseUser, name: String) {
        db.getReference("users").child(user.uid)
            .updateChildren(mapOf<String, Any>("displayName" to name, "email" to (user.email ?: "")))
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
    }

    fun observeProfile(user: FirebaseUser): Flow<UserProfile> = callbackFlow {
        val ref = db.getReference("users").child(user.uid)
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val name = try { s.child("displayName").getValue(String::class.java) } catch (e: Exception) { null }
                val created = try { s.child("createdAt").getValue(Long::class.java) } catch (e: Exception) { null }
                val base = fallbackProfile(user)
                trySend(base.copy(
                    displayName = name?.takeIf { it.isNotBlank() } ?: base.displayName,
                    createdAt = created ?: base.createdAt
                ))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(fallbackProfile(user))
            }
        }
        ref.addValueEventListener(l)
        awaitClose { ref.removeEventListener(l) }
    }

    companion object {
        fun fallbackProfile(user: FirebaseUser) = UserProfile(
            uid = user.uid,
            displayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "User",
            email = user.email.orEmpty(),
            createdAt = user.metadata?.creationTimestamp ?: 0L
        )

        fun friendlyError(e: Throwable): String = when (e) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
            is FirebaseAuthInvalidUserException -> "Invalid email or password."
            is FirebaseNetworkException -> "No network connection. Check your internet and try again."
            else -> "Something went wrong. Please try again."
        }
    }
}
