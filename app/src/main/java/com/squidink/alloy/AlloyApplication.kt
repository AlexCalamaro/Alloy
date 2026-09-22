package com.squidink.alloy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class annotated for Hilt dependency injection.
 */
@HiltAndroidApp
class AlloyApplication : Application()
