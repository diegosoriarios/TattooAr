package com.diego.tattooar.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tattoo_preview_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
    }

    val isDisclaimerAccepted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DISCLAIMER_ACCEPTED] ?: false
        }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISCLAIMER_ACCEPTED] = accepted
        }
    }
}
