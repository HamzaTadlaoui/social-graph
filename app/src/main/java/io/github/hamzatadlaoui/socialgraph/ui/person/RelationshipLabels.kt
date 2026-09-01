package io.github.hamzatadlaoui.socialgraph.ui.person

import androidx.annotation.StringRes
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType

/**
 * A tie is stored pointing away from the person whose page you are on, so the
 * heading it sits under is the *other* end of it: a row saying "Claire is the
 * parent of Alex", read on Claire's page, belongs under "Children".
 */
@StringRes
fun RelationshipType.sectionTitle(): Int = when (this) {
    RelationshipType.PARENT_OF -> R.string.section_children
    RelationshipType.CHILD_OF -> R.string.section_parents
    RelationshipType.SIBLING_OF -> R.string.section_siblings
    RelationshipType.PARTNER_OF -> R.string.section_partners
    RelationshipType.EX_PARTNER_OF -> R.string.section_former_partners
    RelationshipType.FRIEND_OF -> R.string.section_friends
    RelationshipType.COWORKER_OF -> R.string.section_colleagues
    RelationshipType.EMPLOYER_OF -> R.string.section_employees
    RelationshipType.EMPLOYEE_OF -> R.string.section_employers
    RelationshipType.NEIGHBOUR_OF -> R.string.section_neighbours
    RelationshipType.KNOWS -> R.string.section_knows
    RelationshipType.CUSTOM -> R.string.section_other
}

/** What the user picks from when adding a tie: "is the parent of", "works with". */
@StringRes
fun RelationshipType.pickerLabel(): Int = when (this) {
    RelationshipType.PARENT_OF -> R.string.tie_parent_of
    RelationshipType.CHILD_OF -> R.string.tie_child_of
    RelationshipType.SIBLING_OF -> R.string.tie_sibling_of
    RelationshipType.PARTNER_OF -> R.string.tie_partner_of
    RelationshipType.EX_PARTNER_OF -> R.string.tie_ex_partner_of
    RelationshipType.FRIEND_OF -> R.string.tie_friend_of
    RelationshipType.COWORKER_OF -> R.string.tie_coworker_of
    RelationshipType.EMPLOYER_OF -> R.string.tie_employer_of
    RelationshipType.EMPLOYEE_OF -> R.string.tie_employee_of
    RelationshipType.NEIGHBOUR_OF -> R.string.tie_neighbour_of
    RelationshipType.KNOWS -> R.string.tie_knows
    RelationshipType.CUSTOM -> R.string.tie_custom
}

/**
 * Family first, and within it the order a person would say them out loud, then
 * everyone else. Keeps a long page from opening on "Knows".
 */
val relationshipSectionOrder: List<RelationshipType> = listOf(
    RelationshipType.CHILD_OF,
    RelationshipType.PARTNER_OF,
    RelationshipType.PARENT_OF,
    RelationshipType.SIBLING_OF,
    RelationshipType.EX_PARTNER_OF,
    RelationshipType.FRIEND_OF,
    RelationshipType.COWORKER_OF,
    RelationshipType.EMPLOYER_OF,
    RelationshipType.EMPLOYEE_OF,
    RelationshipType.NEIGHBOUR_OF,
    RelationshipType.KNOWS,
    RelationshipType.CUSTOM,
)
