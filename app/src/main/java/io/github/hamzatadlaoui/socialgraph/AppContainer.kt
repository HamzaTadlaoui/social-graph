package io.github.hamzatadlaoui.socialgraph

import android.content.Context
import io.github.hamzatadlaoui.socialgraph.data.DocumentStore
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.data.RoomPeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.SocialGraphDatabase

/**
 * The one place the app is wired together. Small enough not to want a
 * dependency-injection library, and explicit enough to read in one sitting.
 */
class AppContainer(context: Context) {

    val repository: PeopleRepository = RoomPeopleRepository(SocialGraphDatabase.get(context))

    val photos: PhotoStore = PhotoStore(context)

    val documents: DocumentStore = DocumentStore(context)
}
