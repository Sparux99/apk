package com.amine.player

import android.net.Uri

data class Video(
    val id: Long,
    val title: String,
    val duration: Long,
    val contentUri: Uri,
    val dateAdded: Long 
)