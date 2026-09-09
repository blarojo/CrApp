package com.crapp.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.BowelMovement
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Thin -- just collects [com.crapp.data.repository.BowelMovementRepository.movementsWithPhoto], no derived state beyond that. */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val bowelRepo = (application as CrAppApplication).bowelMovementRepository

    val photos: StateFlow<List<BowelMovement>> = bowelRepo.movementsWithPhoto
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
