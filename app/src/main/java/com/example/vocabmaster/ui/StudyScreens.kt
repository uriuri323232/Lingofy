package com.example.vocabmaster.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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

// ===================== בית =====================

@Composable
fun HomeScreen(vm: VocabVM) {
    @Suppress("UNUSED_VARIABLE") val t = vm.tick
    val words = vm.repo.words
    val learned = vm.store.learnedCount(words)
    val today = vm.store.todayCount()
    val weak = vm.store.weakWords(words)
    val wod = vm.wordOfDay()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { HomeHeader(vm, today, learned, words.size) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "לומדים עכשיו: ${WordRepository.emoji(vm.category)} ${vm.scopeTitle()} · ${vm.scope().size} מילים",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigAction("לימוד", "כרטיסיות", Icons.Default.Style, listOf(Indigo, Violet), Modifier.weight(1f)) {
                        vm.startLearn(vm.scope(), vm.scopeTitle())
                    }
                    BigAction("חידון", "4 סוגים", Icons.Default.Quiz, listOf(Color(0xFFF59E0B), Color(0xFFEF4444)), Modifier.weight(1f)) { vm.openQuiz() }
                }
                if (weak.isNotEmpty()) {
                    Card(
                        onClick = { vm.startWeak() }, shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("חיזוק מילים קשות", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("${weak.size} מילים מחכות לחזרה", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = cardColors(), elevation = cardElevation()) {
                Column(Modifier.padding(18.dp)) {
                    Text("המילה היומית", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(wod.en, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(wod.he, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("קטגוריות", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items((listOf(WordRepository.ALL) + vm.repo.categories).chunked(2)) { row ->
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { c ->
                    val list = vm.repo.inCategory(c)
                    CategoryCard(c, list.size, vm.store.learnedCount(list), vm.category == c, Modifier.weight(1f)) { vm.category = c }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeHeader(vm: VocabVM, today: Int, learned: Int, total: Int) {
    val goal = vm.goal
    val ring by animateFloatAsState((today.toFloat() / goal).coerceIn(0f, 1f), label = "ring")
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(Indigo, Violet)))
    ) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lingofy", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("לומדים חכם. זוכרים יותר.", color = Color.White.copy(alpha = 0.85f))
                }
                IconButton(onClick = { vm.screen = Screen.SETTINGS }) { Icon(Icons.Default.Settings, "הגדרות", tint = Color.White) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.16f)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { ring }, modifier = Modifier.size(58.dp), strokeWidth = 7.dp, color = Color.White, trackColor = Color.White.copy(alpha = 0.25f))
                            Text("$today", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("יעד יומי", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$today מתוך $goal", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.16f)) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥", fontSize = 24.sp)
                        Text("${vm.store.streak()} ימים", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun BigAction(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(colors)).clickable(onClick = onClick)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(sub, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CategoryCard(name: String, count: Int, learned: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = cardElevation()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(WordRepository.emoji(name), fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold)
                    Text("$count מילים", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LinearProgressIndicator(
                progress = { if (count == 0) 0f else learned.toFloat() / count },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Success,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
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
    LaunchedEffect(word.en, vm.heFirst) { if (vm.autoSpeak && !vm.heFirst) speaker.speak(word.en, vm.rate) }
    LaunchedEffect(vm.flipped) { if (vm.flipped && vm.heFirst && vm.autoSpeak) speaker.speak(word.en, vm.rate) }
    val frontText = if (vm.heFirst) word.he else word.en
    val backText = if (vm.heFirst) word.en else word.he
    val fav = vm.store.isFavorite(word.en)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${vm.learnIndex + 1} / ${vm.learnList.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${WordRepository.emoji(word.category)} ${word.category}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { vm.learnIndex.toFloat() / vm.learnList.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
        )
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .graphicsLayer { rotationY = rot; cameraDistance = 14f * dens }
                .clickable { vm.flipped = !vm.flipped }
        ) {
            if (rot <= 90f) {
                FlipFace(frontText, if (vm.heFirst) "עברית" else "English", "הקש כדי לראות תרגום", MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.onSurface)
            } else {
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                    FlipFace(backText, if (vm.heFirst) "English" else "עברית", null, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
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
                onClick = { vm.answerLearn(false) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White)
            ) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("עוד פעם") }
            Button(
                onClick = { vm.answerLearn(true) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = Color.White)
            ) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(6.dp)); Text("ידעתי") }
        }
    }
}

@Composable
private fun FlipFace(text: String, label: String, hint: String?, container: Color, content: Color) {
    Card(Modifier.fillMaxSize(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = container, contentColor = content), elevation = CardDefaults.cardElevation(4.dp)) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = content.copy(alpha = 0.6f))
                Text(text, fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 44.sp)
                if (hint != null) Text(hint, color = content.copy(alpha = 0.55f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LearnDone(vm: VocabVM) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🎉", fontSize = 64.sp)
        Spacer(Modifier.height(10.dp))
        Text("סיימת את הסבב!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("ידעת ${vm.learnKnown} · לחיזוק ${vm.learnAgain}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Button(onClick = { vm.startLearn(vm.scope(), vm.scopeTitle()) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("סבב נוסף") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { vm.screen = Screen.HOME }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("חזרה לבית") }
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

@Composable
private fun ModePicker(vm: VocabVM) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("בחר סוג חידון", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("${WordRepository.emoji(vm.category)} ${vm.scopeTitle()} · 10 שאלות בכל סבב", color = MaterialTheme.colorScheme.onSurfaceVariant)
        QuizMode.values().forEach { m ->
            Card(onClick = { vm.startQuiz(m) }, shape = RoundedCornerShape(20.dp), colors = cardColors(), elevation = cardElevation()) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.emoji, fontSize = 30.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(m.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(m.desc, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizQuestion(vm: VocabVM, speaker: Speaker, mode: QuizMode, q: Question) {
    val w = q.word
    LaunchedEffect(w.en, mode) { if (mode == QuizMode.LISTEN) speaker.speak(w.en, vm.rate) }
    val last = vm.qIndex == vm.questions.size - 1

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("שאלה ${vm.qIndex + 1} / ${vm.questions.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("✅ ${vm.qScore}", fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { (vm.qIndex + if (vm.answered) 1 else 0).toFloat() / vm.questions.size }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (mode) {
                    QuizMode.EN_HE -> {
                        Text("מה התרגום?", style = MaterialTheme.typography.labelLarge)
                        Text(w.en, fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        IconButton(onClick = { speaker.speak(w.en, vm.rate) }) { Icon(Icons.Default.VolumeUp, "השמע") }
                    }
                    QuizMode.HE_EN -> {
                        Text("איך אומרים באנגלית?", style = MaterialTheme.typography.labelLarge)
                        Text(w.he, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    QuizMode.LISTEN -> {
                        Text("הקשב ובחר את התרגום", style = MaterialTheme.typography.labelLarge)
                        FilledIconButton(onClick = { speaker.speak(w.en, vm.rate) }, Modifier.size(72.dp)) { Icon(Icons.Default.VolumeUp, "השמע", Modifier.size(36.dp)) }
                        if (vm.answered) Text(w.en, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    QuizMode.TYPE -> {
                        Text("כתוב באנגלית", style = MaterialTheme.typography.labelLarge)
                        Text(w.he, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (mode == QuizMode.TYPE) {
            OutlinedTextField(
                value = vm.typed, onValueChange = { if (!vm.answered) vm.typed = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
                label = { Text("התרגום באנגלית") },
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.checkTyped() })
            )
            if (!vm.answered) Button(onClick = { vm.checkTyped() }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("בדיקה") }
        } else {
            q.options.forEach { o ->
                val isAnswer = o.en == w.en
                val isPicked = vm.pickedEn == o.en
                val container = when { vm.answered && isAnswer -> Success; vm.answered && isPicked -> Danger; else -> MaterialTheme.colorScheme.surfaceContainerLow }
                val content = if (vm.answered && (isAnswer || isPicked)) Color.White else MaterialTheme.colorScheme.onSurface
                Button(
                    onClick = { vm.answerChoice(o) }, Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) { Text(if (mode == QuizMode.HE_EN) o.en else o.he, style = MaterialTheme.typography.titleMedium) }
            }
        }

        if (vm.answered) {
            Text(
                if (vm.lastCorrect) "מעולה! תשובה נכונה 🎉" else "התשובה הנכונה: ${w.en} — ${w.he}",
                fontWeight = FontWeight.Bold, color = if (vm.lastCorrect) Success else Danger
            )
            Button(onClick = { vm.nextQuestion() }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text(if (last) "לתוצאות" else "השאלה הבאה")
            }
        }
    }
}

@Composable
private fun QuizResult(vm: VocabVM, mode: QuizMode) {
    val total = vm.questions.size
    val pct = if (total == 0) 0 else vm.qScore * 100 / total
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(if (pct >= 80) "🏆" else if (pct >= 50) "👏" else "💪", fontSize = 64.sp)
        Spacer(Modifier.height(10.dp))
        Text("${vm.qScore} מתוך $total", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            when { pct >= 80 -> "מצוין! אתה שולט בחומר"; pct >= 50 -> "יפה מאוד, עוד קצת תרגול"; else -> "כל טעות היא הזדמנות ללמוד" },
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = { vm.startQuiz(mode) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("סבב נוסף") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { vm.quizMode = null }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("שינוי סוג חידון") }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = { vm.leaveQuiz() }) { Text("חזרה לבית") }
    }
}
