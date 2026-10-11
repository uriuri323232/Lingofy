package com.example.vocabmaster.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vocabmaster.*
import com.example.vocabmaster.data.Word
import com.example.vocabmaster.data.WordRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

// ===================== בית =====================

@Composable
fun HomeScreen(vm: VocabVM, speaker: Speaker) {
    @Suppress("UNUSED_VARIABLE") val t = vm.tick
    val words = vm.repo.words
    val learned = vm.store.learnedCount(words)
    val today = vm.store.todayCount()
    val weak = vm.store.weakWords(words)
    val week = vm.store.lastDays(7)
    val wod = vm.wordOfDay()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { HomeHeader(vm, today, learned, words.size, week) }
        item { StartCard(vm) }
        if (weak.isNotEmpty()) item {
            Card(
                onClick = { vm.startWeak() }, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Amber), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FitnessCenter, null, tint = Color(0xFF1F2937))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("חיזוק מילים קשות", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("${weak.size} מילים מחכות לחזרה", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = cardColors(), elevation = cardElevation()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("✨ המילה היומית", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(wod.en, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                        Text(wod.he, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalIconButton(onClick = { speaker.speak(wod.en, vm.rate) }, Modifier.size(52.dp)) { Icon(Icons.Default.VolumeUp, "השמע") }
                    IconButton(onClick = { vm.toggleFav(wod) }) {
                        val f = vm.store.isFavorite(wod.en)
                        Icon(if (f) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "מועדף", tint = if (f) Danger else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Text("קטגוריות", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf(WordRepository.ALL) + vm.repo.categories) { c ->
                    val list = vm.repo.inCategory(c)
                    CategoryTile(c, list.size, vm.store.learnedCount(list), vm.category == c) { vm.category = c }
                }
            }
        }
    }
}

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when { h < 5 -> "לילה טוב 🌙"; h < 12 -> "בוקר טוב ☀️"; h < 17 -> "צהריים טובים 🌤️"; h < 21 -> "ערב טוב 🌆"; else -> "לילה טוב 🌙" }
}

@Composable
private fun HomeHeader(vm: VocabVM, today: Int, learned: Int, total: Int, week: List<Pair<LocalDate, Int>>) {
    val goal = vm.goal
    val ring by animateFloatAsState((today.toFloat() / goal).coerceIn(0f, 1f), tween(700), label = "ring")
    val letters = listOf("ב", "ג", "ד", "ה", "ו", "ש", "א")
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(Brush.linearGradient(listOf(Indigo, Violet)))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.width * 0.45f, center = Offset(size.width * 0.05f, size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.06f), radius = size.width * 0.32f, center = Offset(size.width * 0.98f, size.height * 0.95f))
        }
        Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyLarge)
                    Text("Lingofy", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                }
                FilledTonalIconButton(
                    onClick = { vm.screen = Screen.SETTINGS },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White)
                ) { Icon(Icons.Default.Settings, "הגדרות") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.16f)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { ring }, modifier = Modifier.size(64.dp), strokeWidth = 8.dp, color = Color.White, trackColor = Color.White.copy(alpha = 0.25f))
                            Text("$today", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("יעד יומי", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$today מתוך $goal", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.16f)) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥", fontSize = 26.sp)
                        Text("${vm.store.streak()}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text("ימים", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { (d, c) ->
                    val isToday = d == LocalDate.now()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(letters[d.dayOfWeek.value - 1], color = Color.White.copy(alpha = if (isToday) 1f else 0.7f), style = MaterialTheme.typography.labelMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        Box(
                            Modifier.size(32.dp).clip(CircleShape)
                                .background(if (c > 0) Color.White else Color.White.copy(alpha = 0.18f))
                                .then(if (isToday) Modifier.border(2.dp, Amber, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) { if (c > 0) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = Indigo) }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { learned.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Amber, trackColor = Color.White.copy(alpha = 0.25f)
                )
                Text("$learned מתוך $total מילים נלמדו", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StartCard(vm: VocabVM) {
    val list = vm.scope()
    val learned = vm.store.learnedCount(list)
    Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(catGradient(vm.category)))
    ) {
        Text(WordRepository.emoji(vm.category), fontSize = 110.sp, modifier = Modifier.align(Alignment.CenterEnd).alpha(0.22f))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text("ממשיכים ללמוד", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                Text("${WordRepository.emoji(vm.category)} ${vm.scopeTitle()}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("${list.size} מילים · $learned נלמדו", color = Color.White.copy(alpha = 0.9f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.startLearn(list, vm.scopeTitle()) }, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF312E81))
                ) { Icon(Icons.Default.Style, null); Spacer(Modifier.width(8.dp)); Text("לימוד", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { vm.openQuiz() }, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    border = BorderStroke(1.5.dp, Color.White), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Icon(Icons.Default.Quiz, null); Spacer(Modifier.width(8.dp)); Text("חידון", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun CategoryTile(name: String, count: Int, learned: Int, selected: Boolean, onClick: () -> Unit) {
    val sc by animateFloatAsState(if (selected) 1f else 0.95f, tween(220), label = "tile")
    Box(
        Modifier.width(156.dp).height(132.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(catGradient(name)))
            .then(if (selected) Modifier.border(3.dp, Color.White, RoundedCornerShape(24.dp)) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(WordRepository.emoji(name), fontSize = 32.sp)
                if (selected) Icon(Icons.Default.CheckCircle, null, tint = Color.White)
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("$learned/$count נלמדו", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { if (count == 0) 0f else learned.toFloat() / count },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = Color.White, trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ===================== לימוד =====================

@Composable
fun LearnScreen(vm: VocabVM, speaker: Speaker) {
    @Suppress("UNUSED_VARIABLE") val t = vm.tick
    ScreenScaffold(vm.learnTitle, onBack = { vm.screen = Screen.HOME }, actions = {
        IconButton(onClick = { vm.toggleDirection() }) { Icon(Icons.Default.SwapHoriz, "החלף כיוון") }
    }) {
        val list = vm.learnList
        when {
            list.isEmpty() -> EmptyState("אין מילים ללמוד", "נסה לבחור קטגוריה אחרת", Icons.Default.Style)
            vm.learnIndex >= list.size -> LearnDone(vm)
            else -> LearnCard(vm, speaker, list[vm.learnIndex])
        }
    }
}

@Composable
private fun LearnCard(vm: VocabVM, speaker: Speaker, word: Word) {
    val rot by animateFloatAsState(if (vm.flipped) 180f else 0f, tween(450), label = "flip")
    val dens = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current
    var dx by remember(word.en, vm.learnIndex) { mutableFloatStateOf(0f) }
    LaunchedEffect(word.en, vm.heFirst) { if (vm.autoSpeak && !vm.heFirst) speaker.speak(word.en, vm.rate) }
    LaunchedEffect(vm.flipped) { if (vm.flipped && vm.heFirst && vm.autoSpeak) speaker.speak(word.en, vm.rate) }
    val frontText = if (vm.heFirst) word.he else word.en
    val backText = if (vm.heFirst) word.en else word.he
    val fav = vm.store.isFavorite(word.en)
    val emoji = WordRepository.emoji(word.category)
    val answer: (Boolean) -> Unit = { known -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.answerLearn(known) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${vm.learnIndex + 1} / ${vm.learnList.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text("$emoji ${word.category}", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }
        LinearProgressIndicator(
            progress = { vm.learnIndex.toFloat() / vm.learnList.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
        )
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .graphicsLayer { translationX = dx; rotationZ = dx / 60f }
                .pointerInput(word.en, vm.learnIndex) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val d = dx
                            dx = 0f
                            if (d < -220f) answer(true) else if (d > 220f) answer(false)
                        },
                        onDragCancel = { dx = 0f }
                    ) { change, amount -> change.consume(); dx += amount }
                }
        ) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { rotationY = rot; cameraDistance = 14f * dens }
                    .clickable { vm.flipped = !vm.flipped }
            ) {
                if (rot <= 90f) {
                    FlipFace(frontText, if (vm.heFirst) "עברית" else "English", "הקש כדי לראות תרגום", null, MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.onSurface, emoji)
                } else {
                    Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                        FlipFace(backText, if (vm.heFirst) "English" else "עברית", null, Brush.linearGradient(catGradient(word.category)), Color.Transparent, Color.White, emoji)
                    }
                }
            }
            val a = (abs(dx) / 400f).coerceIn(0f, 0.6f)
            if (a > 0.03f) {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background((if (dx < 0) Success else Danger).copy(alpha = a)),
                    contentAlignment = Alignment.Center
                ) { Icon(if (dx < 0) Icons.Default.Check else Icons.Default.Refresh, null, Modifier.size(80.dp), tint = Color.White) }
            }
        }
        Text("החלק שמאלה = ידעתי · החלק ימינה = עוד פעם", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { vm.toggleFav(word) }) {
                Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (fav) Danger else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(6.dp)); Text(if (fav) "במועדפים" else "מועדף")
            }
            OutlinedButton(onClick = { speaker.speak(word.en, vm.rate) }) {
                Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(6.dp)); Text("השמע")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { answer(false) }, Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White)
            ) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("עוד פעם", fontWeight = FontWeight.Bold) }
            Button(
                onClick = { answer(true) }, Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = Color.White)
            ) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(6.dp)); Text("ידעתי", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun FlipFace(text: String, label: String, hint: String?, brush: Brush?, container: Color, content: Color, emoji: String) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        Modifier.fillMaxSize().shadow(8.dp, shape).clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(container))
    ) {
        Text(emoji, fontSize = 170.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-24).dp, y = 36.dp).alpha(0.10f))
        Column(
            Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = content.copy(alpha = 0.12f)) {
                Text(label, Modifier.padding(horizontal = 14.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge, color = content.copy(alpha = 0.75f))
            }
            Spacer(Modifier.height(20.dp))
            Text(text, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 46.sp, color = content)
            if (hint != null) {
                Spacer(Modifier.height(16.dp))
                Text(hint, color = content.copy(alpha = 0.55f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LearnDone(vm: VocabVM) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val sc by animateFloatAsState(if (go) 1f else 0.2f, tween(500), label = "pop")
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🎉", fontSize = 72.sp, modifier = Modifier.graphicsLayer { scaleX = sc; scaleY = sc })
        Spacer(Modifier.height(10.dp))
        Text("סיימת את הסבב!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("ידעתי", "${vm.learnKnown}", Modifier.width(120.dp), Success)
            StatCard("לחיזוק", "${vm.learnAgain}", Modifier.width(120.dp), Danger)
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = { vm.startLearn(vm.scope(), vm.scopeTitle()) }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("סבב נוסף", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { vm.screen = Screen.HOME }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("חזרה לבית") }
    }
}

// ===================== חידון =====================

@Composable
fun QuizScreen(vm: VocabVM, speaker: Speaker) {
    @Suppress("UNUSED_VARIABLE") val t = vm.tick
    ScreenScaffold("חידון", onBack = { vm.leaveQuiz() }) {
        val mode = vm.quizMode
        when {
            mode == null -> ModePicker(vm)
            vm.qIndex >= vm.questions.size -> QuizResult(vm, mode)
            else -> QuizQuestion(vm, speaker, mode, vm.questions[vm.qIndex])
        }
    }
}

private val modeColors = mapOf(
    QuizMode.EN_HE to listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
    QuizMode.HE_EN to listOf(Color(0xFF0EA5E9), Color(0xFF2563EB)),
    QuizMode.LISTEN to listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
    QuizMode.TYPE to listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
)

@Composable
private fun ModePicker(vm: VocabVM) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("בחר סוג חידון", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${WordRepository.emoji(vm.category)} ${vm.scopeTitle()} · 10 שאלות בכל סבב", color = MaterialTheme.colorScheme.onSurfaceVariant)
        QuizMode.values().forEach { m ->
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(modeColors[m] ?: listOf(Indigo, Violet)))
                    .clickable { vm.startQuiz(m) }
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) { Text(m.emoji, fontSize = 28.sp) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(m.desc, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OptionButton(text: String, state: Int, onClick: () -> Unit) {
    // state: 0 רגיל, 1 נכון, 2 שגוי, 3 עמום
    val onSurface = MaterialTheme.colorScheme.onSurface
    val bg by animateColorAsState(
        when (state) { 1 -> Success; 2 -> Danger; else -> MaterialTheme.colorScheme.surfaceContainerLow }, tween(250), label = "optBg"
    )
    val fg by animateColorAsState(
        when (state) { 1, 2 -> Color.White; 3 -> onSurface.copy(alpha = 0.45f); else -> onSurface }, tween(250), label = "optFg"
    )
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        border = BorderStroke(1.dp, if (state == 0 || state == 3) MaterialTheme.colorScheme.outlineVariant else Color.Transparent),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            if (state == 1) Icon(Icons.Default.Check, null)
            if (state == 2) Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
private fun QuizQuestion(vm: VocabVM, speaker: Speaker, mode: QuizMode, q: Question) {
    val w = q.word
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(w.en, mode) { if (mode == QuizMode.LISTEN) speaker.speak(w.en, vm.rate) }
    val last = vm.qIndex == vm.questions.size - 1

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("שאלה ${vm.qIndex + 1} / ${vm.questions.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (vm.combo >= 2) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("🔥 ×${vm.combo}", Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Text("✅ ${vm.qScore}", fontWeight = FontWeight.Bold)
            }
        }
        LinearProgressIndicator(progress = { (vm.qIndex + if (vm.answered) 1 else 0).toFloat() / vm.questions.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))

        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(catGradient(w.category)))) {
            Text(WordRepository.emoji(w.category), fontSize = 120.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-16).dp, y = 30.dp).alpha(0.12f))
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (mode) {
                    QuizMode.EN_HE -> {
                        Text("מה התרגום?", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                        Text(w.en, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = Color.White)
                        IconButton(onClick = { speaker.speak(w.en, vm.rate) }) { Icon(Icons.Default.VolumeUp, "השמע", tint = Color.White) }
                    }
                    QuizMode.HE_EN -> {
                        Text("איך אומרים באנגלית?", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                        Text(w.he, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = Color.White)
                    }
                    QuizMode.LISTEN -> {
                        Text("הקשב ובחר את התרגום", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                        FilledIconButton(
                            onClick = { speaker.speak(w.en, vm.rate) }, Modifier.size(76.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color(0xFF9D174D))
                        ) { Icon(Icons.Default.VolumeUp, "השמע", Modifier.size(38.dp)) }
                        if (vm.answered) Text(w.en, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    QuizMode.TYPE -> {
                        Text("כתוב באנגלית", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                        Text(w.he, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = Color.White)
                    }
                }
            }
        }

        if (mode == QuizMode.TYPE) {
            OutlinedTextField(
                value = vm.typed, onValueChange = { if (!vm.answered) vm.typed = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp),
                label = { Text("התרגום באנגלית") },
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, fontSize = 22.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.checkTyped() })
            )
            if (!vm.answered) Button(onClick = { vm.checkTyped() }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("בדיקה", fontWeight = FontWeight.Bold) }
        } else {
            q.options.forEach { o ->
                val isAnswer = o.en == w.en
                val isPicked = vm.pickedEn == o.en
                val st = when { !vm.answered -> 0; isAnswer -> 1; isPicked -> 2; else -> 3 }
                OptionButton(if (mode == QuizMode.HE_EN) o.en else o.he, st) {
                    if (!vm.answered) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.answerChoice(o) }
                }
            }
        }

        if (vm.answered) {
            Surface(shape = RoundedCornerShape(16.dp), color = (if (vm.lastCorrect) Success else Danger).copy(alpha = 0.12f)) {
                Text(
                    if (vm.lastCorrect) "מעולה! תשובה נכונה 🎉" else "התשובה הנכונה: ${w.en} — ${w.he}",
                    Modifier.fillMaxWidth().padding(14.dp), fontWeight = FontWeight.Bold, color = if (vm.lastCorrect) Success else Danger
                )
            }
            Button(onClick = { vm.nextQuestion() }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
                Text(if (last) "לתוצאות" else "השאלה הבאה", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuizResult(vm: VocabVM, mode: QuizMode) {
    val total = vm.questions.size
    val pct = if (total == 0) 0 else vm.qScore * 100 / total
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val p by animateFloatAsState(if (go) pct / 100f else 0f, tween(900), label = "result")
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { p }, modifier = Modifier.size(170.dp), strokeWidth = 14.dp,
                color = if (pct >= 80) Success else if (pct >= 50) MaterialTheme.colorScheme.primary else Amber,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (pct >= 80) "🏆" else if (pct >= 50) "👏" else "💪", fontSize = 34.sp)
                Text("$pct%", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("${vm.qScore} מתוך $total", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            when { pct >= 80 -> "מצוין! אתה שולט בחומר"; pct >= 50 -> "יפה מאוד, עוד קצת תרגול"; else -> "כל טעות היא הזדמנות ללמוד" },
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        if (vm.bestCombo >= 2) Text("🔥 קומבו שיא: ${vm.bestCombo}", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Button(onClick = { vm.startQuiz(mode) }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("סבב נוסף", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { vm.quizMode = null }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("שינוי סוג חידון") }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = { vm.leaveQuiz() }) { Text("חזרה לבית") }
    }
}
