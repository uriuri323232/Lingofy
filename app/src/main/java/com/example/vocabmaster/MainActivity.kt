package com.example.vocabmaster

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vocabmaster.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LingofyApp() }
    }
}

@Composable
fun LingofyApp(vm: VocabVM = viewModel()) {
    val context = LocalContext.current
    vm.init(context)
    val speaker = remember { Speaker(context) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    val dark = when (vm.themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val view = LocalView.current
    val onHome = vm.screen == Screen.HOME
    SideEffect {
        val window = (view.context as Activity).window
        val c = WindowCompat.getInsetsController(window, view)
        c.isAppearanceLightStatusBars = !dark && !onHome   // בבית הכותרת כהה תמיד
        c.isAppearanceLightNavigationBars = !dark
    }

    BackHandler(enabled = vm.screen != Screen.HOME) {
        if (vm.screen == Screen.QUIZ) vm.leaveQuiz() else vm.screen = Screen.HOME
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LingofyTheme(dark) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = vm.screen,
                            transitionSpec = { (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 24 }) togetherWith fadeOut(tween(140)) },
                            label = "screen"
                        ) { s ->
                            when (s) {
                                Screen.HOME -> HomeScreen(vm, speaker)
                                Screen.LEARN -> LearnScreen(vm, speaker)
                                Screen.QUIZ -> QuizScreen(vm, speaker)
                                Screen.SEARCH -> SearchScreen(vm, speaker)
                                Screen.FAVORITES -> FavoritesScreen(vm, speaker)
                                Screen.STATS -> StatsScreen(vm)
                                Screen.SETTINGS -> SettingsScreen(vm)
                            }
                        }
                    }
                    if (vm.screen in listOf(Screen.HOME, Screen.SEARCH, Screen.FAVORITES, Screen.STATS)) BottomBar(vm)
                }
            }
        }
    }
}
