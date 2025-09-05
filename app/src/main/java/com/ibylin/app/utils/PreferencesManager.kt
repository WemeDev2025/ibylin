package com.ibylin.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import org.readium.r2.navigator.epub.EpubPublicationPreferencesFilter
import org.readium.r2.navigator.epub.EpubSharedPreferencesFilter
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi

// DataStore扩展
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "readium_preferences")

@OptIn(ExperimentalReadiumApi::class)
class PreferencesManager<P : Configurable.Preferences<P>> internal constructor(
    val preferences: StateFlow<P>,
    @Suppress("Unused") // Keep the scope alive until the PreferencesManager is garbage collected
    private val coroutineScope: CoroutineScope,
    private val editPreferences: suspend (P) -> Unit,
) {

    suspend fun setPreferences(preferences: P) {
        editPreferences(preferences)
    }
}

@OptIn(ExperimentalReadiumApi::class)
sealed class PreferencesManagerFactory<P : Configurable.Preferences<P>>(
    private val dataStore: DataStore<Preferences>,
    private val klass: KClass<P>,
    private val sharedPreferencesFilter: PreferencesFilter<P>,
    private val publicationPreferencesFilter: PreferencesFilter<P>,
    private val preferencesSerializer: PreferencesSerializer<P>,
    private val emptyPreferences: P,
) {
    suspend fun createPreferenceManager(bookId: Long): PreferencesManager<P> {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val preferences = getPreferences(bookId, coroutineScope)

        return PreferencesManager(
            preferences = preferences,
            coroutineScope = coroutineScope,
            editPreferences = { setPreferences(bookId, it) }
        )
    }

    private suspend fun setPreferences(bookId: Long, preferences: P) {
        dataStore.edit { data ->
            data[key(klass)] = sharedPreferencesFilter
                .filter(preferences)
                .let { preferencesSerializer.serialize(it) }
        }

        dataStore.edit { data ->
            data[key(bookId)] = publicationPreferencesFilter
                .filter(preferences)
                .let { preferencesSerializer.serialize(it) }
        }
    }

    private suspend fun getPreferences(bookId: Long, scope: CoroutineScope): StateFlow<P> {
        val sharedPrefs = dataStore.data
            .map { data -> data[key(klass)] }
            .map { json ->
                try {
                    json?.let { preferencesSerializer.deserialize(it) }
                } catch (e: Exception) {
                    null
                } ?: emptyPreferences
            }

        val pubPrefs = dataStore.data
            .map { data -> data[key(bookId)] }
            .map { json ->
                try {
                    json?.let { preferencesSerializer.deserialize(it) }
                } catch (e: Exception) {
                    null
                } ?: emptyPreferences
            }

        return combine(sharedPrefs, pubPrefs) { shared, pub -> shared + pub }
            .stateIn(scope, SharingStarted.Eagerly, emptyPreferences)
    }

    /** [DataStore] key for the given [bookId]. */
    private fun key(bookId: Long): Preferences.Key<String> =
        stringPreferencesKey("book-$bookId")

    /** [DataStore] key for the given preferences [klass]. */
    private fun <T : Any> key(klass: KClass<T>): Preferences.Key<String> =
        stringPreferencesKey("class-${klass.simpleName}")
}

@OptIn(ExperimentalReadiumApi::class)
class EpubPreferencesManagerFactory(
    dataStore: DataStore<Preferences>,
) : PreferencesManagerFactory<EpubPreferences>(
    dataStore = dataStore,
    klass = EpubPreferences::class,
    sharedPreferencesFilter = EpubSharedPreferencesFilter,
    publicationPreferencesFilter = EpubPublicationPreferencesFilter,
    preferencesSerializer = EpubPreferencesSerializer(),
    emptyPreferences = EpubPreferences()
)

// 扩展函数用于创建偏好设置管理器
suspend fun Context.createEpubPreferencesManager(bookId: Long): PreferencesManager<EpubPreferences> {
    val factory = EpubPreferencesManagerFactory(dataStore)
    return factory.createPreferenceManager(bookId)
}
