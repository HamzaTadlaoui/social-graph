package io.github.hamzatadlaoui.socialgraph.data

import androidx.room.TypeConverter
import io.github.hamzatadlaoui.socialgraph.model.Certainty
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType

/**
 * Everything the database stores is a string, so that a backup written by one
 * version can still be read by the next (section 16: the format is documented
 * and meant to outlive any one build).
 */
class Converters {

    @TypeConverter
    fun fuzzyDateToString(date: FuzzyDate?): String = date?.store().orEmpty()

    @TypeConverter
    fun stringToFuzzyDate(stored: String?): FuzzyDate = FuzzyDate.parse(stored)

    @TypeConverter
    fun relationshipTypeToString(type: RelationshipType?): String =
        (type ?: RelationshipType.KNOWS).name

    @TypeConverter
    fun stringToRelationshipType(name: String?): RelationshipType =
        RelationshipType.fromName(name) ?: RelationshipType.KNOWS

    @TypeConverter
    fun certaintyToString(certainty: Certainty?): String = (certainty ?: Certainty.SURE).name

    @TypeConverter
    fun stringToCertainty(name: String?): Certainty = Certainty.fromName(name) ?: Certainty.SURE
}
