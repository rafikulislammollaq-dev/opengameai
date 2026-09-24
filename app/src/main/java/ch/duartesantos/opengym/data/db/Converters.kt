package ch.duartesantos.opengym.data.db

import androidx.room.TypeConverter
import ch.duartesantos.opengym.data.model.Equipment
import ch.duartesantos.opengym.data.model.MuscleGroup
import ch.duartesantos.opengym.data.model.SetType

class Converters {

    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup?): String? = value?.name

    @TypeConverter
    fun toMuscleGroup(value: String?): MuscleGroup? =
        value?.let { runCatching { MuscleGroup.valueOf(it) }.getOrDefault(MuscleGroup.FULL_BODY) }

    @TypeConverter
    fun fromEquipment(value: Equipment?): String? = value?.name

    @TypeConverter
    fun toEquipment(value: String?): Equipment? =
        value?.let { runCatching { Equipment.valueOf(it) }.getOrDefault(Equipment.BARBELL) }

    @TypeConverter
    fun fromSetType(value: SetType?): String? = value?.name

    @TypeConverter
    fun toSetType(value: String?): SetType? =
        value?.let { runCatching { SetType.valueOf(it) }.getOrDefault(SetType.NORMAL) }

    @TypeConverter
    fun fromMuscleGroupList(list: List<MuscleGroup>?): String? =
        list?.joinToString(",") { it.name }

    @TypeConverter
    fun toMuscleGroupList(value: String?): List<MuscleGroup> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { name ->
            runCatching { MuscleGroup.valueOf(name.trim()) }.getOrNull()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? =
        list?.joinToString(",")

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").map { it.trim() }
    }
}
