package com.squidink.alloy.core.domain.usecase.clip

import com.squidink.alloy.core.domain.repository.Clip
import com.squidink.alloy.core.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case to transform and sort a list of clips.
 *
 * Business logic:
 * - Pinned clips appear first
 * - Within pinned/unpinned groups, sort by most recent
 *
 * This use case is pure and testable without Android dependencies.
 */
class TransformClipsUseCase @Inject constructor() : UseCase<List<Clip>, List<Clip>> {
    
    override suspend fun execute(clips: List<Clip>): List<Clip> {
        return clips.sortedWith(
            compareByDescending<Clip> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
    }
}
