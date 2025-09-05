package com.ibylin.app.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * 管理用户偏好设置的ViewModel
 * 参考官方实例: /Users/zhaojing/Downloads/kotlin-toolkit-develop/test-app/src/main/java/org/readium/r2/testapp/reader/preferences/UserPreferencesViewModel.kt
 */
@OptIn(ExperimentalReadiumApi::class)
class UserPreferencesViewModel<S : Configurable.Settings, P : Configurable.Preferences<P>>(
    private val viewModelScope: CoroutineScope,
    private val bookId: Long,
    private val preferencesManager: PreferencesManager<P>,
    private val createPreferencesEditor: (P) -> PreferencesEditor<P>,
) {
    val preferences: StateFlow<P> = preferencesManager.preferences
    
    val editor: StateFlow<PreferencesEditor<P>> = preferencesManager.preferences
        .map { createPreferencesEditor(it) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, createPreferencesEditor(preferencesManager.preferences.value))

    fun bind(configurable: Configurable<S, P>, lifecycleOwner: LifecycleOwner) {
        with(lifecycleOwner) {
            preferencesManager.preferences
                .flowWithLifecycle(lifecycle)
                .onEach { configurable.submitPreferences(it) }
                .launchIn(lifecycleScope)
        }
    }

    fun commit() {
        viewModelScope.launch {
            preferencesManager.setPreferences(editor.value.preferences)
        }
    }
    
    suspend fun setPreferences(preferences: P) {
        preferencesManager.setPreferences(preferences)
    }

    companion object {
        fun createForEpub(
            viewModelScope: CoroutineScope,
            bookId: Long,
            preferencesManager: PreferencesManager<EpubPreferences>,
            navigatorFactory: org.readium.r2.navigator.epub.EpubNavigatorFactory,
        ): UserPreferencesViewModel<org.readium.r2.navigator.epub.EpubSettings, EpubPreferences> {
            return UserPreferencesViewModel<org.readium.r2.navigator.epub.EpubSettings, EpubPreferences>(
                viewModelScope,
                bookId,
                preferencesManager,
                createPreferencesEditor = navigatorFactory::createPreferencesEditor
            )
        }
    }
}
