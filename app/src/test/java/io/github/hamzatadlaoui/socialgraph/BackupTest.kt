package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.DocumentTagEntity
import io.github.hamzatadlaoui.socialgraph.data.EventAttendeeEntity
import io.github.hamzatadlaoui.socialgraph.data.EventEntity
import io.github.hamzatadlaoui.socialgraph.data.FactEntity
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.RelationshipEntity
import io.github.hamzatadlaoui.socialgraph.export.Backup
import io.github.hamzatadlaoui.socialgraph.model.Certainty
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {

    private val claire = PersonEntity(
        id = "claire",
        displayName = "Claire",
        lastName = "Martin",
        nickname = "Clo",
        photo = "claire.jpg",
        notes = "Met at university.",
        birth = FuzzyDate(1982, approximate = true),
        pronouns = "she/her",
        address = "22 Rue de Lyon",
        occupation = "Architect",
        isMe = false,
        isFavourite = true,
        createdAt = 1_700_000_000_000,
        updatedAt = 1_700_000_001_000,
    )

    private val marc = PersonEntity(id = "marc", displayName = "Marc")

    private val married = RelationshipEntity(
        id = "tie-1",
        pairId = "pair-1",
        fromId = "claire",
        toId = "marc",
        type = RelationshipType.PARTNER_OF,
        start = FuzzyDate(2004, 6),
        notes = "in Lyon",
        certainty = Certainty.PROBABLE,
    )

    @Test
    fun `everyone and every tie survives the trip through JSON`() {
        val json = Backup.toJson(listOf(claire, marc), listOf(married))
        val reread = JSONObject(json.toString())

        assertEquals(listOf(claire, marc), Backup.peopleFrom(reread))
        assertEquals(listOf(married), Backup.relationshipsFrom(reread))
    }

    @Test
    fun `half-known dates come back half-known, not guessed at`() {
        val reread = JSONObject(Backup.toJson(listOf(claire), listOf(married)).toString())

        assertEquals(FuzzyDate(1982, approximate = true), Backup.peopleFrom(reread).first().birth)
        assertEquals(FuzzyDate(2004, 6), Backup.relationshipsFrom(reread).first().start)
        assertEquals(FuzzyDate.Unknown, Backup.peopleFrom(reread).first().death)
    }

    @Test
    fun `the file says which version wrote it`() {
        assertEquals(Backup.VERSION, Backup.toJson(emptyList(), emptyList()).getInt("version"))
    }

    @Test
    fun `a damaged entry is skipped rather than losing the whole backup`() {
        val json = JSONObject(
            """
            {
              "version": 1,
              "people": [
                {"id": "", "displayName": "No id"},
                {"displayName": "No id either"},
                {"id": "ok", "displayName": "Ada"}
              ],
              "relationships": [
                {"id": "broken"},
                {"id": "t", "fromId": "ok", "toId": "other", "type": "NOT_A_TYPE"}
              ]
            }
            """.trimIndent(),
        )

        val people = Backup.peopleFrom(json)
        assertEquals(1, people.size)
        assertEquals("Ada", people.first().displayName)

        val ties = Backup.relationshipsFrom(json)
        assertEquals(1, ties.size)
        // An unreadable kind of tie is kept as a plain "knows" rather than dropped.
        assertEquals(RelationshipType.KNOWS, ties.first().type)
    }

    @Test
    fun `documents and their tags survive the round trip`() {
        val photo = DocumentEntity(
            id = "doc-1",
            fileName = "abc.jpg",
            originalName = "wedding.jpg",
            mimeType = "image/jpeg",
            title = "The wedding",
            notes = "Back garden.",
            dated = FuzzyDate(2004, 6),
            sizeBytes = 204_800,
            addedAt = 1_700_000_002_000,
        )
        val face = DocumentTagEntity(
            id = "tag-1",
            documentId = "doc-1",
            personId = "claire",
            left = 0.25f,
            top = 0.1f,
            right = 0.45f,
            bottom = 0.4f,
        )
        val wholeThing = DocumentTagEntity(id = "tag-2", documentId = "doc-1", personId = "marc")

        val json = Backup.toJson(
            people = listOf(claire, marc),
            relationships = listOf(married),
            documents = listOf(photo),
            tags = listOf(face, wholeThing),
        )
        val read = JSONObject(json.toString())

        assertEquals(listOf(photo), Backup.documentsFrom(read))

        val tags = Backup.tagsFrom(read)
        assertEquals(2, tags.size)
        // The region has to come back exactly, or every face moves.
        assertEquals(0.25f, tags.first().left, 0.0001f)
        assertEquals(0.4f, tags.first().bottom, 0.0001f)
        assertEquals(false, tags.first().whole)
        assertEquals(true, tags[1].whole)
    }

    @Test
    fun `a version one backup still restores, it simply has no documents`() {
        // Exactly what the previous release wrote: no documents, no documentTags.
        val old = JSONObject(
            """
            {
              "version": 1,
              "people": [{ "id": "ada", "displayName": "Ada" }],
              "relationships": []
            }
            """.trimIndent(),
        )

        assertEquals(1, Backup.peopleFrom(old).size)
        assertTrue(Backup.documentsFrom(old).isEmpty())
        assertTrue(Backup.tagsFrom(old).isEmpty())
    }

    @Test
    fun `facts survive the round trip`() {
        val fact = FactEntity(
            id = "fact-1",
            personId = "claire",
            category = "background",
            text = "started working at the university in March",
            createdAt = 1_700_000_003_000,
        )

        val json = Backup.toJson(
            people = listOf(claire),
            relationships = emptyList(),
            facts = listOf(fact),
        )
        val read = JSONObject(json.toString())

        assertEquals(listOf(fact), Backup.factsFrom(read))
    }

    @Test
    fun `a version two backup still restores, it simply has no facts`() {
        // Exactly what the previous release wrote: documents, but no facts key.
        val old = JSONObject(
            """
            {
              "version": 2,
              "people": [{ "id": "ada", "displayName": "Ada" }],
              "relationships": [],
              "documents": [],
              "documentTags": []
            }
            """.trimIndent(),
        )

        assertEquals(1, Backup.peopleFrom(old).size)
        assertTrue(Backup.factsFrom(old).isEmpty())
    }

    @Test
    fun `events and their attendees survive the round trip`() {
        val party = EventEntity(
            id = "event-1",
            title = "Marc's fortieth",
            date = FuzzyDate(2024, 6, 12),
            location = "Lyon",
            description = "Back garden, evening.",
            addedAt = 1_700_000_004_000,
        )
        val attendee = EventAttendeeEntity(id = "attendee-1", eventId = "event-1", personId = "claire")

        val json = Backup.toJson(
            people = listOf(claire, marc),
            relationships = listOf(married),
            events = listOf(party),
            attendees = listOf(attendee),
        )
        val read = JSONObject(json.toString())

        assertEquals(listOf(party), Backup.eventsFrom(read))
        assertEquals(listOf(attendee), Backup.attendeesFrom(read))
    }

    @Test
    fun `a version three backup still restores, it simply has no events`() {
        // Exactly what the previous release wrote: facts, but no events key.
        val old = JSONObject(
            """
            {
              "version": 3,
              "people": [{ "id": "ada", "displayName": "Ada" }],
              "relationships": [],
              "documents": [],
              "documentTags": [],
              "facts": []
            }
            """.trimIndent(),
        )

        assertEquals(1, Backup.peopleFrom(old).size)
        assertTrue(Backup.eventsFrom(old).isEmpty())
        assertTrue(Backup.attendeesFrom(old).isEmpty())
        assertTrue(Backup.eventPhotosFrom(old).isEmpty())
    }

    @Test
    fun `the format version says four, now that events are in it`() {
        assertEquals(4, Backup.VERSION)
    }

    @Test
    fun `the suggested file name carries the date`() {
        assertTrue(Backup.fileName(0L).startsWith("social-graph-19"))
        assertTrue(Backup.fileName(0L).endsWith(".zip"))
    }
}
