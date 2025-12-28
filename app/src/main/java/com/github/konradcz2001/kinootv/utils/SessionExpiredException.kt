package com.github.konradcz2001.kinootv.utils

/**
 * Custom exception thrown when the scraper detects that the current session is invalid or expired,
 * requiring the user to re-authenticate.
 */
class SessionExpiredException : Exception("Session expired. Re-login required.")