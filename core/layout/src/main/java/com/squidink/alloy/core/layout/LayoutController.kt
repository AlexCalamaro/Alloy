package com.squidink.alloy.core.layout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that manages the three-pane layout state.
 * Handles pane visibility, feature selection, and state persistence.
 */
@HiltViewModel
class LayoutController @Inject constructor(
    private val repository: LayoutStateRepository
) : ViewModel() {
    
    private val _layoutState = repository.observeLayoutState()
    val layoutState: StateFlow<AppLayoutState> = _layoutState
        .map { it }
        .let { flow ->
            flow.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
                initialValue = AppLayoutState.DEFAULT
            )
        }
    
    /**
     * Process a layout event and update state accordingly.
     */
    fun handleEvent(event: LayoutEvent) {
        viewModelScope.launch {
            when (event) {
                is LayoutEvent.OpenNavigation -> {
                    repository.updateLayoutState { copy(navigationPaneOpen = true) }
                }
                
                is LayoutEvent.CloseNavigation -> {
                    repository.updateLayoutState { copy(navigationPaneOpen = false) }
                }
                
                is LayoutEvent.ToggleNavigation -> {
                    repository.updateLayoutState { 
                        copy(navigationPaneOpen = !navigationPaneOpen) 
                    }
                }
                
                is LayoutEvent.OpenDetail -> {
                    repository.updateLayoutState { 
                        copy(detailPaneOpen = true, detailFeature = event.feature) 
                    }
                }
                
                is LayoutEvent.CloseDetail -> {
                    repository.updateLayoutState { 
                        copy(detailPaneOpen = false, detailFeature = null) 
                    }
                }
                
                is LayoutEvent.ToggleDetail -> {
                    repository.updateLayoutState { 
                        copy(detailPaneOpen = !detailPaneOpen) 
                    }
                }
                
                is LayoutEvent.SelectFeature -> {
                    repository.updateLayoutState { 
                        copy(
                            currentFeature = event.feature,
                            navigationPaneOpen = false  // Auto-close navigation on feature select
                        ) 
                    }
                }
                
                is LayoutEvent.Reset -> {
                    repository.clearLayoutState()
                }
            }
        }
    }
    
    /**
     * Convenience method to open navigation.
     */
    fun openNavigation() {
        handleEvent(LayoutEvent.OpenNavigation)
    }
    
    /**
     * Convenience method to close navigation.
     */
    fun closeNavigation() {
        handleEvent(LayoutEvent.CloseNavigation)
    }
    
    /**
     * Convenience method to toggle navigation visibility.
     */
    fun toggleNavigation() {
        handleEvent(LayoutEvent.ToggleNavigation)
    }
    
    /**
     * Convenience method to open detail pane for a feature.
     */
    fun openDetail(feature: String) {
        handleEvent(LayoutEvent.OpenDetail(feature))
    }
    
    /**
     * Convenience method to close detail pane.
     */
    fun closeDetail() {
        handleEvent(LayoutEvent.CloseDetail)
    }
    
    /**
     * Convenience method to toggle detail pane visibility.
     */
    fun toggleDetail() {
        handleEvent(LayoutEvent.ToggleDetail)
    }
    
    /**
     * Convenience method to select a feature.
     */
    fun selectFeature(feature: String) {
        handleEvent(LayoutEvent.SelectFeature(feature))
    }
    
    /**
     * Reset layout state to defaults.
     */
    fun reset() {
        handleEvent(LayoutEvent.Reset)
    }
}
