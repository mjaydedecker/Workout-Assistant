package com.mjaydedecker.workoutassistant.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("workoutDayId")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutDayId: Long?,
    val workoutDayName: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val durationSeconds: Long? = null,
    val totalPausedSeconds: Long = 0,
    val notes: String? = null
)
