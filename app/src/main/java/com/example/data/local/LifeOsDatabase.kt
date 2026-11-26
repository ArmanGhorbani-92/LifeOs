package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        TagEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        GoalEntity::class,
        MilestoneEntity::class,
        GoalProgressLogEntity::class,
        HabitEntity::class,
        HabitRecordEntity::class,
        CalendarEventEntity::class,
        NoteEntity::class,
        TimeRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LifeOsDatabase : RoomDatabase() {

    abstract fun lifeOsDao(): LifeOsDao

    companion object {
        private const val DATABASE_NAME = "lifeos_local.db"

        @Volatile
        private var INSTANCE: LifeOsDatabase? = null

        val MIGRATIONS: Array<Migration> = emptyArray()

        val defaultCategories: List<CategoryEntity> = listOf(
            CategoryEntity(id = "cat_personal", name = "شخصی", iconName = "person", colorHex = "#71805A", scope = "ALL"),
            CategoryEntity(id = "cat_work", name = "کاری", iconName = "work", colorHex = "#17263F", scope = "ALL"),
            CategoryEntity(id = "cat_study", name = "آموزش و مطالعه", iconName = "school", colorHex = "#5A7180", scope = "ALL"),
            CategoryEntity(id = "cat_health", name = "سلامتی و ورزش", iconName = "favorite", colorHex = "#5D805A", scope = "ALL"),
            CategoryEntity(id = "cat_finance", name = "مالی", iconName = "account_balance", colorHex = "#80755A", scope = "ALL"),
            CategoryEntity(id = "cat_family", name = "خانواده", iconName = "home", colorHex = "#6B745F", scope = "ALL")
        )

        fun getInstance(context: Context): LifeOsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeOsDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*MIGRATIONS)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.lifeOsDao()?.upsertCategories(defaultCategories)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
