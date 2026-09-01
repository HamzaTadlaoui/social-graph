package io.github.hamzatadlaoui.socialgraph.model

/**
 * The kinds of tie the app understands out of the box. [CUSTOM] carries its own
 * label instead, for everything the list does not cover.
 */
enum class RelationshipType {
    PARENT_OF,
    CHILD_OF,
    SIBLING_OF,
    PARTNER_OF,
    EX_PARTNER_OF,
    FRIEND_OF,
    COWORKER_OF,
    EMPLOYER_OF,
    EMPLOYEE_OF,
    NEIGHBOUR_OF,
    KNOWS,
    CUSTOM;

    /**
     * The tie as seen from the other end. Saying "David is the parent of Alex"
     * is the same act as saying "Alex is the child of David", and section 4.2 is
     * clear that the user should only have to say it once.
     */
    val inverse: RelationshipType
        get() = when (this) {
            PARENT_OF -> CHILD_OF
            CHILD_OF -> PARENT_OF
            EMPLOYER_OF -> EMPLOYEE_OF
            EMPLOYEE_OF -> EMPLOYER_OF
            // The rest read the same from either end: if I am your sibling,
            // you are mine.
            SIBLING_OF, PARTNER_OF, EX_PARTNER_OF, FRIEND_OF,
            COWORKER_OF, NEIGHBOUR_OF, KNOWS, CUSTOM,
            -> this
        }

    /** True when the tie reads the same from both ends. */
    val isSymmetric: Boolean get() = inverse == this

    /** The ties the family tree is built out of; the graph view uses them all. */
    val isFamily: Boolean
        get() = this == PARENT_OF || this == CHILD_OF || this == SIBLING_OF ||
            this == PARTNER_OF || this == EX_PARTNER_OF

    companion object {
        fun fromName(name: String?): RelationshipType? = entries.firstOrNull { it.name == name }
    }
}

/** How sure the user is that a tie or a fact is true (section 4.2). */
enum class Certainty {
    SURE,
    PROBABLE,
    UNSURE;

    companion object {
        fun fromName(name: String?): Certainty? = entries.firstOrNull { it.name == name }
    }
}
