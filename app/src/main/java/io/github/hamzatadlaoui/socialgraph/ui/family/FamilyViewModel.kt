package io.github.hamzatadlaoui.socialgraph.ui.family

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.graph.Edge
import io.github.hamzatadlaoui.socialgraph.graph.PeopleGraph
import io.github.hamzatadlaoui.socialgraph.graph.TreePlace
import io.github.hamzatadlaoui.socialgraph.graph.familyTree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A box in the tree: where it goes, and who is in it. */
data class TreeNode(val person: PersonEntity, val place: TreePlace)

data class FamilyState(
    val root: PersonEntity? = null,
    val nodes: List<TreeNode> = emptyList(),
    val couples: List<Pair<String, String>> = emptyList(),
    val descents: List<Pair<String, String>> = emptyList(),
    val isEmpty: Boolean = true,
)

class FamilyViewModel(private val repository: PeopleRepository) : ViewModel() {

    private val chosenRoot = MutableStateFlow<String?>(null)

    /** How many generations either way; section 5.4's depth control. */
    var generations by mutableStateOf(2)
        private set

    private val depth = MutableStateFlow(generations)

    val state: StateFlow<FamilyState> = combine(
        repository.people(),
        repository.allRelationships(),
        chosenRoot,
        depth,
    ) { people, relationships, chosen, depth ->
        val root = people.firstOrNull { it.id == chosen }
            ?: people.firstOrNull { it.isMe }
            ?: people.firstOrNull()
            ?: return@combine FamilyState()

        val graph = PeopleGraph(relationships.map { Edge(it.pairId, it.fromId, it.toId, it.type) })
        val tree = familyTree(graph, root.id, up = depth, down = depth)
        val byId = people.associateBy { it.id }

        FamilyState(
            root = root,
            nodes = tree.places.mapNotNull { place ->
                byId[place.id]?.let { TreeNode(it, place) }
            },
            couples = tree.couples,
            descents = tree.descents,
            isEmpty = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyState())

    fun rootOn(personId: String) {
        chosenRoot.value = personId
    }

    fun onGenerationsChange(value: Int) {
        generations = value
        depth.value = value
    }
}
