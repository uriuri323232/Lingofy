package com.example.vocabmaster.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vocabmaster.Screen
import com.example.vocabmaster.VocabVM
import com.example.vocabmaster.data.Word

@Composable
fun cardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

@Composable
fun cardElevation() = CardDefaults.cardElevation(defaultElevation = 1.dp)

@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה") }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            actions()
        }
        Box(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) { content() }
    }
}

@Composable
fun TabHeader(title: String) {
    Text(
        title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

@Composable
fun EmptyState(title: String, sub: String, icon: ImageVector = Icons.Default.FavoriteBorder) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun LevelDot(level: Int) {
    val c = when (level) { 1 -> Danger; 2 -> Amber; 3 -> Success; else -> MaterialTheme.colorScheme.outlineVariant }
    Box(Modifier.size(10.dp).clip(CircleShape).background(c))
}

@Composable
fun WordRow(w: Word, vm: VocabVM, speaker: Speaker) {
    val fav = vm.store.isFavorite(w.en)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = cardColors(), elevation = cardElevation()) {
        Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(catGradient(w.category))),
                contentAlignment = Alignment.Center
            ) { Text(com.example.vocabmaster.data.WordRepository.emoji(w.category), fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.en, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp)); LevelDot(vm.store.level(w.en))
                }
                Text(w.he, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { speaker.speak(w.en, vm.rate) }) { Icon(Icons.Default.VolumeUp, "השמע", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { vm.toggleFav(w) }) {
                Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "מועדף", tint = if (fav) Danger else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BottomBar(vm: VocabVM) {
    val items = listOf(
        Triple(Screen.HOME, "בית", Icons.Default.Home),
        Triple(Screen.SEARCH, "חיפוש", Icons.Default.Search),
        Triple(Screen.FAVORITES, "מועדפים", Icons.Default.Favorite),
        Triple(Screen.STATS, "התקדמות", Icons.Default.BarChart)
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        items.forEach { (id, label, icon) ->
            NavigationBarItem(
                selected = vm.screen == id, onClick = { vm.screen = id }, icon = { Icon(icon, label) }, label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = cardColors(), elevation = cardElevation()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
