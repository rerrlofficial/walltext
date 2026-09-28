package com.walltext.app

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import coil.compose.AsyncImage
import com.walltext.app.data.WallTextStore
import com.walltext.app.model.*
import com.walltext.app.ui.Theme
import com.walltext.app.wallpaper.WallpaperRenderer
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { Theme { WallTextApp(this) } } }
}

@Composable
private fun WallTextApp(context: Context) {
    val store = remember { WallTextStore(context) }; val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(WallpaperConfig(UUID.randomUUID().toString())) }
    var selected by remember { mutableIntStateOf(0) }
    val history by store.history.collectAsState(initial = emptyList())
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        config = config.copy(wallpaperUri = uri.toString())
    }
    Scaffold(topBar={ TopAppBar(title={Text("WallText")}, actions={ TextButton(onClick={scope.launch{store.save(config); applyWallpaper(context,config)}}){Text("Apply")}})}, bottomBar={
        NavigationBar { listOf("Wallpaper","Editor","History","Settings").forEachIndexed { i,n -> NavigationBarItem(selected=i==selected,onClick={selected=i},icon={},label={Text(n)}) } }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when(selected){
                0 -> WallpaperTab(config,{config=it},{picker.launch(arrayOf("image/*"))})
                1 -> EditorTab(config,{config=it})
                2 -> HistoryTab(history){ config=it }
                else -> SettingsTab(config){config=it}
            }
        }
    }
}

@Composable private fun WallpaperTab(c: WallpaperConfig,onChange:(WallpaperConfig)->Unit,onPick:()->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("Wallpaper",style=MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth().height(420.dp)){ Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ if(c.wallpaperUri!=null) AsyncImage(c.wallpaperUri,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop) else Box(Modifier.fillMaxSize().background(Color(c.wallpaperColor)),contentAlignment=Alignment.Center){Text("No wallpaper selected",color=Color.White)} } }
        Button(onClick=onPick,Modifier.fillMaxWidth()){Text("Choose wallpaper")}
        OutlinedButton(onClick={onChange(c.copy(wallpaperUri=null,wallpaperColor=0xFF101114))},Modifier.fillMaxWidth()){Text("Use solid background")}
    }
}

@Composable private fun EditorTab(c: WallpaperConfig,onChange:(WallpaperConfig)->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Text placeholders",style=MaterialTheme.typography.headlineSmall)
        Text("Drag a placeholder in the preview to reposition it. The same position is used when rendering the lock-screen wallpaper.",style=MaterialTheme.typography.bodyMedium)
        Box(Modifier.fillMaxWidth().height(360.dp).background(Color(0xFF101114), RoundedCornerShape(20.dp))) {
            c.placeholders.forEachIndexed { i, p ->
                Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment=Alignment.TopStart) {
                    Text(
                        text = previewText(p), color = Color.White.copy(alpha=p.opacity), fontSize=p.fontSizeSp.sp,
                        modifier = Modifier.fillMaxWidth(p.width).offset((p.x*320-160).dp,(p.y*320-20).dp)
                            .pointerInput(p.id){detectDragGestures{change,drag->change.consume(); val nx=(p.x+drag.x/320f).coerceIn(0f,1f); val ny=(p.y+drag.y/320f).coerceIn(0f,1f); onChange(c.copy(placeholders=c.placeholders.mapIndexed{j,x->if(j==i)x.copy(x=nx,y=ny) else x}))}}
                    )
                }
            }
        }
        c.placeholders.forEachIndexed { i,p -> PlaceholderCard(i,p,{np -> onChange(c.copy(placeholders=c.placeholders.mapIndexed{j,x->if(j==i)np else x}))},{onChange(c.copy(placeholders=c.placeholders.filterIndexed{j,_->j!=i}))}) }
        if(c.placeholders.size<5){ Button(onClick={onChange(c.copy(placeholders=c.placeholders+TextPlaceholder(UUID.randomUUID().toString(),title="Placeholder ${c.placeholders.size+1}")))},Modifier.fillMaxWidth()){Text("+ Add placeholder")} }
    }
}

@Composable private fun PlaceholderCard(index:Int,p:TextPlaceholder,onChange:(TextPlaceholder)->Unit,onDelete:()->Unit){
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()){ Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Text(p.title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Close" else "Edit")};TextButton(onClick=onDelete){Text("Delete")}}
        if(expanded){
            OutlinedTextField(p.title,{onChange(p.copy(title=it))},label={Text("Name")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ listOf(PlaceholderType.TEXT,PlaceholderType.TABLE,PlaceholderType.CHECKLIST,PlaceholderType.BULLETS).forEach{t->FilterChip(selected=p.type==t,onClick={onChange(p.copy(type=t))},label={Text(t.name.lowercase().replaceFirstChar{it.uppercase()})})} }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=p.mode==DisplayMode.STATIC,onClick={onChange(p.copy(mode=DisplayMode.STATIC))},label={Text("Static")});FilterChip(selected=p.mode==DisplayMode.DYNAMIC,onClick={onChange(p.copy(mode=DisplayMode.DYNAMIC))},label={Text("Dynamic")})}
            if(p.mode==DisplayMode.STATIC){OutText(p.entries.firstOrNull().orEmpty()){onChange(p.copy(entries=listOf(it)))}}
            else {Text("Dynamic entries (max 10)");p.entries.take(10).forEachIndexed{j,e->OutText(e,{v->onChange(p.copy(entries=p.entries.mapIndexed{k,x->if(k==j)v else x}))},"Entry ${j+1}")};if(p.entries.size<10)TextButton(onClick={onChange(p.copy(entries=p.entries+""))}){Text("+ Add entry")}}
            Text("Font size: ${p.fontSizeSp.toInt()} sp");Slider(value=p.fontSizeSp,onValueChange={onChange(p.copy(fontSizeSp=it))},valueRange=12f..48f)
        }
    }}
}

@Composable private fun previewText(p:TextPlaceholder):String = when(p.type){PlaceholderType.TEXT->p.entries.firstOrNull().orEmpty();PlaceholderType.BULLETS->p.entries.firstOrNull().orEmpty().lines().joinToString("\n"){"• $it"};PlaceholderType.CHECKLIST->p.entries.firstOrNull().orEmpty().lines().joinToString("\n"){"☐ $it"};PlaceholderType.TABLE->p.entries.firstOrNull().orEmpty()}

@Composable private fun OutText(value:String,onValueChange:(String)->Unit,label:String="Content"){OutlinedTextField(value,onValueChange,label={Text(label)},minLines=3,modifier=Modifier.fillMaxWidth())}

@Composable private fun HistoryTab(items:List<WallpaperConfig>,onUse:(WallpaperConfig)->Unit){
    if(items.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("No history yet")}
    else LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(items){c->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text("${c.placeholders.size} placeholders",Modifier.weight(1f));TextButton(onClick={onUse(c)}){Text("Use")}}}}}
}

@Composable private fun SettingsTab(c:WallpaperConfig,onChange:(WallpaperConfig)->Unit){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Settings",style=MaterialTheme.typography.headlineSmall);Text("Dynamic wallpaper refresh interval");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(15,30,60).forEach{m->FilterChip(selected=c.dynamicIntervalMinutes==m,onClick={onChange(c.copy(dynamicIntervalMinutes=m))},label={Text("$m min")})}};Text("Dynamic mode uses up to 10 entries per placeholder and cycles through them.")}}

private fun applyWallpaper(context:Context,config:WallpaperConfig){
    val wm=WallpaperManager.getInstance(context);val base=if(config.wallpaperUri!=null){context.contentResolver.openInputStream(Uri.parse(config.wallpaperUri))?.use{BitmapFactory.decodeStream(it)}}else null
    val bitmap=base ?: Bitmap.createBitmap(1080,2400,Bitmap.Config.ARGB_8888).also{it.eraseColor(config.wallpaperColor.toInt())}
    val rendered=WallpaperRenderer.render(bitmap,config)
    val bytes=ByteArrayOutputStream().also{rendered.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray()
    wm.setStream(bytes.inputStream(),null,true,WallpaperManager.FLAG_LOCK)
    if(config.placeholders.any { it.mode==DisplayMode.DYNAMIC }) { val req=PeriodicWorkRequestBuilder<com.walltext.app.wallpaper.DynamicWallpaperWorker>(config.dynamicIntervalMinutes.toLong(),TimeUnit.MINUTES).build(); WorkManager.getInstance(context).enqueueUniquePeriodicWork("dynamic_wallpaper",ExistingPeriodicWorkPolicy.UPDATE,req) } else WorkManager.getInstance(context).cancelUniqueWork("dynamic_wallpaper")
}
