package my.maze.malaysia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.MapLibre
import okhttp3.OkHttpClient

private val Forest=Color(0xFF0B715F)
private val Ink=Color(0xFF103C35)
private val Warm=Color(0xFFF7FAF6)
class MainActivity:ComponentActivity() {
    private var model:MazeViewModel?=null
    private val foregroundLocation by lazy { MazeForegroundLocation(this) }
    private val locationPermissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if(grants.values.any{it})findLocation() else model?.message("Location is optional. Choose a start point instead.")
    }
    private val packPicker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)model?.importPack(uri)}
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        MapLibre.getInstance(this)
        val dispatcher=okhttp3.Dispatcher().apply { maxRequests=4;maxRequestsPerHost=4 }
        HttpRequestUtil.setOkHttpClient(OkHttpClient.Builder().dispatcher(dispatcher).addNetworkInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent","MazeMalaysiaWay/0.3.1 (Android offline community preview)").build()) }.build())
        setContent {
            val vm:MazeViewModel=viewModel();model=vm
            MazeTheme(vm.state.appearance) { night -> SideEffect{androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply{isAppearanceLightStatusBars=!night;isAppearanceLightNavigationBars=!night}};MazeScreen(vm,night,onImportPack={packPicker.launch(arrayOf("application/zip","application/octet-stream"))}) { locationPermissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)) } }
        }
    }

    private fun findLocation() {
        model?.message("Finding your location… GPS may take longer outdoors without a network.")
        foregroundLocation.request { result ->
            when (result) {
                is LocationOutcome.Fix -> model?.select(
                    Place("current", "Current location", result.description, result.point), origin = true
                )
                is LocationOutcome.Unavailable -> model?.message(result.message)
            }
        }
    }
    override fun onStop() {
        if (foregroundLocation.cancel()) model?.message("Location request cancelled. Tap Locate me to try again, or choose a start point.")
        super.onStop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MazeScreen(model:MazeViewModel,night:Boolean,onImportPack:()->Unit,onLocation:()->Unit) {
 val s=model.state;val colors=MaterialTheme.colorScheme
 var sheet by remember { mutableStateOf("") };var expanded by remember { mutableStateOf(true) };var explain by remember { mutableStateOf(false) }
 var mapReady by remember { mutableStateOf(false) };var header by remember{mutableStateOf(0)};var panel by remember{mutableStateOf(0)};var panelWidth by remember{mutableStateOf(0)}
 val window=LocalWindowInfo.current.containerSize;val wide=window.width>window.height;val compactHeader=wide&&LocalDensity.current.fontScale>=1.5f;val maxPanel=with(LocalDensity.current){window.height.toDp()}*(if(wide).78f else .50f)
 val route=s.routes.getOrNull(s.selectedRoute);val uri=LocalUriHandler.current
 val mapStyle=s.localBase+if(night)"/night.json" else "/day.json"
 if(!s.packReady&&route==null)MazeWelcome(s,onDownload={runCatching{uri.openUri("https://github.com/ilanvkn/maze-malaysia-way/releases/download/v0.3.1-community.1/Maze-Malaysia-offline-2026-11-04.zip")}.onFailure{model.message("No browser is available. Download the matching pack from the Maze GitHub release page on another device.")}},onImport=onImportPack,onCancel=model::cancelPackImport,onSaved={sheet="saved"},onCommunity={sheet="community"})
 else Box(Modifier.fillMaxSize().background(colors.background)) {
  if(s.packReady)MazeMap(s,Modifier.fillMaxSize(),topInset=header+20,bottomInset=if(wide)40 else panel+20,rightInset=if(wide)panelWidth+20 else 40,styleUri=mapStyle){mapReady=it}
  Surface(Modifier.align(Alignment.TopStart).onSizeChanged{header=it.height}.then(if(wide)Modifier.width(330.dp)else Modifier.fillMaxWidth()).statusBarsPadding().padding(12.dp),shape=RoundedCornerShape(24.dp),shadowElevation=5.dp) {
   Column(Modifier.padding(horizontal=16.dp,vertical=8.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically) { Image(painterResource(R.drawable.maze_icon),"Maze",Modifier.size(32.dp));if(compactHeader)Spacer(Modifier.weight(1f))else Text("maze",Modifier.weight(1f).padding(start=10.dp),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=25.sp,fontWeight=FontWeight.Bold);TextButton(onClick={sheet="saved"}){Text("Saved")};TextButton(onClick={sheet="settings"}){Text("More")} }
    TextButton(onClick={sheet="search"},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Column(Modifier.weight(1f)) {Text("Where to?",style=MaterialTheme.typography.labelMedium);Text(s.destination.name,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold)};Text("Search") }
    Text(if(s.packReady&&compactHeader)"Offline pack ready" else if(s.packReady)"OFFLINE · Malaysia regional pack" else s.packStatus,style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
    if(!s.packReady&&s.packProgress==null)TextButton(onClick=onImportPack){Text("Import Malaysia pack")}
    if(s.packProgress!=null&&!s.packReady)LinearProgressIndicator(progress={s.packProgress},modifier=Modifier.fillMaxWidth())
    if(!s.online&&!compactHeader)Text(if(s.packReady)"Airplane-ready · on-device maps, search and routes" else if(s.packProgress!=null)"Offline pack preparing" else "Offline · import a map pack",style=MaterialTheme.typography.labelSmall)
   }
  }
  Surface(Modifier.align(if(wide)Alignment.BottomEnd else Alignment.BottomCenter).then(if(wide)Modifier.width(370.dp)else Modifier.fillMaxWidth()).onSizeChanged{panel=it.height;panelWidth=it.width},shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp),shadowElevation=8.dp) {
   Column(Modifier.navigationBarsPadding().heightIn(max=maxPanel).verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=8.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text(if(route==null)"YOUR JOURNEY" else if(s.savedPreview)"SAVED PREVIEW" else if(s.sample)"RECORDED SAMPLE" else "OFFLINE ROAD PREVIEW",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant);Text(if(route==null)s.origin.name else "${routeMinutes(route.seconds)} min · ${routeDistanceLabel(route.meters)}",fontSize=22.sp,fontWeight=FontWeight.Bold)};TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Less detail" else "Trip details")} }
    if(s.routeBusy){LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical=6.dp));Text("Long offline trips can take up to 45 seconds.",style=MaterialTheme.typography.bodySmall);TextButton(onClick=model::cancelRoute){Text("Cancel route")}}
    if(s.message!=null)Surface(color=colors.secondaryContainer,shape=RoundedCornerShape(12.dp),modifier=Modifier.padding(vertical=6.dp).semantics{liveRegion=LiveRegionMode.Polite}){Text(s.message,Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)}
    if(expanded) {
     Row {TextButton(onClick={model.query("");sheet="origin"}){Text("Change start")};TextButton(onClick={explain=true}){Text("Locate me")};TextButton(onClick={sheet="offline"}){Text("Map area")} }
     if(s.origin.id=="current")Text(s.origin.area,style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
     TextButton(onClick={model.nearbyAccess();sheet="access"},enabled=s.packReady){Text("Nearby entrances & exits")}
     if(route!=null) {
      Text("${s.origin.name} to ${s.destination.name}",style=MaterialTheme.typography.bodyMedium)
      Text("${s.provider} · ${timeLabel(s.dataAt ?: s.fetchedAt)}",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
      Text("No live traffic · planning preview",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
      if(s.routes.size>1)LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(s.routes.indices.toList()){i->FilterChip(i==s.selectedRoute,{model.chooseRoute(i)},label={Text("Route ${i+1} · ${routeMinutes(s.routes[i].seconds)} min")})}}
      Row {TextButton(onClick=model::saveTrip,enabled=!s.saving){Text(if(s.saving)"Saving…" else "Save trip")};TextButton(onClick=model::findRoutes,enabled=!s.routeBusy){Text("Refresh route")} }
     } else Text("A clearer way there. Choose your start and destination.",style=MaterialTheme.typography.bodyMedium)
    }
    Button(onClick={if(route==null)model.findRoutes()else sheet="directions"},enabled=!s.routeBusy&&(s.packReady||route!=null),modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).padding(vertical=4.dp),shape=RoundedCornerShape(16.dp)){Text(if(route!=null)"View road sequence" else if(s.routeBusy)"Finding routes…" else "Find road routes")}
    Text(if(mapReady)"© OpenStreetMap contributors · ODbL 1.0 · Geofabrik" else "Loading basemap · saved directions remain available",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
   }
  }
 }
 if(sheet.isNotEmpty())ModalBottomSheet(onDismissRequest={sheet=""},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
  Column(Modifier.fillMaxWidth().heightIn(max=with(LocalDensity.current){window.height.toDp()}*.85f).imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp).padding(bottom=28.dp)) {
   when(sheet) {
    "search","origin"->{Text(if(sheet=="origin")"Choose your start" else "Find your place",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(s.query,model::query,label={Text("Search Malaysia")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp));Text("On-device OSM search · shops, addresses, tagged entrances and motorway exits. Missing results do not mean a place does not exist.",style=MaterialTheme.typography.bodySmall);if(s.searchBusy)LinearProgressIndicator(Modifier.fillMaxWidth());s.results.forEach{p->ListItem(headlineContent={Text(p.name)},supportingContent={Text(placeDetailsLabel(p.area)+" · "+routeDistanceLabel(groundDistance(s.origin.point,p.point))+" from start")},modifier=Modifier.clickable{model.select(p,origin=sheet=="origin");sheet=""})};s.message?.let{Text(it,Modifier.semantics{liveRegion=LiveRegionMode.Polite})}}
    "access"->{Text("Mapped access nearby",style=MaterialTheme.typography.headlineSmall);Text("Within 300 m of ${s.accessFor}. Proximity does not establish a connection to this building or legal vehicle access.",Modifier.padding(vertical=12.dp));if(s.accessBusy)LinearProgressIndicator(Modifier.fillMaxWidth());if(s.accessError!=null)Text(s.accessError)else if(!s.accessBusy&&s.accessPoints.isEmpty())Text("No mapped entrances or motorway exits nearby in this extract. This does not mean none exist.");s.accessPoints.forEach{p->ListItem(headlineContent={Text(p.name)},supportingContent={Text(placeDetailsLabel(p.area))},modifier=Modifier.clickable{model.select(p);sheet=""})}}
    "directions"->{Text("Road sequence",style=MaterialTheme.typography.headlineSmall);Text("Road names in travel order · not turn-by-turn navigation. No live traffic. Access from the snapped junction to the exact place is not verified.",Modifier.padding(vertical=12.dp));route?.steps?.forEach{step->ListItem(headlineContent={Text(step.text)},supportingContent={Text(routeDistanceLabel(step.meters))})}}
    "saved"->{Text("Saved trips",style=MaterialTheme.typography.headlineSmall);Text("Up to four trips, stored only on this device. Saved roads and estimates may be out of date.",Modifier.padding(vertical=12.dp));if(s.savedTrips.isEmpty())Text("No saved trips yet. Find a route, then choose Save trip.");s.savedTrips.forEach{trip->ListItem(headlineContent={Text(trip.destination.name)},supportingContent={Text("From ${trip.origin.name} · saved ${timeLabel(trip.savedAt)}"+(if(trip.sample)" · sample" else ""))},modifier=Modifier.clickable{model.openTrip(trip);sheet=""})};TextButton(onClick=model::clearTrips){Text("Clear saved trips")}}
    "offline"->{Text("Malaysia offline pack",style=MaterialTheme.typography.headlineSmall);Text(s.packStatus,Modifier.padding(vertical=12.dp));if(!s.packReady)Button(onClick=onImportPack,enabled=s.packProgress==null){Text("Choose offline pack")};Text("Maps, search and conservative driving previews run on this device. The regional extract also includes Singapore and Brunei. Shops and entrances are available only where mapped in OpenStreetMap. Building points do not identify a doorway. Motorway exits are tagged junctions, not building exits. Zoom above 14 enlarges existing detail. No live traffic or active navigation.");TextButton(onClick=model::removePack,enabled=s.packProgress==null&&!s.routeBusy){Text(if(s.packReady)"Remove offline pack" else "Clear old map files")};Text("© OpenStreetMap contributors · ODbL 1.0 · Geofabrik Shortbread",Modifier.padding(top=16.dp))}
    "credits"->{Text("Offline map credits",style=MaterialTheme.typography.headlineSmall);Text("© OpenStreetMap contributors · Open Database License 1.0. Regional extracts supplied by Geofabrik, Shortbread schema 1.0. Noto Sans glyphs use the SIL Open Font License 1.1; MapLibre demotiles packaging uses BSD 3-Clause. The regional pack includes Singapore and Brunei. Mapped features can be missing or outdated.",Modifier.padding(vertical=16.dp));Text("Data license: opendatacommons.org/licenses/odbl/1-0/\nAttribution: openstreetmap.org/copyright") }
    "community"->{Text("Build Maze together",style=MaterialTheme.typography.headlineSmall);Text("No account is needed to use your map. GitHub is optional when you choose to contribute.");TextButton(onClick={runCatching{uri.openUri("https://github.com/ilanvkn/maze-malaysia-way/discussions/4")}}){Text("Join the conversation")}}
    "settings"->{Text("Make yourself at home",style=MaterialTheme.typography.headlineSmall);Text("Appearance",Modifier.padding(top=16.dp));Appearance.entries.forEach{mode->Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected=s.appearance==mode,role=Role.RadioButton,onClick={model.appearance(mode)}),verticalAlignment=Alignment.CenterVertically){RadioButton(s.appearance==mode,null);Text(mode.name.lowercase().replaceFirstChar{it.uppercase()})}};Text("Route search effort",Modifier.padding(top=16.dp));RouteSearchMode.entries.forEach{mode->Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected=s.routeSearchMode==mode,role=Role.RadioButton,onClick={model.routeSearchMode(mode)}),verticalAlignment=Alignment.CenterVertically){RadioButton(s.routeSearchMode==mode,null);Text(mode.label)}};Text("Careful A* explores more of the supported road graph. It can time out and does not fix missing roads or provide live traffic.");Text("Maps, search and driving previews use the installed regional pack on this device. No API request is required. "+s.packStatus,Modifier.padding(vertical=12.dp));Text("MapLibre + Geofabrik Shortbread. © OpenStreetMap contributors, ODbL 1.0. No advertising SDK, account or background location. Searches and routes stay on this device. Data may be incomplete or outdated.");TextButton(onClick={sheet="credits"}){Text("Map data credits")}}
   }
   TextButton(onClick={sheet=""},modifier=Modifier.heightIn(min=48.dp)){Text("Back to map")}
  }
 }
 if(explain)AlertDialog(onDismissRequest={explain=false},title={Text("Your location, your choice")},text={Text("Use one foreground fix as the start. Coordinates are used on this device when you request a route. Saving that trip stores its coordinates on this device. No background location or history upload.")},confirmButton={TextButton(onClick={explain=false;onLocation()}){Text("Continue")}},dismissButton={TextButton(onClick={explain=false}){Text("Choose manually")}})
}
