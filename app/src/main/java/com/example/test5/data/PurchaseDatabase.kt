package com.example.test5.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.test5.models.Purchase

@Database(entities = [Purchase::class], version = 3, exportSchema = false)
abstract class PurchaseDatabase : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        @Volatile
        private var INSTANCE: PurchaseDatabase? = null

        fun getDatabase(context: Context): PurchaseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PurchaseDatabase::class.java,
                    "purchase_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)// Добавляем миграцию
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Миграция 1 -> 2: добавляем поле category
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Создаём новую таблицу без поля quantity
                db.execSQL("""
                    CREATE TABLE new_purchases (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        price REAL NOT NULL,
                        category TEXT NOT NULL
                    )
                """)

                // Копируем данные из старой таблицы в новую
                db.execSQL("""
                    INSERT INTO new_purchases (id, name, price, category)
                    SELECT id, name, price, 'Uncategorized' FROM purchases
                """)

                // Удаляем старую таблицу
                db.execSQL("DROP TABLE purchases")

                // Переименовываем новую таблицу в старую
                db.execSQL("ALTER TABLE new_purchases RENAME TO purchases")
            }

        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            ALTER TABLE purchases ADD COLUMN date TEXT NOT NULL DEFAULT 'не указано'
        """.trimIndent())
            }
        }
    }
}

