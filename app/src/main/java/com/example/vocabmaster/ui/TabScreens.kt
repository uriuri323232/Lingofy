package com.example.vocabmaster.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vocabmaster.*
import com.example.vocabmaster.data.WordRepository

// ===================== חיפוש =====================

@Composable
fun SearchScreen(vm: VocabVM, speaker: Speaker) {
    @Suppress("UNUSED_VARIABLE") val tick = vm.tick
    var cat by rememberSaveable { mutableStateOf(WordRepository.ALL) }
    val q = vm.query.trim()
    val results = remember(q, cat) {
        vm.repo.words.filter { (cat == WordRepository.ALL || it.category == cat) && (q.isEmpty() || it.en.contains(q, true) || it.he.contains(q)) }
    }
    Column(Modifier.fillMaxSize()) {
        TabHeader("חיפוש")
        OutlinedTextField(
            value = vm.query, onValueChange = { vm.query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("חפש באנגלית או בעברית") }, singleLine = true, shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (vm.query.isNotEmpty()) IconButton(onClick = { vm.query = "" }) { Icon(Icons.Default.Close, "נקה") } }
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(WordRepository.ALL) + vm.repo.categories) { c ->
                FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text("${WordRepository.emoji(c)} $c") })
            }
        }
        if (results.isEmpty()) EmptyState("לא נמצאו מילים", "נסה חיפוש אחר או קטגוריה אחרת", Icons.Default.Search)
        else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "count") { Text("${results.size} מילים", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            items(results, key = { it.en }) { WordRow(it, vm, speaker) }
        }
    }
}

// ===================== מועדפים =====================

@Composable
fun FavoritesScreen(vm: VocabVM, speaker: Speaker) {
    @Suppress("UNUSED_VARIABLE") val t = vm.tick
    val fav = vm.store.favorites(vm.repo.words)
    Column(Modifier.fillMaxSize()) {
        TabHeader("מועדפים")
        if (fav.isEmpty()) EmptyState("עדיין אין מועדפים", "סמן מילים בלב, בכרטיסיות או בחיפוש, והן יופיעו כאן")
        else {
            Button(
                onClick = { vm.startLearn(fav, "המועדפים שלי") },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Style, null); Spacer(Modifier.width(8.dp)); Text("תרגל ${fav.size} מועדפים") }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fav, key = { it.en }) { WordRow(it, vm, speaker) }
            }
        }
    }
}

// ===================== התקדמות =====================

@Composable
fun StatsScreen(vm: VocabVM) {
    @Suppress("UNUSED_VARIABLE") val t = vm.tick
    val s = vm.store; val words = vm.repo.words
    val attempts = s.attempts(); val ok = s.successes()
    val acc = if (attempts == 0) 0 else ok * 100 / attempts
    val weak = s.weakWords(words).take(8)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        TabHeader("ההתקדמות שלי")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("מילים שנלמדו", "${s.learnedCount(words)}/${words.size}", Modifier.weight(1f), Success)
                StatCard("דיוק בחידונים", "$acc%", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("רצף נוכחי 🔥", "${s.streak()}", Modifier.weight(1f), Amber)
                StatCard("רצף שיא", "${s.bestStreak()}", Modifier.weight(1f), Amber)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("תרגולים היום", "${s.todayCount()}/${vm.goal}", Modifier.weight(1f))
                StatCard("סה״כ תרגולים", "${s.reviews()}", Modifier.weight(1f))
            }
            Text("לפי קטגוריה", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = cardColors(), elevation = cardElevation()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    vm.repo.categories.forEach { c ->
                        val l = vm.repo.inCategory(c); val n = s.learnedCount(l)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${WordRepository.emoji(c)} $c"); Text("$n/${l.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LinearProgressIndicator(
                                progress = { n.toFloat() / l.size }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = Success, trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
            if (weak.isNotEmpty()) {
                Text("המילים הקשות שלי", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Card(shape = RoundedCornerShape(20.dp), colors = cardColors(), elevation = cardElevation()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        weak.forEach { w ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(w.en, fontWeight = FontWeight.Bold); Text(w.he, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Button(onClick = { vm.startWeak() }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("תרגל אותן עכשיו") }
                    }
                }
            }
        }
    }
}

// ===================== הגדרות =====================

@Composable
fun SettingsScreen(vm: VocabVM) {
    var confirm by remember { mutableStateOf(false) }
    ScreenScaffold("הגדרות", onBack = { vm.screen = Screen.HOME }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsCard("מראה") {
                ChipRow(listOf("לפי המערכת", "בהיר", "כהה"), vm.themeMode) { vm.setTheme(it) }
            }
            SettingsCard("יעד יומי (תרגולים)") {
                val goals = listOf(10, 20, 30, 50)
                ChipRow(goals.map { "$it" }, goals.indexOf(vm.goal)) { vm.setGoal(goals[it]) }
            }
            SettingsCard("קול") {
                val rates = listOf(0.7f, 0.9f, 1.1f)
                Text("מהירות הקראה", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                ChipRow(listOf("איטית", "רגילה", "מהירה"), rates.indexOfFirst { it >= vm.rate - 0.01f }.coerceAtLeast(0)) { vm.setRate(rates[it]) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("השמעה אוטומטית בכרטיסיות", Modifier.weight(1f))
                    Switch(checked = vm.autoSpeak, onCheckedChange = { vm.setAutoSpeak(it) })
                }
            }
            SettingsCard("כיוון כרטיסיות") {
                ChipRow(listOf("אנגלית ← עברית", "עברית ← אנגלית"), if (vm.heFirst) 1 else 0) { if ((it == 1) != vm.heFirst) vm.toggleDirection() }
            }
            SettingsCard("נתונים") {
                OutlinedButton(onClick = { confirm = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) {
                    Icon(Icons.Default.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("איפוס כל ההתקדמות")
                }
                Text("הכול נשמר רק במכשיר ועובד בלי אינטרנט.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text("Lingofy · גרסה 2.0", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("לאפס את ההתקדמות?") },
        text = { Text("כל ההתקדמות, המועדפים והרצף יימחקו. אי אפשר לבטל.") },
        confirmButton = { TextButton(onClick = { vm.reset(); confirm = false }) { Text("איפוס", color = Danger) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("ביטול") } }
    )
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = cardColors(), elevation = cardElevation()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, l -> FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(l) }) }
    }
}
