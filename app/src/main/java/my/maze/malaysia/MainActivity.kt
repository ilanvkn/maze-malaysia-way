package my.maze.malaysia

import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.runtime.saveable.rememberSaveable
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
        HttpRequestUtil.setOkHttpClient(OkHttpClient.Builder().dispatcher(dispatcher).addNetworkInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent","MazeMalaysiaWay/${BuildConfig.VERSION_NAME} (Android offline community preview)").build()) }.build())
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
                    Place("current", "Current location", result.description, result.point), origin = true, accuracyMeters=result.accuracyMeters
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

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun MazeScreen(model:MazeViewModel,night:Boolean,onImportPack:()->Unit,onLocation:()->Unit) {
 val s=model.state;val colors=MaterialTheme.colorScheme
 val gateModel:MazeGateModel=viewModel()
 var sheet by rememberSaveable { mutableStateOf("") };var expanded by remember { mutableStateOf(false) };var explain by remember { mutableStateOf(false) }
 var renderedApproach by remember{mutableStateOf<ApproachPreview?>(null)}
 var mapReady by remember { mutableStateOf(false) };var header by remember{mutableStateOf(0)};var panel by remember{mutableStateOf(0)};var panelWidth by remember{mutableStateOf(0)}
 val window=LocalWindowInfo.current.containerSize;val wide=window.width>window.height;val compactHeader=wide&&LocalDensity.current.fontScale>=1.5f;val maxPanel=with(LocalDensity.current){window.height.toDp()}*(if(wide).78f else .50f)
 val route=s.routes.getOrNull(s.selectedRoute);val uri=LocalUriHandler.current;val context=LocalContext.current
 fun openExternal(url:String){try{uri.openUri(url)}catch(_:ActivityNotFoundException){model.message("No browser is available. You can visit the Maze GitHub project on another device.")}catch(_:IllegalArgumentException){model.message("This link could not be opened. Try the Maze GitHub project in your browser.")}}
 fun shareMaze(){try{context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,MAZE_INVITATION)},"Share Maze"))}catch(_:ActivityNotFoundException){model.message("No sharing app is available. Visit github.com/ilanvkn/maze-malaysia-way to share the project link.")}}
 var reportConfirmation by rememberSaveable{mutableStateOf(false)}
 val mapStyle=s.localBase+if(night)"/night.json" else "/day.json"
 if(!s.packReady&&route==null)MazeWelcome(s,onDownload={openExternal(MAZE_PACK_DOWNLOAD)},onImport=onImportPack,onCancel=model::cancelPackImport,onSaved={sheet="saved"},onCommunity={sheet="community"},onGatePilot={sheet="gatepilot"})
 else Box(Modifier.fillMaxSize().background(colors.background)) {
  val choosing=s.approachReviewActive
  if(s.packReady)MazeMap(s,Modifier.fillMaxSize(),topInset=header+20,bottomInset=if(wide)40 else panel+20,rightInset=if(wide)panelWidth+20 else 40,styleUri=mapStyle,onApproachApplied={renderedApproach=it}){mapReady=it;if(!it)renderedApproach=null}
  if(!choosing){
  Surface(Modifier.align(Alignment.TopStart).then(if(wide)Modifier.width(330.dp)else Modifier.fillMaxWidth()).onSizeChanged{header=it.height}.statusBarsPadding().padding(12.dp),shape=RoundedCornerShape(20.dp),shadowElevation=3.dp,color=colors.surfaceContainerLowest) {
   Column(Modifier.padding(horizontal=16.dp,vertical=4.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically) {
     Image(painterResource(R.drawable.maze_icon),null,Modifier.size(28.dp))
     if(compactHeader)Spacer(Modifier.weight(1f))else Column(Modifier.weight(1f).padding(start=8.dp)){Text("maze",fontSize=22.sp,fontWeight=FontWeight.Bold);if(!compactHeader)Text("MALAYSIA · OFFLINE",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)}
     TextButton(onClick={sheet="saved"}){Text("Saved")};TextButton(onClick={sheet="settings"}){Text("More")}
    }
    HorizontalDivider(color=colors.outlineVariant)
    TextButton(onClick={sheet="search"},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),contentPadding=PaddingValues(vertical=8.dp)) {
     Column(Modifier.weight(1f)){Text("Where to?",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant);Text(s.destination.name,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleMedium,color=colors.onSurface)}
     Icon(painterResource(R.drawable.ic_search),null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text("Search",style=MaterialTheme.typography.labelLarge)
    }
    if(!s.packReady&&s.packProgress==null)TextButton(onClick=onImportPack){Text("Import Malaysia pack")}
    if(s.packProgress!=null&&!s.packReady)LinearProgressIndicator(progress={s.packProgress},modifier=Modifier.fillMaxWidth())
   }
  }
  if(!wide)SmallFloatingActionButton(onClick={explain=true},modifier=Modifier.align(Alignment.CenterEnd).padding(end=16.dp),containerColor=colors.surfaceContainerLowest,contentColor=colors.primary){Icon(painterResource(R.drawable.ic_locate),"Locate me")}
  Surface(Modifier.align(if(wide)Alignment.BottomEnd else Alignment.BottomCenter).then(if(wide)Modifier.width(370.dp)else Modifier.fillMaxWidth()).padding(horizontal=12.dp).padding(bottom=8.dp).onSizeChanged{panel=it.height;panelWidth=it.width},shape=RoundedCornerShape(20.dp),shadowElevation=3.dp,color=colors.surfaceContainerLowest) {
   Column(Modifier.navigationBarsPadding().heightIn(max=maxPanel).verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=8.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text(if(route==null)"YOUR JOURNEY" else if(s.savedPreview)"SAVED PREVIEW" else if(s.sample)"RECORDED SAMPLE" else "DRIVING · OFFLINE",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant);Text(if(route==null)s.origin.name else "Est. ${routeMinutes(route.seconds)} min · ${routeDistanceLabel(route.meters)}",fontSize=22.sp,fontWeight=FontWeight.Bold)};TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Less detail" else "Trip details")} }
    if(!expanded)Text("${s.origin.name} → ${s.destination.name}",style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(vertical=4.dp))
    if(route!=null){
     if(!expanded)Text("Road preview · no live traffic",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
     routeEndGapLabel(route,s.destination)?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)}
    }
    if(s.origin.id=="current")Text(s.origin.area,style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
    if(s.routeBusy){LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical=6.dp));Text("Long offline trips can take up to 45 seconds.",style=MaterialTheme.typography.bodySmall);TextButton(onClick=model::cancelRoute){Text("Cancel route")}}
    if(s.origin.id=="current"&&needsManualRoadStart(s.originAccuracyMeters))TextButton(onClick={model.query("");sheet="origin"}){Text("Choose start manually")}
    if(s.message!=null)Surface(color=colors.secondaryContainer,shape=RoundedCornerShape(12.dp),modifier=Modifier.padding(vertical=6.dp).semantics{liveRegion=LiveRegionMode.Polite}){Text(s.message,Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)}
    if(expanded) {
     FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {TextButton(onClick={model.query("");sheet="origin"}){Text("Change start")};TextButton(onClick={explain=true}){Text("Locate me")};TextButton(onClick={sheet="offline"}){Text("Map area")} }
     TextButton(onClick={model.nearbyAccess();sheet="access"},enabled=s.packReady){Text("Nearby entrances & exits")}
     if(route!=null) {
      Text("${s.origin.name} to ${s.destination.name}",style=MaterialTheme.typography.bodyMedium)
      Text("${s.provider} · ${timeLabel(s.dataAt ?: s.fetchedAt)}",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
      Text("No live traffic · planning preview",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
      if(s.routes.size>1)LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(s.routes.indices.toList()){i->FilterChip(i==s.selectedRoute,{model.chooseRoute(i)},label={Text("Route ${i+1} · ${routeMinutes(s.routes[i].seconds)} min")})}}
      TextButton(onClick={model.reviewRoadApproach()},enabled=!s.routeBusy){Text("Change road approach")}
      Row {TextButton(onClick=model::saveTrip,enabled=!s.saving){Text(if(s.saving)"Saving…" else "Save trip")};TextButton(onClick=model::findRoutes,enabled=!s.routeBusy){Text("Refresh route")} }
     } else Text("A clearer way there. Choose your start and destination.",style=MaterialTheme.typography.bodyMedium)
    }
    Button(onClick={if(route==null)model.findRoutes()else sheet="directions"},enabled=!s.routeBusy&&(s.packReady||route!=null),modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).padding(vertical=4.dp),shape=RoundedCornerShape(16.dp)){Text(if(route!=null)"Road sequence" else if(s.routeBusy)"Finding routes…" else "Find route")}
    Text(if(mapReady)"© OpenStreetMap contributors · ODbL 1.0 · Geofabrik" else "Loading basemap · saved directions remain available",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
   }
  }
  }else MazeRoadChooser(s,wide,with(LocalDensity.current){window.height.toDp()}*(if(wide).88f else .55f),renderedApproach,model::previewRoadApproach,model::confirmRoadApproach,model::dismissRoadChoices,onHeader={header=it},onPanel={h,w->panel=h;panelWidth=w})
 }
 if(sheet.isNotEmpty())ModalBottomSheet(onDismissRequest={sheet=""},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
  Column(Modifier.fillMaxWidth().heightIn(max=with(LocalDensity.current){window.height.toDp()}*.85f).imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp).padding(bottom=28.dp)) {
   when(sheet) {
    "gatepilot"->MazeGatePilot(gateModel,s.packReady){place->model.select(place);sheet=""}
    "search","origin"->{Text(if(sheet=="origin")"Choose your start" else "Find your place",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(s.query,model::query,label={Text("Search Malaysia")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp));Text("On-device OSM search · shops, addresses, tagged entrances and motorway exits. Missing results do not mean a place does not exist.",style=MaterialTheme.typography.bodySmall);if(s.searchBusy)LinearProgressIndicator(Modifier.fillMaxWidth());s.results.forEach{p->ListItem(headlineContent={Text(p.name)},supportingContent={Text(placeDetailsLabel(p.area)+" · "+routeDistanceLabel(groundDistance(s.origin.point,p.point))+" from start")},modifier=Modifier.clickable{model.select(p,origin=sheet=="origin");sheet=""})};s.message?.let{Text(it,Modifier.semantics{liveRegion=LiveRegionMode.Polite})}}
    "access"->{TextButton(onClick={sheet="gatepilot"}){Text("Gate pilot")};Text("Mapped access nearby",style=MaterialTheme.typography.headlineSmall);Text("Within 300 m of ${s.accessFor}. Proximity does not establish a connection to this building or legal vehicle access.",Modifier.padding(vertical=12.dp));if(s.accessBusy)LinearProgressIndicator(Modifier.fillMaxWidth());if(s.accessError!=null)Text(s.accessError)else if(!s.accessBusy&&s.accessPoints.isEmpty())Text("No mapped entrances or motorway exits nearby in this extract. This does not mean none exist.");s.accessPoints.forEach{p->ListItem(headlineContent={Text(p.name)},supportingContent={Text(placeDetailsLabel(p.area))},modifier=Modifier.clickable{model.select(p);sheet=""})}}
    "directions"->{Text("Road sequence",style=MaterialTheme.typography.headlineSmall);Text("Road names in travel order · not turn-by-turn navigation. No live traffic. Access from the snapped road to the exact place is not verified.",Modifier.padding(vertical=12.dp));route?.steps?.forEach{step->ListItem(headlineContent={Text(step.text)},supportingContent={Text(routeDistanceLabel(step.meters))})}}
    "saved"->{Text("Saved trips",style=MaterialTheme.typography.headlineSmall);Text("Up to four trips, stored only on this device. Saved roads and estimates may be out of date.",Modifier.padding(vertical=12.dp));if(s.savedTrips.isEmpty())Text("No saved trips yet. Find a route, then choose Save trip.");s.savedTrips.forEach{trip->ListItem(headlineContent={Text(trip.destination.name)},supportingContent={Text("From ${trip.origin.name} · saved ${timeLabel(trip.savedAt)}"+(if(trip.sample)" · sample" else ""))},modifier=Modifier.clickable{model.openTrip(trip);sheet=""})};TextButton(onClick=model::clearTrips){Text("Clear saved trips")}}
    "offline"->{Text("Malaysia offline pack",style=MaterialTheme.typography.headlineSmall);Text(s.packStatus,Modifier.padding(vertical=12.dp));if(!s.packReady)Button(onClick=onImportPack,enabled=s.packProgress==null){Text("Choose offline pack")};Text("Maps, search and conservative driving previews run on this device. The regional extract also includes Singapore and Brunei. Shops and entrances are available only where mapped in OpenStreetMap. Building points do not identify a doorway. Motorway exits are tagged junctions, not building exits. Zoom above 14 enlarges existing detail. No live traffic or active navigation.");TextButton(onClick=model::removePack,enabled=s.packProgress==null&&!s.routeBusy){Text(if(s.packReady)"Remove offline pack" else "Clear old map files")};Text("© OpenStreetMap contributors · ODbL 1.0 · Geofabrik Shortbread",Modifier.padding(top=16.dp))}
    "credits"->{Text("Offline map credits",style=MaterialTheme.typography.headlineSmall);Text("© OpenStreetMap contributors · Open Database License 1.0. Regional extracts supplied by Geofabrik, Shortbread schema 1.0. Noto Sans glyphs use the SIL Open Font License 1.1; MapLibre demotiles packaging uses BSD 3-Clause. The regional pack includes Singapore and Brunei. Mapped features can be missing or outdated.",Modifier.padding(vertical=16.dp));Text("Data license: opendatacommons.org/licenses/odbl/1-0/\nAttribution: openstreetmap.org/copyright") }
    "community"->{
     Text("Peta kita. Bina bersama.",style=MaterialTheme.typography.headlineSmall)
     Text("Use your map without an account. If you want to help, there is a place for you here.",Modifier.padding(vertical=12.dp))
     OutlinedButton(onClick=::shareMaze,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Invite someone to Maze")}
     Text("Shares a project invitation only. Your location, searches and saved trips are not included.",style=MaterialTheme.typography.bodySmall)
     Text("One useful contribution",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=20.dp))
     TextButton(onClick={openExternal(MAZE_DISCUSSIONS)},modifier=Modifier.heightIn(min=48.dp)){Text("Say hello to the community")}
     TextButton(onClick={openExternal("$MAZE_HOME/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22")},modifier=Modifier.heightIn(min=48.dp)){Text("Find a first contribution")}
     TextButton(onClick={reportConfirmation=true},modifier=Modifier.heightIn(min=48.dp)){Text("Report a mapped place")}
     Text("Know a place? Spot a problem? Help with Malay, English, Chinese or Tamil copy, accessibility, or testing. Public reports should use public landmarks.",Modifier.padding(vertical=12.dp))
     Text("GitHub opens in your browser. Reading is public; posting needs your own GitHub account. Nothing is submitted automatically.",style=MaterialTheme.typography.bodySmall)
     s.message?.let{Text(it,Modifier.semantics{liveRegion=LiveRegionMode.Polite})}
    }
    "settings"->{Text("Make yourself at home",style=MaterialTheme.typography.headlineSmall);TextButton(onClick={sheet="gatepilot"},modifier=Modifier.heightIn(min=48.dp)){Text("Gate pilot")};Text("Appearance",Modifier.padding(top=16.dp));Appearance.entries.forEach{mode->Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected=s.appearance==mode,role=Role.RadioButton,onClick={model.appearance(mode)}),verticalAlignment=Alignment.CenterVertically){RadioButton(s.appearance==mode,null);Text(mode.name.lowercase().replaceFirstChar{it.uppercase()})}};Text("Route search effort",Modifier.padding(top=16.dp));RouteSearchMode.entries.forEach{mode->Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected=s.routeSearchMode==mode,role=Role.RadioButton,onClick={model.routeSearchMode(mode)}),verticalAlignment=Alignment.CenterVertically){RadioButton(s.routeSearchMode==mode,null);Text(mode.label)}};Text("Careful A* explores more of the supported road graph. It can time out and does not fix missing roads or provide live traffic.");Text("Maps, search and driving previews use the installed regional pack on this device. No API request is required. "+s.packStatus,Modifier.padding(vertical=12.dp));Text("MapLibre + Geofabrik Shortbread. © OpenStreetMap contributors, ODbL 1.0. No advertising SDK, account or background location. Searches and routes stay on this device. Data may be incomplete or outdated.");TextButton(onClick={sheet="community"},modifier=Modifier.heightIn(min=48.dp)){Text("Community & sharing")};TextButton(onClick={sheet="credits"}){Text("Map data credits")}}
   }
   TextButton(onClick={sheet=""},modifier=Modifier.heightIn(min=48.dp)){Text("Back to map")}
  }
 }
 if(reportConfirmation)AlertDialog(onDismissRequest={reportConfirmation=false},title={Text("Help improve this place")},text={Column{Text("Open a draft report on GitHub and review it before posting. No report is sent by Maze.");Text(if(publicOsmLink(s.destination.id)!=null)"Includes this selected public OSM object and the app version. Your GPS start, searches and saved trips are left out." else "This selection has no public OSM identity, so only the app version is prefilled. Add a public place manually.",Modifier.padding(top=12.dp))}},confirmButton={TextButton(onClick={reportConfirmation=false;openExternal(placeReportUrl(s.destination.id,BuildConfig.VERSION_NAME))}){Text("Review on GitHub")}},dismissButton={TextButton(onClick={reportConfirmation=false}){Text("Keep exploring")}})
 if(explain)AlertDialog(onDismissRequest={explain=false},title={Text("Your location, your choice")},text={Text("Use one foreground fix as the start. Coordinates are used on this device when you request a route. Saving that trip stores its coordinates on this device. No background location or history upload.")},confirmButton={TextButton(onClick={explain=false;onLocation()}){Text("Continue")}},dismissButton={TextButton(onClick={explain=false}){Text("Choose manually")}})
}
