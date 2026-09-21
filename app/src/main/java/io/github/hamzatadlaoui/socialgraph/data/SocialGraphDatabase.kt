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
        FactEntity::class,
        EventEntity::class,
        EventAttendeeEntity::class,
        EventPhotoEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SocialGraphDatabase : RoomDatabase() {

    abstract fun people(): PersonDao

    abstract fun relationships(): RelationshipDao

    abstract fun documents(): DocumentDao

    abstract fun facts(): FactDao

    abstract fun events(): EventDao

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

        /** Version 3 adds one table for short categorised facts about a person. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `facts` (
                        `id` TEXT NOT NULL,
                        `personId` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`personId`) REFERENCES `people`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_facts_personId` ON `facts` (`personId`)",
                )
            }
        }

        /** Version 4 adds a person's address and occupation - two plain columns. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `people` ADD COLUMN `address` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `occupation` TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Version 5 adds events: a real occasion with a date, a place, who was
         * there and photos of it - fed onto a person's profile the same way a
         * tie is, rather than typed straight onto it. Same shape as version 2's
         * two document tables, one extra table for the photos.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `events` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `event_attendees` (
                        `id` TEXT NOT NULL,
                        `eventId` TEXT NOT NULL,
                        `personId` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`eventId`) REFERENCES `events`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`personId`) REFERENCES `people`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_event_attendees_eventId` " +
                        "ON `event_attendees` (`eventId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_event_attendees_personId` " +
                        "ON `event_attendees` (`personId`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `event_photos` (
                        `id` TEXT NOT NULL,
                        `eventId` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`eventId`) REFERENCES `events`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_event_photos_eventId` " +
                        "ON `event_photos` (`eventId`)",
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { instance = it }
        }
    }
}
