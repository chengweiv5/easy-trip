package com.yangchengwei.easytrip.share

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.yangchengwei.easytrip.amap.AmapConsentToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext

internal sealed interface ShareGeneration {
    data class Working(val options:ShareOptions,val message:String):ShareGeneration
    data class Ready(val options:ShareOptions,val image:ShareImage,val missingMaps:Boolean):ShareGeneration
    data class Failed(val options:ShareOptions,val tooLong:Boolean):ShareGeneration
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryShareScreen(
    tripId:String,
    loader:ShareSnapshotSource,
    consent:AmapConsentToken?,
    onBack:()->Unit,
) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val snackbar=remember { SnackbarHostState() }
    var trip by remember(tripId) { mutableStateOf<ShareTrip?>(null) }
    var loadFailed by remember(tripId) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var selectedDay by rememberSaveable(tripId) { mutableStateOf<String?>(null) }
    var includeNotes by rememberSaveable(tripId) { mutableStateOf(true) }
    var picker by remember { mutableStateOf(false) }
    var enlarged by rememberSaveable(tripId) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    val shareLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { sharing=false }
    var pendingSave by remember { mutableStateOf<ShareImage?>(null) }
    val options=ShareOptions(selectedDay,includeNotes)
    val host=remember(context) { FrameLayout(context) }
    val consentState=consent?.active?.collectAsState()?.value
    // A different request owns different state, so even a late completion cannot enable old output.
    var generation by remember(tripId,options,retry,consent,consentState) {
        mutableStateOf<ShareGeneration>(ShareGeneration.Working(options,"正在生成长图…"))
    }
    LaunchedEffect(tripId,options,retry,consent,consentState) {
        loadFailed=false
        val output=ShareImageStorage.newOutput(context)
        try {
            val currentTrip=try { loader.load(tripId) }
                catch(cancelled:CancellationException){throw cancelled}
                catch(_:Exception){loadFailed=true;return@LaunchedEffect}
            trip=currentTrip
            val days=currentTrip.selected(options)
            if(days.none { it.stops.isNotEmpty() })return@LaunchedEffect
            ShareImageStorage.pruneCache(context)
            val renderer=ShareImageRenderer()
            // Budget the full map height before loading any SDK views.
            renderer.render(currentTrip,options,days.associate { it.id to ShareDayMap(output) },output,measureOnly=true)
            val capture=ShareMapCapture(host,consent)
            val maps=mutableMapOf<String,ShareDayMap>()
            for((i,day)in days.withIndex()) {
                ensureActive()
                generation=ShareGeneration.Working(options,"正在整理第 ${i+1} / ${days.size} 天的地图…")
                if(day.stops.isNotEmpty())maps[day.id]=capture.capture(day)
            }
            generation=ShareGeneration.Working(options,"正在生成长图…")
            val image=renderer.render(currentTrip,options,maps,output)
            ensureActive()
            generation=ShareGeneration.Ready(options,image,maps.values.any { it.file==null })
        } catch(cancelled:CancellationException){output.delete();throw cancelled}
        catch(_:ShareImageTooLongException){output.delete();generation=ShareGeneration.Failed(options,true)}
        catch(_:OutOfMemoryError){output.delete();generation=ShareGeneration.Failed(options,false)}
        catch(_:Exception){output.delete();generation=ShareGeneration.Failed(options,false)}
    }
    val selected=trip?.selected(options).orEmpty()
    val isEmpty=trip!=null && selected.none { it.stops.isNotEmpty() }
    val ready=(generation as? ShareGeneration.Ready)?.takeIf { !isEmpty && !loadFailed }
    fun save(image:ShareImage) {
        saving=true
        scope.launch {
            val message=try { ShareImageStorage.save(context,image); "已保存到相册" }
            catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){"保存失败，请重试"}
            finally { saving=false }
            snackbar.showSnackbar(message)
        }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val image=pendingSave;pendingSave=null
        if(granted && image!=null)save(image)
        else scope.launch { snackbar.showSnackbar("未获得存储权限，仍可使用分享") }
    }
    fun requestSave(image:ShareImage) {
        if(Build.VERSION.SDK_INT<=28 && ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
            pendingSave=image;permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else save(image)
    }
    BackHandler(enlarged) { enlarged=false }
    if(enlarged && ready!=null) {
        Scaffold(topBar={TopAppBar(title={Text("长图预览")},navigationIcon={TextButton(onClick={enlarged=false}){Text("返回")}})}) { padding ->
            ShareImagePreview(ready.image,Modifier.padding(padding).fillMaxSize(),enlarged=true)
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
        // The SDK needs an attached, measured TextureView. The opaque UI covers this capture host.
        AndroidView(factory={host},modifier=Modifier.size(342.dp,171.dp))
        Scaffold(
            modifier=Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            topBar={TopAppBar(title={Text("分享行程长图")},navigationIcon={TextButton(onClick=onBack){Text("返回")}},actions={
                TextButton(onClick={enlarged=true},enabled=ready!=null,modifier=Modifier.testTag("share-expand")){Text("放大")}
            })},
            snackbarHost={SnackbarHost(snackbar)},
            bottomBar={Surface(shadowElevation=2.dp){Column(Modifier.navigationBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(if(saving)"正在保存…" else if(!includeNotes)"已隐藏备注 · 上下滑动查看完整内容" else "长图预览 · 上下滑动查看完整内容",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick={ready?.image?.let(::requestSave)},enabled=ready!=null&&!saving&&!sharing&&pendingSave==null,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("share-save")){Text("保存图片")}
                    Button(onClick={ready?.image?.let { image ->
                        try { sharing=true;shareLauncher.launch(Intent.createChooser(ShareImageStorage.shareIntent(context,image),"分享行程长图")) }
                        catch(_:Exception){sharing=false;scope.launch{snackbar.showSnackbar("无法打开分享，请先保存图片")}}
                    }},enabled=ready!=null&&!saving&&!sharing&&pendingSave==null,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("share-send")){Text("分享")}
                }
            }}},
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Surface { Column(Modifier.padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected=selectedDay==null,onClick={selectedDay=null},label={Text("全程")},modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("share-all"))
                        FilterChip(selected=selectedDay!=null,onClick={trip?.days?.firstOrNull()?.let { picker=true }},label={Text("选一天")},modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("share-single"))
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(if(selectedDay==null)"${trip?.days?.size ?: 0} 天行程" else "第 ${(selected.firstOrNull()?.index ?: 0)+1} 天",modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                        Text("包含备注",style=MaterialTheme.typography.bodyMedium)
                        Switch(checked=includeNotes,onCheckedChange={includeNotes=it},modifier=Modifier.testTag("share-notes"))
                    }
                    if(ready?.missingMaps==true) Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("部分地图暂不可用，清单已保留",modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                        TextButton(onClick={retry++},modifier=Modifier.testTag("share-retry-maps")){Text("重新生成")}
                    }
                    if(selectedDay!=null) TextButton(onClick={picker=true},modifier=Modifier.fillMaxWidth().testTag("share-day-picker")) {
                        Text(selected.firstOrNull()?.let { "第 ${it.index+1} 天 · ${it.date ?: "日期待定"}　⌄" } ?: "选择旅行日")
                    }
                }}
                when {
                    loadFailed -> ShareMessage("行程加载失败","重试",{retry++})
                    isEmpty -> ShareMessage(if(selectedDay==null)"还没有可以分享的行程" else "这一天还没有行程",if(selectedDay==null)"返回安排行程" else "分享全程",{if(selectedDay==null)onBack() else selectedDay=null})
                    ready!=null -> ShareImagePreview(ready.image,Modifier.weight(1f).testTag("share-image-preview"))
                    generation is ShareGeneration.Failed && (generation as ShareGeneration.Failed).options==options -> {
                        val tooLong=(generation as ShareGeneration.Failed).tooLong
                        ShareMessage(if(tooLong) "备注内容过多，请精简后重试" else "长图生成失败",
                            if(tooLong && includeNotes) "关闭备注" else if(tooLong) "返回调整行程" else "重试",
                            {if(tooLong && includeNotes)includeNotes=false else if(tooLong)onBack() else retry++})
                    }
                    else -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)){
                        CircularProgressIndicator();Text((generation as? ShareGeneration.Working)?.message ?: "正在生成长图…",modifier=Modifier.testTag("share-generating"))
                    }}
                }
            }
        }
    }
    if(picker) AlertDialog(onDismissRequest={picker=false},title={Text("选择旅行日")},text={LazyColumn {
        items(trip?.days.orEmpty(),key={it.id}) { day -> TextButton(onClick={selectedDay=day.id;picker=false},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("share-day-${day.index}")){Text("第 ${day.index+1} 天 · ${day.date ?: "日期待定"} · ${day.stops.size} 站")} }
    }},confirmButton={TextButton(onClick={picker=false}){Text("关闭")}})
}

@Composable
private fun ShareMessage(message:String,action:String,onAction:()->Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(message,modifier=Modifier.testTag("share-message"));Button(onClick=onAction){Text(action)}
    }}
}

@Composable
internal fun ShareImagePreview(image:ShareImage,modifier:Modifier=Modifier,enlarged:Boolean=false) {
    BoxWithConstraints(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        val width=if(enlarged)(maxWidth-32.dp)*1.7f else (maxWidth-32.dp).coerceAtLeast(1.dp)
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            LazyColumn(Modifier.width(width+32.dp),contentPadding=PaddingValues(16.dp)) {
                items((0 until image.height step 1200).toList(),key={"${image.file.name}:$it"}) { top ->
                    val height=minOf(1200,image.height-top)
                    var failed by remember(image.file,top,enlarged) { mutableStateOf(false) }
                    val bitmap by produceState<android.graphics.Bitmap?>(null,image.file,top,enlarged) {
                        var decoded:android.graphics.Bitmap?=null
                        try {
                            withContext(Dispatchers.IO) {
                                @Suppress("DEPRECATION") val decoder=BitmapRegionDecoder.newInstance(image.file.absolutePath,false)
                                try { decoded=decoder?.decodeRegion(Rect(0,top,image.width,top+height),BitmapFactory.Options().apply { inSampleSize=1 }) }
                                finally { decoder?.recycle() }
                            }
                            value=decoded
                            failed=decoded==null
                            awaitCancellation()
                        } catch(cancelled:CancellationException){throw cancelled}
                        catch(_:Exception){failed=true}
                        finally { value=null;decoded?.recycle() }
                    }
                    Box(Modifier.width(width).height(width*height/image.width).background(Color.White),contentAlignment=Alignment.Center) {
                        bitmap?.let { Image(it.asImageBitmap(),"行程长图",Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds) }
                        if(failed)Text("图片读取失败，请返回后重新生成",Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}
