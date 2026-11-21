package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CurrencyOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smartsplit_prefs")

class UserPreferences(private val context: Context) {
    private object Keys {
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val INITIAL_SEEDED = booleanPreferencesKey("initial_seeded")
    }

    val currencyFlow: Flow<CurrencyOption> = context.dataStore.data.map { prefs ->
        CurrencyOption.fromCode(prefs[Keys.CURRENCY_CODE])
    }

    suspend fun setCurrency(currency: CurrencyOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_CODE] = currency.code
        }
    }

    suspend fun isInitialSeeded(): Boolean =
        context.dataStore.data.first()[Keys.INITIAL_SEEDED] == true

    suspend fun markInitialSeeded() {
        context.dataStore.edit { prefs ->
            prefs[Keys.INITIAL_SEEDED] = true
        }
    }
}
