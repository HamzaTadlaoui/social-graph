package io.github.hamzatadlaoui.socialgraph.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PersonEntity::class,
        RelationshipEntity::class,
        DocumentEntity::class,
        DocumentTagEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SocialGraphDatabase : RoomDatabase() {

    abstract fun people(): PersonDao

    abstract fun relationships(): RelationshipDao

    abstract fun documents(): DocumentDao

    companion object {
        @Volatile
        private var instance: SocialGraphDatabase? = null

        /**
         * Version 2 adds the document shelf. Written out by hand rather than
         * left to a destructive fallback: version 1 is already on phones, and
         * the whole point of this app is that it does not lose what you put in
         * it. Nothing existing is touched - two new tables, and that is all.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `documents` (
                        `id` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `originalName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `dated` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `document_tags` (
                        `id` TEXT NOT NULL,
                        `documentId` TEXT NOT NULL,
                        `personId` TEXT NOT NULL,
                        `left` REAL NOT NULL,
                        `top` REAL NOT NULL,
                        `right` REAL NOT NULL,
                        `bottom` REAL NOT NULL,
                        `note` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`personId`) REFERENCES `people`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_document_tags_documentId` " +
                        "ON `document_tags` (`documentId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_document_tags_personId` " +
                        "ON `document_tags` (`personId`)",
                )
            }
        }

        fun get(context: Context): SocialGraphDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SocialGraphDatabase::class.java,
                "social-graph.db",
            )
                // Foreign keys are on by default in Room, which is what makes
                // deleting a person take their ties - and their tags - with them.
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
