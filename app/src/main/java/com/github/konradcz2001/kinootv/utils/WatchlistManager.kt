package com.github.konradcz2001.kinootv.utils

import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.github.konradcz2001.kinootv.BuildConfig
import com.github.konradcz2001.kinootv.data.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Singleton manager responsible for handling the user's movie watchlist via Firebase Realtime Database.
 * It manages synchronization, data parsing, and user authentication state changes.
 */
object WatchlistManager {
    private const val TAG = "WatchlistManager"
    private const val NODE_WATCHLIST = "watchlist" // Corresponds to Firebase rules structure

    private val DATABASE_URL = BuildConfig.FIREBASE_DB_URL

    private val _watchlistFlow = MutableStateFlow<List<Movie>>(emptyList())
    /**
     * A StateFlow emitting the current list of movies in the user's watchlist.
     */
    val watchlistFlow: StateFlow<List<Movie>> = _watchlistFlow

    // Reference to the active listener to allow removal on user change
    private var valueEventListener: ValueEventListener? = null

    // Helper variable for the current database reference
    private var currentDatabaseRef: com.google.firebase.database.DatabaseReference? = null

    init {
        // Listen for authentication state changes to init or clear data
        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d(TAG, "User detected: ${user.uid}. Initializing watchlist.")
                setupFirebaseListener(user.uid)
            } else {
                Log.d(TAG, "No user. Clearing watchlist.")
                clearWatchlist()
            }
        }
    }

    /**
     * Manually triggers a refresh of the listener for the current user.
     */
    fun refreshUser() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) setupFirebaseListener(user.uid)
    }

    private fun setupFirebaseListener(userId: String) {
        // 1. Remove existing listener if any
        if (currentDatabaseRef != null && valueEventListener != null) {
            currentDatabaseRef!!.removeEventListener(valueEventListener!!)
        }

        try {
            // 2. Create new reference: watchlist / {USER_ID}
            val dbInstance = FirebaseDatabase.getInstance(DATABASE_URL)
            currentDatabaseRef = dbInstance.getReference(NODE_WATCHLIST).child(userId)

            // 3. Define the listener
            valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Movie>()
                    for (child in snapshot.children) {
                        try {
                            val movie = child.getValue(Movie::class.java)
                            if (movie != null) {
                                list.add(movie)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Parsing error: ${e.message}")
                        }
                    }
                    // Reverse list to show newest additions first
                    _watchlistFlow.value = list.reversed()
                    Log.d(TAG, "Retrieved ${list.size} movies for user $userId")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase error: ${error.message}")
                }
            }

            // 4. Attach listener
            currentDatabaseRef!!.addValueEventListener(valueEventListener!!)

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL DATABASE ERROR", e)
        }
    }

    private fun clearWatchlist() {
        if (currentDatabaseRef != null && valueEventListener != null) {
            currentDatabaseRef!!.removeEventListener(valueEventListener!!)
        }
        _watchlistFlow.value = emptyList()
        currentDatabaseRef = null
    }

    /**
     * Adds a movie to the current user's watchlist.
     *
     * @param movie The Movie object to save.
     */
    fun addToWatchlist(movie: Movie) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || currentDatabaseRef == null) {
            Log.e(TAG, "Cannot add - user not logged in.")
            return
        }

        val key = encodeUrlToKey(movie.moviePageUrl)

        // Write directly to the user's reference
        currentDatabaseRef!!.child(key).setValue(movie)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Added movie to account ${user.uid}")
                } else {
                    Log.e(TAG, "Write error", task.exception)
                }
            }
    }

    /**
     * Removes a movie from the watchlist based on its page URL.
     *
     * @param moviePageUrl The unique URL of the movie.
     */
    fun removeFromWatchlist(moviePageUrl: String) {
        if (currentDatabaseRef == null) return

        val key = encodeUrlToKey(moviePageUrl)
        currentDatabaseRef!!.child(key).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d(TAG, "Movie removed")
            }
    }

    /**
     * Encodes a URL into a safe string format for use as a Firebase Database key.
     * Replaces forbidden characters (/, +, ., #, $, [, ]).
     */
    private fun encodeUrlToKey(url: String): String {
        return Base64.encodeToString(url.toByteArray(), Base64.NO_WRAP)
            .replace("/", "_")
            .replace("+", "-")
            .replace("=", "")
            .replace(".", "")
            .replace("#", "")
            .replace("$", "")
            .replace("[", "")
            .replace("]", "")
    }
}