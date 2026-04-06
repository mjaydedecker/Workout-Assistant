package com.mjaydedecker.workoutassistant.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mjaydedecker.workoutassistant.data.db.converters.InstantConverter
import com.mjaydedecker.workoutassistant.data.db.dao.AppSettingsDao
import com.mjaydedecker.workoutassistant.data.db.dao.ExerciseDao
import com.mjaydedecker.workoutassistant.data.db.dao.ExerciseDefaultWeightDao
import com.mjaydedecker.workoutassistant.data.db.dao.SessionExerciseDao
import com.mjaydedecker.workoutassistant.data.db.dao.WorkoutDayDao
import com.mjaydedecker.workoutassistant.data.db.dao.WorkoutDayExerciseDao
import com.mjaydedecker.workoutassistant.data.db.dao.WorkoutSessionDao
import com.mjaydedecker.workoutassistant.data.db.entity.AppSettingsEntity
import com.mjaydedecker.workoutassistant.data.db.entity.ExerciseDefaultWeightEntity
import com.mjaydedecker.workoutassistant.data.db.entity.ExerciseEntity
import com.mjaydedecker.workoutassistant.data.db.entity.SessionExerciseEntity
import com.mjaydedecker.workoutassistant.data.db.entity.WorkoutDayEntity
import com.mjaydedecker.workoutassistant.data.db.entity.WorkoutDayExerciseEntity
import com.mjaydedecker.workoutassistant.data.db.entity.WorkoutSessionEntity
import com.mjaydedecker.workoutassistant.data.db.migrations.ALL_MIGRATIONS

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutDayEntity::class,
        WorkoutDayExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        ExerciseDefaultWeightEntity::class,
        AppSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(InstantConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDayDao(): WorkoutDayDao
    abstract fun workoutDayExerciseDao(): WorkoutDayExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun sessionExerciseDao(): SessionExerciseDao
    abstract fun exerciseDefaultWeightDao(): ExerciseDefaultWeightDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_assistant.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
