package com.mjaydedecker.workoutassistant.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 1 → 2:
 *  - workout_sessions: add totalPausedSeconds (INTEGER NOT NULL DEFAULT 0)
 *  - app_settings: add weightUnit (TEXT NOT NULL DEFAULT 'KG')
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE workout_sessions ADD COLUMN totalPausedSeconds INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE app_settings ADD COLUMN weightUnit TEXT NOT NULL DEFAULT 'KG'"
        )
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
