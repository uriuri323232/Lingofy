package com.example.vocabmaster

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vocabmaster.data.*
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { VocabApp() } }
}

class VocabVM : ViewModel() {
    lateinit var repo: WordRepository; lateinit var store: ProgressStore
    var screen by mutableStateOf("home"); var selectedCategory by mutableStateOf("הכול"); var query by mutableStateOf("")
    var flashIndex by mutableIntStateOf(0); var revealed by mutableStateOf(false); var quizIndex by mutableIntStateOf(0)
    var quizOptions by mutableStateOf(listOf<Word>()); var quizAnswer by mutableStateOf<Word?>(null); var quizAnswered by mutableStateOf(false); var quizCorrect by mutableStateOf(false)
    var dark by mutableStateOf(false)
    val filtered: List<Word> get() = repo.words.filter { (selectedCategory=="הכול" || it.category==selectedCategory) && (query.isBlank() || it.en.contains(query,true) || it.he.contains(query,true)) }
    fun init(context: android.content.Context) { if (!::repo.isInitialized) { repo=WordRepository(context); store=ProgressStore(context) } }
    fun startFlash() { flashIndex=0; revealed=false; screen="learn" }
    fun nextFlash() { flashIndex=(flashIndex+1)%filtered.size.coerceAtLeast(1); revealed=false }
    fun startQuiz() { quizIndex=0; newQuiz(); screen="quiz" }
    fun newQuiz() { val pool=filtered.shuffled().ifEmpty { repo.words.shuffled() }; val answer=pool.first(); quizAnswer=answer; quizOptions=(listOf(answer)+repo.words.filter{it.en!=answer.en}.shuffled().take(3)).shuffled(); quizAnswered=false; quizCorrect=false }
}

@Composable fun VocabApp(vm: VocabVM = viewModel()) {
    val context=LocalContext.current; vm.init(context)
    MaterialTheme(colorScheme = if(vm.dark) darkColorScheme() else lightColorScheme(), typography = Typography()) {
        Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize()) {
            AppTopBar(vm)
            Box(Modifier.weight(1f)) { when(vm.screen) { "learn" -> LearnScreen(vm); "quiz" -> QuizScreen(vm); "search" -> SearchScreen(vm); "favorites" -> FavoritesScreen(vm); "stats" -> StatsScreen(vm); else -> HomeScreen(vm) } } 
            BottomBar(vm)
        } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppTopBar(vm: VocabVM) { TopAppBar(title={Text("מילים בכיף",fontWeight=FontWeight.Bold)}, actions={ IconButton(onClick={vm.dark=!vm.dark}){Icon(if(vm.dark) Icons.Default.LightMode else Icons.Default.DarkMode,"מצב תצוגה")} }) }

@Composable fun HomeScreen(vm: VocabVM) {
    val learned=vm.store.learnedCount(vm.repo.words); val pct=(learned.toFloat()/vm.repo.words.size).coerceIn(0f,1f); val anim by animateFloatAsState(pct,label="progress")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("לומדים חכם. זוכרים יותר.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text("הכול נשמר במכשיר ועובד גם בלי אינטרנט.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Card(shape=RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){ Text("ההתקדמות שלך",fontWeight=FontWeight.Bold); LinearProgressIndicator({anim},Modifier.fillMaxWidth()); Text("$learned מתוך ${vm.repo.words.size} מילים נלמדו") } } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){ ActionCard("לימוד",Icons.Default.Style,Modifier.weight(1f)){vm.startFlash()}; ActionCard("חידון",Icons.Default.Quiz,Modifier.weight(1f)){vm.startQuiz()} } }
        item { Text("קטגוריות",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
        items(vm.repo.categories.chunked(2)) { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){ row.forEach { c -> FilterChip(selected=vm.selectedCategory==c,onClick={vm.selectedCategory=c},label={Text(c)},modifier=Modifier.weight(1f)) }; if(row.size==1) Spacer(Modifier.weight(1f)) } }
    }
}

@Composable fun ActionCard(title:String, icon:ImageVector, modifier:Modifier, onClick:()->Unit) { Card(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,Modifier.size(30.dp));Spacer(Modifier.height(8.dp));Text(title,fontWeight=FontWeight.Bold)}} }

@Composable fun LearnScreen(vm:VocabVM) {
    val context=LocalContext.current
    val speaker=remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine=TextToSpeech(context) { status -> if(status==TextToSpeech.SUCCESS) speaker.value?.language=Locale.US }
        speaker.value=engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    val list=vm.filtered.ifEmpty{vm.repo.words}; val word=list[vm.flashIndex.coerceIn(0,list.lastIndex)]
    Column(Modifier.fillMaxSize().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("כרטיסייה ${vm.flashIndex+1}/${list.size}"); Text(word.category,color=MaterialTheme.colorScheme.primary)}
        Card(Modifier.fillMaxWidth().weight(1f).clickable{vm.revealed=!vm.revealed},shape=RoundedCornerShape(30.dp)) { Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(word.en,style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);if(vm.revealed){Spacer(Modifier.height(18.dp));Text(word.he,style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.height(8.dp));Text("לחץ על הבא כדי להמשיך",color=MaterialTheme.colorScheme.onSurfaceVariant)}}} }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ OutlinedButton(onClick={vm.store.toggleFavorite(word.en)}){Icon(if(vm.store.isFavorite(word.en)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,null);Text("מועדף")}; OutlinedButton(onClick={speaker.value?.speak(word.en, TextToSpeech.QUEUE_FLUSH, null, "word")}){Icon(Icons.Default.VolumeUp,null);Text("השמע")} }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Button(onClick={vm.store.wrong(word.en);vm.nextFlash()},Modifier.weight(1f)){Text("צריך חזרה")};Button(onClick={vm.store.correct(word.en);vm.nextFlash()},Modifier.weight(1f)){Text("ידעתי ✓")}}
    }
}



@Composable fun QuizScreen(vm:VocabVM) {
    val answer=vm.quizAnswer ?: return
    Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("חידון",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text("מה התרגום של המילה:")
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Text(answer.en,Modifier.fillMaxWidth().padding(30.dp),style=MaterialTheme.typography.displaySmall,textAlign=TextAlign.Center,fontWeight=FontWeight.Bold)}
        vm.quizOptions.drop(1).let { /* keep answer randomized in options below */ }
        vm.quizOptions.forEach { option -> Button(onClick={if(!vm.quizAnswered){vm.quizCorrect=option.en==answer.en;vm.quizAnswered=true; if(vm.quizCorrect)vm.store.correct(answer.en) else vm.store.wrong(answer.en);vm.store.record(vm.quizCorrect)}},Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=if(vm.quizAnswered && option.en==answer.en) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,contentColor=MaterialTheme.colorScheme.onSurfaceVariant)){Text(option.he)} }
        if(vm.quizAnswered){Text(if(vm.quizCorrect) "מצוין! תשובה נכונה 🎉" else "כמעט! התשובה הנכונה: ${answer.he}",fontWeight=FontWeight.Bold);Button(onClick={vm.newQuiz()},Modifier.align(Alignment.CenterHorizontally)){Text("שאלה הבאה")}}
    }
}

@Composable fun SearchScreen(vm:VocabVM){ Column(Modifier.fillMaxSize().padding(16.dp)){ OutlinedTextField(vm.query,{vm.query=it},Modifier.fillMaxWidth(),label={Text("חפש באנגלית או בעברית")},leadingIcon={Icon(Icons.Default.Search,null)}); Spacer(Modifier.height(10.dp)); LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(vm.filtered){WordRow(it,vm)}} } }
@Composable fun WordRow(w:Word,vm:VocabVM){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(15.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text(w.en,fontWeight=FontWeight.Bold);Text(w.he,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(w.category,color=MaterialTheme.colorScheme.primary)}}}
@Composable fun FavoritesScreen(vm:VocabVM){val fav=vm.store.favorites(vm.repo.words); if(fav.isEmpty()) EmptyState("עדיין אין מועדפים","סמן מילים בכרטיסיות והן יופיעו כאן") else LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(fav){WordRow(it,vm)}}}
@Composable fun StatsScreen(vm:VocabVM){val attempts=vm.store.attempts();val success=vm.store.successes();Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Text("הסטטיסטיקות שלך",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Stat("מילים במאגר",vm.repo.words.size.toString());Stat("מילים שנלמדו",vm.store.learnedCount(vm.repo.words).toString());Stat("ניסיונות בחידון",attempts.toString());Stat("תשובות נכונות",success.toString());Stat("רצף ימים",vm.store.streak().toString())}}
@Composable fun Stat(a:String,b:String){Card{Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(a);Text(b,fontWeight=FontWeight.Bold)}}}
@Composable fun EmptyState(a:String,b:String){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.FavoriteBorder,null,Modifier.size(50.dp));Spacer(Modifier.height(12.dp));Text(a,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(b,color=MaterialTheme.colorScheme.onSurfaceVariant)}}

@Composable fun BottomBar(vm:VocabVM){NavigationBar{listOf(Triple("home","בית",Icons.Default.Home),Triple("search","חיפוש",Icons.Default.Search),Triple("favorites","מועדפים",Icons.Default.Favorite),Triple("stats","התקדמות",Icons.Default.BarChart)).forEach{(id,label,icon)->NavigationBarItem(selected=vm.screen==id,onClick={vm.screen=id},icon={Icon(icon,null)},label={Text(label)})}}}
