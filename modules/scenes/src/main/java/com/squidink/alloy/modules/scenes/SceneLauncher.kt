package com.squidink.alloy.modules.scenes

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.scenes.model.PlacementHint
import com.squidink.alloy.modules.scenes.model.Scene
import com.squidink.alloy.modules.scenes.model.SceneStep
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Geometric bounds data container.
 */
data class DisplayBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * Executes Scenes by dispatching launch intents with setLaunchBounds(Rect) and FLAG_ACTIVITY_LAUNCH_ADJACENT.
 */
@Singleton
class SceneLauncher @Inject constructor() {

    fun launchScene(context: Context, scene: Scene): Boolean {
        Logger.i(TAG, "Launching Scene: ${scene.name} with ${scene.steps.size} steps")
        var successCount = 0

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        for (step in scene.steps) {
            if (launchStep(context, step, screenWidth, screenHeight)) {
                successCount++
            }
        }
        return successCount > 0
    }

    private fun launchStep(
        context: Context,
        step: SceneStep,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        return try {
            val intent = if (step.activityName != null) {
                Intent().setClassName(step.packageName, step.activityName)
            } else {
                context.packageManager.getLaunchIntentForPackage(step.packageName)
            } ?: return false

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)

            val displayBounds = calculateBounds(step.placement, screenWidth, screenHeight)
            val options = ActivityOptions.makeBasic().apply {
                launchBounds = Rect(displayBounds.left, displayBounds.top, displayBounds.right, displayBounds.bottom)
            }

            context.startActivity(intent, options.toBundle())
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to launch scene step for package ${step.packageName}", e)
            false
        }
    }

    fun calculateBounds(placement: PlacementHint, width: Int, height: Int): DisplayBounds {
        return when (placement) {
            PlacementHint.LEFT -> DisplayBounds(0, 0, width / 2, height)
            PlacementHint.RIGHT -> DisplayBounds(width / 2, 0, width, height)
            PlacementHint.TOP_LEFT -> DisplayBounds(0, 0, width / 2, height / 2)
            PlacementHint.TOP_RIGHT -> DisplayBounds(width / 2, 0, width, height / 2)
            PlacementHint.CENTER -> DisplayBounds(width / 4, height / 4, (width * 3) / 4, (height * 3) / 4)
            PlacementHint.FULLSCREEN -> DisplayBounds(0, 0, width, height)
        }
    }

    companion object {
        private const val TAG = "SceneLauncher"
    }
}
