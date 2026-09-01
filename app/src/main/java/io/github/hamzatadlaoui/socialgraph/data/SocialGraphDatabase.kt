package io.github.hamzatadlaoui.socialgraph.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PersonEntity::class, RelationshipEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SocialGraphDatabase : RoomDatabase() {

    abstract fun people(): PersonDao

    abstract fun relationships(): RelationshipDao

    companion object {
        @Volatile
        private var instance: SocialGraphDatabase? = null

        fun get(context: Context): SocialGraphDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SocialGraphDatabase::class.java,
                "social-graph.db",
            )
                // Foreign keys are on by default in Room, which is what makes
                // deleting a person take their ties with them.
                .build()
                .also { instance = it }
        }
    }
}
