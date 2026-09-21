package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.DocumentTagEntity
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.graph.Kinship
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.ui.person.ImpliedTie
import io.github.hamzatadlaoui.socialgraph.ui.person.PersonProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonProfileViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun typeBetween(
        repository: FakePeopleRepository,
        fromId: String,
        toId: String,
    ): RelationshipType? =
        repository.snapshot().second.firstOrNull { it.fromId == fromId && it.toId == toId }?.type

    @Test
    fun `confirming a sibling's parent records a real parent tie`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val david = PersonEntity(displayName = "David")
        val repository = FakePeopleRepository(listOf(alex, david))

        val viewModel = PersonProfileViewModel(repository, alex.id)
        viewModel.confirmImplied(ImpliedTie(david, Kinship.SIBLINGS_PARENT, null), "Sibling's parent")

        assertEquals(RelationshipType.PARENT_OF, typeBetween(repository, david.id, alex.id))
        assertEquals(RelationshipType.CHILD_OF, typeBetween(repository, alex.id, david.id))
    }

    @Test
    fun `confirming a worked-out sibling records a real sibling tie`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val sam = PersonEntity(displayName = "Sam")
        val repository = FakePeopleRepository(listOf(alex, sam))

        val viewModel = PersonProfileViewModel(repository, alex.id)
        viewModel.confirmImplied(ImpliedTie(sam, Kinship.SIBLING, null), "Sibling")

        assertEquals(RelationshipType.SIBLING_OF, typeBetween(repository, alex.id, sam.id))
        assertEquals(RelationshipType.SIBLING_OF, typeBetween(repository, sam.id, alex.id))
    }

    @Test
    fun `confirming an aunt or uncle, which has no built-in type, falls back to a custom label`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val marie = PersonEntity(displayName = "Marie")
        val repository = FakePeopleRepository(listOf(alex, marie))

        val viewModel = PersonProfileViewModel(repository, alex.id)
        viewModel.confirmImplied(ImpliedTie(marie, Kinship.AUNT_OR_UNCLE, null), "Aunt or uncle")

        assertEquals(RelationshipType.CUSTOM, typeBetween(repository, alex.id, marie.id))
    }

    @Test
    fun `adding a fact records it under the given category`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val repository = FakePeopleRepository(listOf(alex))
        val viewModel = PersonProfileViewModel(repository, alex.id)

        viewModel.addFact("background", "started a new job")

        val facts = repository.factSnapshot()
        assertEquals(1, facts.size)
        assertEquals("background", facts.first().category)
        assertEquals("started a new job", facts.first().text)
    }

    @Test
    fun `blank fact text is not recorded`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val repository = FakePeopleRepository(listOf(alex))
        val viewModel = PersonProfileViewModel(repository, alex.id)

        viewModel.addFact("background", "   ")

        assertTrue(repository.factSnapshot().isEmpty())
    }

    @Test
    fun `deleting a fact removes it`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val repository = FakePeopleRepository(listOf(alex))
        val viewModel = PersonProfileViewModel(repository, alex.id)
        viewModel.addFact("background", "started a new job")
        val id = repository.factSnapshot().first().id

        viewModel.deleteFact(id)

        assertTrue(repository.factSnapshot().isEmpty())
    }

    @Test
    fun `tagged photos pair each tag with its document, skipping non-images`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val repository = FakePeopleRepository(listOf(alex))
        val photo = DocumentEntity(id = "doc-1", fileName = "a.jpg", mimeType = "image/jpeg")
        val pdf = DocumentEntity(id = "doc-2", fileName = "b.pdf", mimeType = "application/pdf")
        repository.saveDocument(photo)
        repository.saveDocument(pdf)
        repository.tag(DocumentTagEntity(documentId = photo.id, personId = alex.id))
        repository.tag(DocumentTagEntity(documentId = pdf.id, personId = alex.id))

        val viewModel = PersonProfileViewModel(repository, alex.id)

        // taggedPhotos is only shared WhileSubscribed: the first value a new
        // collector sees can still be its empty seed, before the upstream
        // combine has had a turn to run, so wait for the real one rather
        // than trusting whichever value arrives first.
        val tagged = viewModel.taggedPhotos.first { it.isNotEmpty() }

        assertEquals(1, tagged.size)
        assertEquals(photo.id, tagged.first().document.id)
    }
}
