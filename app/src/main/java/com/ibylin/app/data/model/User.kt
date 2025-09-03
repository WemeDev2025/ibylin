package com.ibylin.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Entity(tableName = "users")
@Parcelize
data class User(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val avatar: String? = null
) : Parcelable
