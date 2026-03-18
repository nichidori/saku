package dev.nichidori.saku.core.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import dev.nichidori.saku.domain.model.TrxType

val TrxTypeNavType = object : NavType<TrxType>(isNullableAllowed = false) {
    override fun get(bundle: SavedState, key: String): TrxType {
        return bundle.read { enumValueOf(getString(key)) }
    }

    override fun parseValue(value: String): TrxType {
        return enumValueOf(value)
    }

    override fun serializeAsValue(value: TrxType): String {
        return value.name
    }

    override fun put(bundle: SavedState, key: String, value: TrxType) {
        bundle.write { putString(key, value.name) }
    }
}
