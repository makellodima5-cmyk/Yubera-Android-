package com.yubera.app.features.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.core.player.KodikLinkPolicy
import com.yubera.app.core.player.KodikRepository
import com.yubera.app.core.player.KodikSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PlayerUiState(
    val animeTitle: String = "Просмотр",
    val sources: List<KodikSource> = emptyList(),
    val selectedSourceId: String? = null,
    val selectedEpisodeNumber: Int? = null,
    val selectedEpisodeUrl: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val kodikRepository: KodikRepository,
    private val libraryRepository: PersonalLibraryRepository
) : ViewModel() {
    private val animeId = savedStateHandle.get<String>("animeId").orEmpty()
    private val mutableState = MutableStateFlow(PlayerUiState())
    private var requestInProgress = false
    val uiState = mutableState.asStateFlow()

    init { load() }

    fun selectSource(sourceId: String) {
        val source = mutableState.value.sources.firstOrNull { it.id == sourceId } ?: return
        val episode = source.episodes.firstOrNull()
        mutableState.value = mutableState.value.copy(
            selectedSourceId = source.id,
            selectedEpisodeNumber = episode?.number,
            selectedEpisodeUrl = episode?.link
        )
        episode?.let { selected ->
            viewModelScope.launch { recordOpenedEpisode(selected.number, selected.link) }
        }
    }

    fun selectEpisode(number: Int) {
        val source = mutableState.value.sources.firstOrNull { it.id == mutableState.value.selectedSourceId } ?: return
        val episode = source.episodes.firstOrNull { it.number == number } ?: return
        mutableState.value = mutableState.value.copy(
            selectedEpisodeNumber = episode.number,
            selectedEpisodeUrl = episode.link
        )
        viewModelScope.launch { recordOpenedEpisode(episode.number, episode.link) }
    }

    fun retry() = load()

    private fun load() {
        if (requestInProgress) return
        requestInProgress = true
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            try {
                val anime = catalogRepository.observeAnime(animeId).first()
                val sources = kodikRepository.getSources(animeId)
                val firstSource = sources.firstOrNull()
                val firstEpisode = firstSource?.episodes?.firstOrNull()
                mutableState.value = PlayerUiState(
                    animeTitle = anime?.title ?: "Просмотр",
                    sources = sources,
                    selectedSourceId = firstSource?.id,
                    selectedEpisodeNumber = firstEpisode?.number,
                    selectedEpisodeUrl = firstEpisode?.link,
                    isLoading = false,
                    errorMessage = if (sources.isEmpty()) "Для этого тайтла сейчас нет доступных источников Kodik." else null
                )
                firstEpisode?.let { selected ->
                    viewModelScope.launch { recordOpenedEpisode(selected.number, selected.link) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    errorMessage = "Не удалось загрузить плеер. Проверьте подключение и повторите попытку."
                )
            } finally {
                requestInProgress = false
            }
        }
    }

    private suspend fun recordOpenedEpisode(number: Int, link: String) {
        if (!KodikLinkPolicy.isTrustedHttpsUrl(link)) return
        val existing = libraryRepository.progress.first().firstOrNull { it.animeId == animeId }
        if (existing?.episodeNumber == number) return
        libraryRepository.saveProgress(
            AnimeProgressEntity(
                animeId = animeId,
                seasonNumber = null,
                episodeNumber = number,
                positionMillis = 0L,
                durationMillis = 0L,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }
}
