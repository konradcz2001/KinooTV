package com.github.konradcz2001.kinootv.data

import java.io.Serializable

/**
 * Represents a user comment.
 *
 * @property depth Indicates the nesting level of the comment (0 = root, 1 = reply, etc.).
 */
data class Comment(
    val author: String,
    val date: String,
    val text: String,
    val depth: Int = 0
) : Serializable