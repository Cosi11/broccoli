package com.roulette.tracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Hier kannst du die Schemaänderungen für die Migration von Version 1 zu Version 2 definieren
        // Zum Beispiel:
        // database.execSQL("ALTER TABLE simulation_results ADD COLUMN new_column INTEGER")
    }
} 