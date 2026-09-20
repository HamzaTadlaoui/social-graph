package io.github.hamzatadlaoui.socialgraph.graph

import io.github.hamzatadlaoui.socialgraph.model.RelationshipType

/**
 * A tie nobody typed in, but which follows from the ones that were.
 *
 * Recording that David is Alex's parent and that David is Sam's parent says,
 * without anyone else having to say it, that Alex and Sam are siblings. The app
 * would rather work that out than ask twice.
 */
enum class Kinship {
    SIBLING,
    HALF_SIBLING,
    GRANDPARENT,
    GRANDCHILD,
    GREAT_GRANDPARENT,
    GREAT_GRANDCHILD,
    AUNT_OR_UNCLE,
    NIECE_OR_NEPHEW,
    GREAT_AUNT_OR_UNCLE,
    GREAT_NIECE_OR_NEPHEW,
    COUSIN,
    PARENT_IN_LAW,
    CHILD_IN_LAW,
    SIBLING_IN_LAW,
    PARENTS_PARTNER,
    PARTNERS_CHILD,
    STEP_SIBLING,
    SIBLINGS_PARENT,
}

/**
 * One implied tie: who, what, and the person it runs through, so the app can
 * say *why* rather than simply asserting it.
 */
data class Implied(
    val personId: String,
    val kinship: Kinship,
    /** Whoever the inference passes through, for "through Firdaws". */
    val throughId: String?,
)

/**
 * Everything that follows from what is already recorded about [personId].
 *
 * Deliberately conservative. Only descent, siblinghood and partnership are
 * treated as load-bearing; a friend of a friend is not a relation, and nothing
 * here guesses at anything a person would find presumptuous to be told. Ties
 * that are already recorded directly are never repeated back as inferences -
 * the point is to fill gaps, not to restate the database.
 *
 * Half-siblings are distinguished from full ones only when both parents of both
 * children are known: with one parent recorded there is no way to tell, so the
 * gentler [Kinship.SIBLING] is used.
 */
fun impliedKin(graph: PeopleGraph, personId: String): List<Implied> {
    val direct = graph.neighbours(personId).map { it.toId }.toSet()
    val found = LinkedHashMap<String, Implied>()

    fun offer(otherId: String, kinship: Kinship, throughId: String?) {
        if (otherId == personId || otherId in direct) return
        // The first way a relation is found is the closest, because they are
        // offered nearest-first below.
        found.putIfAbsent(otherId, Implied(otherId, kinship, throughId))
    }

    val parents = graph.parentsOf(personId)
    val children = graph.childrenOf(personId)
    val siblings = graph.siblingsOf(personId) + parents.flatMap { graph.childrenOf(it) } - personId
    val partners = graph.partnersOf(personId)

    // Brothers and sisters: anyone who shares a parent.
    for (parent in parents) {
        for (child in graph.childrenOf(parent)) {
            if (child == personId) continue
            val theirs = graph.parentsOf(child)
            val shared = parents.intersect(theirs.toSet())
            // Only call it a half-sibling when both sides are known well enough
            // to be sure one parent differs.
            val half = parents.size > 1 && theirs.size > 1 && shared.size == 1
            offer(child, if (half) Kinship.HALF_SIBLING else Kinship.SIBLING, parent)
        }
    }

    // Grandparents and grandchildren, then one more step out.
    for (parent in parents) {
        for (grandparent in graph.parentsOf(parent)) {
            offer(grandparent, Kinship.GRANDPARENT, parent)
            for (great in graph.parentsOf(grandparent)) {
                offer(great, Kinship.GREAT_GRANDPARENT, grandparent)
            }
            // A grandparent's sibling, one generation further out than an
            // aunt or uncle.
            for (greatAuntOrUncle in graph.siblingLike(grandparent)) {
                offer(greatAuntOrUncle, Kinship.GREAT_AUNT_OR_UNCLE, grandparent)
            }
        }
    }
    for (child in children) {
        for (grandchild in graph.childrenOf(child)) {
            offer(grandchild, Kinship.GRANDCHILD, child)
            for (great in graph.childrenOf(grandchild)) {
                offer(great, Kinship.GREAT_GRANDCHILD, grandchild)
            }
        }
    }

    // Aunts and uncles: a sibling of a parent. Nieces and nephews: the reverse.
    for (parent in parents) {
        for (auntOrUncle in graph.siblingLike(parent)) {
            offer(auntOrUncle, Kinship.AUNT_OR_UNCLE, parent)
            // Cousins hang off them.
            for (cousin in graph.childrenOf(auntOrUncle)) {
                offer(cousin, Kinship.COUSIN, auntOrUncle)
            }
        }
    }
    for (sibling in siblings.distinct()) {
        for (niblingId in graph.childrenOf(sibling)) {
            offer(niblingId, Kinship.NIECE_OR_NEPHEW, sibling)
            // One generation further out: a nibling's child.
            for (greatNiblingId in graph.childrenOf(niblingId)) {
                offer(greatNiblingId, Kinship.GREAT_NIECE_OR_NEPHEW, niblingId)
            }
        }
    }

    // In-laws, through a partner.
    for (partner in partners) {
        for (parentInLaw in graph.parentsOf(partner)) {
            offer(parentInLaw, Kinship.PARENT_IN_LAW, partner)
        }
        for (siblingInLaw in graph.siblingLike(partner)) {
            offer(siblingInLaw, Kinship.SIBLING_IN_LAW, partner)
        }
        // A partner's child who is not also yours.
        for (theirChild in graph.childrenOf(partner)) {
            if (theirChild !in children) offer(theirChild, Kinship.PARTNERS_CHILD, partner)
        }
    }
    for (child in children) {
        for (childInLaw in graph.partnersOf(child)) {
            offer(childInLaw, Kinship.CHILD_IN_LAW, child)
        }
    }
    // A parent's partner who is not also a parent: stated as exactly that
    // rather than as "step-parent", which is a thing for people to call
    // themselves rather than for a database to decide.
    for (parent in parents) {
        for (theirPartner in graph.partnersOf(parent)) {
            if (theirPartner !in parents) {
                offer(theirPartner, Kinship.PARENTS_PARTNER, parent)
            }
        }
    }

    // Step-siblings: another child of a parent's partner, who is not already
    // a blood sibling. Named for the peer relationship, not restated as "the
    // step-parent's child" - that reading belongs to the partner side, not
    // theirs. Kept last so it never outranks a closer blood-sibling reading
    // already offered above.
    for (parent in parents) {
        for (theirPartner in graph.partnersOf(parent)) {
            if (theirPartner in parents) continue
            for (theirChild in graph.childrenOf(theirPartner)) {
                if (theirChild == personId) continue
                if (theirChild in siblings) continue
                offer(theirChild, Kinship.STEP_SIBLING, theirPartner)
            }
        }
    }

    // A sibling's parent, when not also recorded as one's own - the mirror of
    // a partner's child, but through an explicitly recorded sibling instead
    // of a partner. A sibling sharing a recorded parent already puts that
    // parent in `direct` for both sides, so this only ever fires for a
    // sibling tie recorded on its own.
    for (sibling in graph.siblingsOf(personId)) {
        for (theirParent in graph.parentsOf(sibling)) {
            offer(theirParent, Kinship.SIBLINGS_PARENT, sibling)
        }
    }

    return found.values.toList()
}

private fun PeopleGraph.related(personId: String, type: RelationshipType): List<String> =
    neighbours(personId).filter { it.type == type }.map { it.toId }.distinct()

internal fun PeopleGraph.parentsOf(personId: String) = related(personId, RelationshipType.CHILD_OF)

internal fun PeopleGraph.childrenOf(personId: String) = related(personId, RelationshipType.PARENT_OF)

internal fun PeopleGraph.siblingsOf(personId: String) = related(personId, RelationshipType.SIBLING_OF)

internal fun PeopleGraph.partnersOf(personId: String) =
    related(personId, RelationshipType.PARTNER_OF)

/** Recorded siblings, plus anyone sharing a parent with them. */
internal fun PeopleGraph.siblingLike(personId: String): List<String> =
    (siblingsOf(personId) + parentsOf(personId).flatMap { childrenOf(it) })
        .distinct()
        .filter { it != personId }
