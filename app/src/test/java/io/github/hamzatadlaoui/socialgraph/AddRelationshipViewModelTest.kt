package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.ui.person.AddRelationshipViewModel
import io.github.hamzatadlaoui.socialgraph.ui.person.FollowUpKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class AddRelationshipViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `linking a child to someone with one partner asks about the partner`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val sam = PersonEntity(displayName = "Sam")
        val repository = FakePeopleRepository(listOf(alex, sam))
        repository.link(alex.id, sam.id, RelationshipType.PARTNER_OF)

        val viewModel = AddRelationshipViewModel(repository, alex.id)
        viewModel.onTypeChange(RelationshipType.PARENT_OF)

        var done = false
        viewModel.createAndLink("Robin") { done = true }

        assertEquals(false, done)
        val prompt = viewModel.pendingPrompts.value.single()
        assertEquals(FollowUpKind.CHILD_OF_PARTNER, prompt.kind)
        assertEquals("Sam", prompt.candidateName)
        assertEquals("Robin", prompt.newPersonName)
    }

    @Test
    fun `a partner already recorded as the child's parent is not asked about again`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val sam = PersonEntity(displayName = "Sam")
        val robin = PersonEntity(displayName = "Robin")
        val repository = FakePeopleRepository(listOf(alex, sam, robin))
        repository.link(alex.id, sam.id, RelationshipType.PARTNER_OF)
        repository.link(sam.id, robin.id, RelationshipType.PARENT_OF)

        val viewModel = AddRelationshipViewModel(repository, alex.id)
        viewModel.onTypeChange(RelationshipType.PARENT_OF)

        var done = false
        viewModel.linkTo(robin.id) { done = true }

        assertTrue(done)
        assertTrue(viewModel.pendingPrompts.value.isEmpty())
    }

    @Test
    fun `each candidate is resolved in turn before the caller is told it is done`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val childA = PersonEntity(displayName = "Childa")
        val childB = PersonEntity(displayName = "Childb")
        val repository = FakePeopleRepository(listOf(alex, childA, childB))
        repository.link(alex.id, childA.id, RelationshipType.PARENT_OF)
        repository.link(alex.id, childB.id, RelationshipType.PARENT_OF)

        val viewModel = AddRelationshipViewModel(repository, alex.id)
        viewModel.onTypeChange(RelationshipType.PARTNER_OF)

        var done = false
        viewModel.createAndLink("Sam") { done = true }

        assertEquals(2, viewModel.pendingPrompts.value.size)

        viewModel.confirmPrompt(viewModel.pendingPrompts.value.first())
        assertEquals(1, viewModel.pendingPrompts.value.size)
        assertEquals(false, done)

        viewModel.declinePrompt(viewModel.pendingPrompts.value.first())
        assertTrue(viewModel.pendingPrompts.value.isEmpty())
        assertTrue(done)
    }

    @Test
    fun `declining a follow-up still lets the caller move on`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val david = PersonEntity(displayName = "David")
        val repository = FakePeopleRepository(listOf(alex, david))
        repository.link(david.id, alex.id, RelationshipType.PARENT_OF)

        val viewModel = AddRelationshipViewModel(repository, alex.id)
        viewModel.onTypeChange(RelationshipType.SIBLING_OF)

        var done = false
        viewModel.createAndLink("Sam") { done = true }

        val prompt = viewModel.pendingPrompts.value.single()
        assertEquals(FollowUpKind.PARENT_OF_SIBLING, prompt.kind)

        viewModel.declinePrompt(prompt)
        assertTrue(done)
    }

    @Test
    fun `a relationship type with no follow-up finishes immediately`() = runTest {
        val alex = PersonEntity(displayName = "Alex")
        val repository = FakePeopleRepository(listOf(alex))

        val viewModel = AddRelationshipViewModel(repository, alex.id)
        viewModel.onTypeChange(RelationshipType.FRIEND_OF)

        var done = false
        viewModel.createAndLink("Sam") { done = true }

        assertTrue(done)
        assertTrue(viewModel.pendingPrompts.value.isEmpty())
    }
}
