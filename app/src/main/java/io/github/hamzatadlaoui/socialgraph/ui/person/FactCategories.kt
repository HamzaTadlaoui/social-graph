package io.github.hamzatadlaoui.socialgraph.ui.person

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.hamzatadlaoui.socialgraph.R

/**
 * The categories a fact can sit under out of the box - the one free-text
 * one Orwell's own per-category profile lists use that this app hasn't
 * given its own structured tab. Activities used to be one of these too,
 * but an activity is a real occasion with a date and people at it, not a
 * line of text - see [io.github.hamzatadlaoui.socialgraph.data.EventEntity]
 * and [io.github.hamzatadlaoui.socialgraph.ui.person.ProfileTab.Events],
 * which now cover that instead.
 * A category is otherwise nothing more than whatever string
 * [io.github.hamzatadlaoui.socialgraph.data.FactEntity.category] holds;
 * this one is simply offered up front and always shown, even with nothing
 * filed under it yet, so there is somewhere obvious to start. Anything
 * else the person types becomes a category of its own the moment a fact
 * is saved under it - see [CUSTOM_CATEGORY_ICON].
 */
enum class FactCategory(val id: String, @param:StringRes val label: Int, val icon: ImageVector) {
    PERSONALITY("personality", R.string.fact_category_personality, Icons.Default.Psychology),
    ;

    companion object {
        fun byId(id: String): FactCategory? = entries.firstOrNull { it.id == id }
    }
}

/** What a category the person typed themselves gets in the rail. */
val CUSTOM_CATEGORY_ICON: ImageVector = Icons.AutoMirrored.Filled.Label
