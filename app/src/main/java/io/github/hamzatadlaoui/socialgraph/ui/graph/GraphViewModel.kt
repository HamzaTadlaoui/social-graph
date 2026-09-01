package io.github.hamzatadlaoui.socialgraph.ui.graph

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.PeopleRepository
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.graph.Edge
import io.github.hamzatadlaoui.socialgraph.graph.PeopleGraph
import io.github.hamzatadlaoui.socialgraph.graph.Point
import io.github.hamzatadlaoui.socialgraph.graph.radialLayout
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Which ties to draw. Section 5.3 asks for a filter; these are the useful cuts. */
enum class TieFilter(@param:StringRes val label: Int) {
    ALL(R.string.filter_all),
    FAMILY(R.string.filter_family),
    SOCIAL(R.string.filter_social),
    WORK(R.string.filter_work);

    fun accepts(type: RelationshipType): Boolean = when (this) {
        ALL -> true
        FAMILY -> type.isFamily
        SOCIAL -> type == RelationshipType.FRIEND_OF ||
            type == RelationshipType.NEIGHBOUR_OF ||
            type == RelationshipType.KNOWS
        WORK -> type == RelationshipType.COWORKER_OF ||
            type == RelationshipType.EMPLOYER_OF ||
            type == RelationshipType.EMPLOYEE_OF
    }
}

/** A person as drawn: where they sit, and how far out from the centre. */
data class GraphNode(val person: PersonEntity, val at: Point, val depth: Int)

data class GraphState(
    val root: PersonEntity? = null,
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<Edge> = emptyList(),
    val isEmpty: Boolean = true,
)

class GraphViewModel(private val repository: PeopleRepository) : ViewModel() {

    /** Null until the user picks; the centre then falls back to "me". */
    private val chosenRoot = MutableStateFlow<String?>(null)

    var depth by mutableStateOf(2)
        private set

    var filter by mutableStateOf(TieFilter.ALL)
        private set

    private val settings = MutableStateFlow(depth to filter)

    val state: StateFlow<GraphState> = combine(
        repository.people(),
        repository.allRelationships(),
        chosenRoot,
        settings,
    ) { people, relationships, chosen, (depth, filter) ->
        // Nobody in the database yet: nothing to draw and nothing to centre on.
        val root = people.firstOrNull { it.id == chosen }
            ?: people.firstOrNull { it.isMe }
            ?: people.firstOrNull()
            ?: return@combine GraphState()

        val graph = PeopleGraph(
            relationships.map { Edge(it.pairId, it.fromId, it.toId, it.type) },
        )
        val network = graph.egoNetwork(root.id, depth, filter::accepts)
        val places = radialLayout(network)
        val byId = people.associateBy { it.id }

        GraphState(
            root = root,
            nodes = network.ids.mapNotNull { id ->
                val person = byId[id] ?: return@mapNotNull null
                GraphNode(person, places[id] ?: Point(0f, 0f), network.depth[id] ?: 0)
            },
            edges = network.edges,
            isEmpty = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphState())

    fun centreOn(personId: String) {
        chosenRoot.value = personId
    }

    fun onDepthChange(value: Int) {
        depth = value
        settings.value = value to filter
    }

    fun onFilterChange(value: TieFilter) {
        filter = value
        settings.value = depth to value
    }
}
