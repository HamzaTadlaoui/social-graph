package io.github.hamzatadlaoui.socialgraph.ui.person

import androidx.annotation.StringRes
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.graph.Kinship

/**
 * What to call each worked-out relation on screen.
 *
 * Kept beside [RelationshipLabels] and for the same reason: the graph package
 * knows nothing about string resources, and should not have to.
 */
@StringRes
fun Kinship.label(): Int = when (this) {
    Kinship.SIBLING -> R.string.kin_sibling
    Kinship.HALF_SIBLING -> R.string.kin_half_sibling
    Kinship.GRANDPARENT -> R.string.kin_grandparent
    Kinship.GRANDCHILD -> R.string.kin_grandchild
    Kinship.GREAT_GRANDPARENT -> R.string.kin_great_grandparent
    Kinship.GREAT_GRANDCHILD -> R.string.kin_great_grandchild
    Kinship.AUNT_OR_UNCLE -> R.string.kin_aunt_or_uncle
    Kinship.NIECE_OR_NEPHEW -> R.string.kin_niece_or_nephew
    Kinship.COUSIN -> R.string.kin_cousin
    Kinship.PARENT_IN_LAW -> R.string.kin_parent_in_law
    Kinship.CHILD_IN_LAW -> R.string.kin_child_in_law
    Kinship.SIBLING_IN_LAW -> R.string.kin_sibling_in_law
    Kinship.PARENTS_PARTNER -> R.string.kin_parents_partner
    Kinship.PARTNERS_CHILD -> R.string.kin_partners_child
}
