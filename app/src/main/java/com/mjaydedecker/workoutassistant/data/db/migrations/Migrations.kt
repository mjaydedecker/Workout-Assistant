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

/**
 * Migration 2 → 3:
 *  - workout_day_exercises: add sets (INTEGER NOT NULL DEFAULT 3), populated from exercises.defaultSets
 *  - exercises: add isCustom, force, equipment, primaryMuscles, secondaryMuscles,
 *               description, videoUrls, imageRefs columns
 *  - exercises: rebuild table to remove defaultSets (SQLite table-rebuild pattern,
 *               required for minSdk 26 compatibility — DROP COLUMN needs API 35+)
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Add sets to workout_day_exercises, copying from exercises.defaultSets
        db.execSQL(
            "ALTER TABLE workout_day_exercises ADD COLUMN sets INTEGER NOT NULL DEFAULT 3"
        )
        db.execSQL(
            """UPDATE workout_day_exercises
               SET sets = (
                   SELECT defaultSets FROM exercises
                   WHERE exercises.id = workout_day_exercises.exerciseId
               )"""
        )

        // Step 2: Add new nullable columns to exercises
        db.execSQL("ALTER TABLE exercises ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE exercises ADD COLUMN force TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN equipment TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN primaryMuscles TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN secondaryMuscles TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN description TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN videoUrls TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN imageRefs TEXT")

        // Step 3: Rebuild exercises table to remove defaultSets
        db.execSQL(
            """CREATE TABLE exercises_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                isCustom INTEGER NOT NULL DEFAULT 1,
                force TEXT,
                equipment TEXT,
                primaryMuscles TEXT,
                secondaryMuscles TEXT,
                description TEXT,
                videoUrls TEXT,
                imageRefs TEXT
            )"""
        )
        db.execSQL(
            """INSERT INTO exercises_new
               SELECT id, name, isCustom, force, equipment,
                      primaryMuscles, secondaryMuscles, description, videoUrls, imageRefs
               FROM exercises"""
        )
        db.execSQL("DROP TABLE exercises")
        db.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
