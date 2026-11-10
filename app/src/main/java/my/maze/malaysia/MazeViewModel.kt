package my.maze.malaysia

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

enum class Appearance { SYSTEM, DAY, NIGHT }
data class MazeState(
 val query:String="",val origin:Place=landmarks[0],val destination:Place=landmarks[1],val results:List<Place> = landmarks,
 val approachReviewActive:Boolean=false,val originAccuracyMeters:Float?=null,val previewChoice:RoadChoice?=null,val approachPreview:ApproachPreview?=null,val approachPreviewBusy:Boolean=false,val approachPreviewError:String?=null,val roadChoices:List<RoadChoice> = emptyList(),val destinationApproach:Long?=null,val originApproach:Long?=null,val reviewingOrigin:Boolean=false,val searchBusy:Boolean=false,val routeBusy:Boolean=false,val routes:List<RoadRoute> = emptyList(),val selectedRoute:Int=0,
 val message:String?=null,val fetchedAt:Long?=null,val provider:String="",val dataAt:Long?=null,val sample:Boolean=BuildConfig.IS_LAB,
 val routeSearchMode:RouteSearchMode=RouteSearchMode.QUICK,val savedPreview:Boolean=false,val savedTrips:List<SavedTrip> = emptyList(),val saving:Boolean=false,val appearance:Appearance=Appearance.SYSTEM,
 val packChecking:Boolean=true,val packError:String?=null,val accessPoints:List<Place> = emptyList(),val accessBusy:Boolean=false,val accessFor:String="",val accessError:String?=null,val packProgress:Float?=null,val localBase:String="",val packStatus:String="Preparing offline Malaysia pack…",val packReady:Boolean=false,val online:Boolean=true,val offlineArea:OfflineAreaState=OfflineAreaState(),val servicesConfigured:Boolean=false
)
private fun restorePlace(saved:SavedStateHandle,key:String,fallback:Place):Place {
 val id=saved.get<String>(key) ?: return fallback;landmarks.find{it.id==id}?.let{return it};if(id=="current")return fallback
 return runCatching{val point=GeoPoint(saved.get<Double>(key+"-lat")!!,saved.get<Double>(key+"-lon")!!);require(inMalaysiaRegion(point));Place(id,saved.get<String>(key+"-name")!!,saved.get<String>(key+"-area") ?: "Malaysia",point)}.getOrDefault(fallback)
}
class MazeViewModel(application:Application,private val saved:SavedStateHandle):AndroidViewModel(application) {
 private var localPack=MazeLocalPack(application);private val store=TripStore(application);private val preferences=application.getSharedPreferences("maze-settings",Context.MODE_PRIVATE)
 var state by mutableStateOf(MazeState(query=saved["query"] ?: "",origin=restorePlace(saved,"origin",landmarks[0]),destination=restorePlace(saved,"destination",landmarks[1]),appearance=runCatching{Appearance.valueOf(preferences.getString("appearance","SYSTEM")!!)}.getOrDefault(Appearance.SYSTEM),servicesConfigured=false,routeSearchMode=runCatching{RouteSearchMode.valueOf(preferences.getString("route-search","QUICK")!!)}.getOrDefault(RouteSearchMode.QUICK)));private set
 private var previewJob:Job?=null;private var previewGeneration=0;private val previewCache=RouteCache<Pair<Long,GeoPoint>,ApproachPreview>(4)
 private var importJob:Job?=null;private var accessJob:Job?=null;private var searchJob:Job?=null;private var routeJob:Job?=null;private var searchGeneration=0;private var routeGeneration=0
 private val connectivity=application.getSystemService(ConnectivityManager::class.java)
 private val networkCallback=object:ConnectivityManager.NetworkCallback(){override fun onCapabilitiesChanged(network:Network,c:NetworkCapabilities){viewModelScope.launch{state=state.copy(online=c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))}};override fun onLost(network:Network){viewModelScope.launch{state=state.copy(online=false)}}}
 init {preparePack();connectivity.registerDefaultNetworkCallback(networkCallback);val c=connectivity.getNetworkCapabilities(connectivity.activeNetwork);state=state.copy(online=c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true);viewModelScope.launch{try{val trips=withContext(Dispatchers.IO){store.read()};state=state.copy(savedTrips=trips)}catch(e:Exception){state=state.copy(message="Saved trips could not be opened. You can clear them from Saved trips.")}}}
 private fun preparePack(){viewModelScope.launch{try{withContext(Dispatchers.IO){localPack.open()};state=state.copy(localBase=localPack.base,packReady=true,packChecking=false,packError=null,packProgress=null,packStatus=localPack.summary);query(state.query)}catch(e:Exception){localPack.close();if(e is CancellationException)throw e;state=state.copy(packReady=false,packChecking=false,packProgress=null,packStatus="Import the Malaysia offline pack to start maps, search and routing.")}}}
 fun importPack(uri:android.net.Uri){
  if(state.packReady||state.packChecking||state.packProgress!=null)return
  state=state.copy(packProgress=0f,packError=null,packStatus="Checking Malaysia pack…",message=null)
  importJob=viewModelScope.launch{
   try{
    withContext(Dispatchers.IO){importMalaysiaPack(getApplication(),uri){done,total->withContext(Dispatchers.Main){state=state.copy(packProgress=done.toFloat()/total,packStatus="Importing Malaysia pack · ${done*100/total}%")}}}
    localPack=MazeLocalPack(getApplication());state=state.copy(packChecking=true,packProgress=null);preparePack()
   }catch(e:CancellationException){state=state.copy(packProgress=null,packChecking=false,packStatus="Import cancelled. Choose your ZIP to try again.",packError=null,message="Import cancelled. Choose your ZIP to try again.");throw e}
   catch(e:Exception){state=state.copy(packProgress=null,packChecking=false,packError=importFailureMessage(e),packStatus="Your map is not imported yet.")}
  }
 }
 fun cancelPackImport(){importJob?.cancel()}
 fun removePack(){if(state.packProgress!=null||state.routeBusy)return;clearApproachPreview();previewCache.clear();searchGeneration++;searchJob?.cancel();accessJob?.cancel();localPack.close();state=state.copy(packReady=false,localBase="",approachReviewActive=false,destinationApproach=null,originApproach=null,roadChoices=emptyList(),packProgress=0f,packStatus="Removing offline map files…",results=landmarks,accessPoints=emptyList(),accessBusy=false);viewModelScope.launch{try{withContext(Dispatchers.IO){java.io.File(getApplication<Application>().noBackupFilesDir,"regional-pack").listFiles()?.filter{isRegionalPackFile(it.name)}?.forEach{check(it.delete())}};localPack=MazeLocalPack(getApplication());state=state.copy(packProgress=null,packStatus="Offline map files removed. Saved trips remain on this device. Import a pack to use maps and search.")}catch(e:Exception){state=state.copy(packProgress=null,packStatus="Some map files could not be removed. Restart and try again.")}}}
 fun nearbyAccess(){if(!state.packReady)return;accessJob?.cancel();val destination=state.destination;state=state.copy(accessBusy=true,accessPoints=emptyList(),accessFor=destination.name,accessError=null);accessJob=viewModelScope.launch{try{val found=withContext(Dispatchers.IO){localPack.nearbyAccess(destination.point)};if(state.destination==destination)state=state.copy(accessBusy=false,accessPoints=found)}catch(e:CancellationException){throw e}catch(e:Exception){state=state.copy(accessBusy=false,accessError="Mapped access could not be read. Try again.")}}}
 fun cancelRoute(){routeGeneration++;routeJob?.cancel();state=state.copy(routeBusy=false,message="Route calculation cancelled.")}
 fun query(value:String){searchGeneration++;searchJob?.cancel();val q=value.take(160);val token=searchGeneration;state=state.copy(query=q,results=localSearch(q),searchBusy=false,message=null);saved["query"]=q
  if(q.trim().length>=2&&state.packReady){state=state.copy(searchBusy=true);searchJob=viewModelScope.launch { delay(180);try{val found=withContext(Dispatchers.IO){localPack.search(q)};if(token==searchGeneration)state=state.copy(results=found,searchBusy=false,message=if(found.isEmpty())"No matching mapped places in this extract. Try another name or address." else null)}catch(e:CancellationException){throw e}catch(e:Exception){if(token==searchGeneration)state=state.copy(searchBusy=false,message="Offline search could not read this query. Try a shorter name.")}}}
 }
 fun onlineSearch(){query(state.query)}
 fun select(place:Place,origin:Boolean=false,accuracyMeters:Float?=null){clearApproachPreview();if(origin)state=state.copy(originAccuracyMeters=accuracyMeters?.takeIf{it.isFinite()&&it>0&&it<=5000});state=state.copy(approachReviewActive=false,destinationApproach=null,originApproach=null,roadChoices=emptyList());accessJob?.cancel();state=state.copy(accessPoints=emptyList(),accessBusy=false,accessError=null);routeGeneration++;routeJob?.cancel();state=if(origin)state.copy(origin=place,routes=emptyList(),routeBusy=false,fetchedAt=null,message=null,savedPreview=false)else state.copy(destination=place,routes=emptyList(),routeBusy=false,fetchedAt=null,message=null,savedPreview=false);val key=if(origin)"origin" else "destination";saved[key]=place.id;if(place.id!="current"){saved[key+"-name"]=place.name;saved[key+"-area"]=place.area;saved[key+"-lat"]=place.point.lat;saved[key+"-lon"]=place.point.lon}else for(suffix in listOf("-name","-area","-lat","-lon"))saved.remove<Any>(key+suffix)}
 private fun clearApproachPreview(){previewGeneration++;previewJob?.cancel();state=state.copy(previewChoice=null,approachPreview=null,approachPreviewBusy=false,approachPreviewError=null)}
 fun dismissRoadChoices(){routeGeneration++;routeJob?.cancel();clearApproachPreview();state=state.copy(approachReviewActive=false,routeBusy=false,roadChoices=emptyList())}
 fun previewRoadApproach(choice:RoadChoice){
  if(choice !in state.roadChoices)return
  clearApproachPreview();val token=previewGeneration;val place=if(state.reviewingOrigin)state.origin.point else state.destination.point
  val key=choice.geometry to place;val cached=previewCache[key]
  state=state.copy(previewChoice=choice,approachPreview=cached,approachPreviewBusy=cached==null)
  if(cached!=null)return
  previewJob=viewModelScope.launch{try{val preview=withContext(Dispatchers.IO){MazeLocalRouter(localPack.roads).preview(choice,place)};if(token==previewGeneration){previewCache[key]=preview;state=state.copy(approachPreview=preview,approachPreviewBusy=false)}}catch(e:CancellationException){throw e}catch(e:Exception){if(token==previewGeneration)state=state.copy(approachPreviewBusy=false,approachPreviewError="This road could not be highlighted. Try another option or review again.")}}
 }
 fun confirmRoadApproach(){val choice=state.previewChoice ?: return;if(state.approachPreviewBusy||state.approachPreview?.choice!=choice)return;chooseRoadApproach(choice)}
 private fun chooseRoadApproach(choice:RoadChoice){if(choice !in state.roadChoices)return;clearApproachPreview();state=state.copy(approachReviewActive=false,originApproach=if(state.reviewingOrigin)choice.geometry else state.originApproach,destinationApproach=if(state.reviewingOrigin)state.destinationApproach else choice.geometry,roadChoices=emptyList(),routes=emptyList(),fetchedAt=null,savedPreview=false);findRoutes()}
 fun reviewRoadApproach(origin:Boolean=false){if(state.routeBusy||!state.packReady)return
  if(origin&&state.origin.id=="current"&&needsManualRoadStart(state.originAccuracyMeters)){state=state.copy(message="The reported location uncertainty is wider than the nearby-road area. Choose a known start manually, or request a better fix.");return}
  clearApproachPreview();val destination=if(origin)state.origin else state.destination;val token=++routeGeneration;routeJob?.cancel();state=state.copy(approachReviewActive=true,routeBusy=true,message=null,roadChoices=emptyList(),reviewingOrigin=origin)
  routeJob=viewModelScope.launch{try{val choices=withContext(Dispatchers.IO){MazeLocalRouter(localPack.roads).approaches(destination.point)};if(token==routeGeneration){state=state.copy(approachReviewActive=choices.isNotEmpty(),routeBusy=false,roadChoices=choices,message=if(choices.isEmpty())"No supported road approach within 350 m." else null);choices.firstOrNull()?.let{previewRoadApproach(it)}}}catch(e:CancellationException){throw e}catch(e:Exception){if(token==routeGeneration)state=state.copy(approachReviewActive=false,routeBusy=false,message=e.message?.take(300) ?: "Road approaches could not be read.")}}
 }
 fun findRoutes(){if(state.routeBusy)return;if(state.originApproach==null){reviewRoadApproach(true);return};if(state.destinationApproach==null){reviewRoadApproach();return};val from=state.origin;val to=state.destination;val mode=state.routeSearchMode;val approach=state.destinationApproach;val startApproach=state.originApproach;val token=++routeGeneration;state=state.copy(routeBusy=true,message=null)
  routeJob=viewModelScope.launch{try{check(state.packReady){"Offline pack is still preparing."};val reply=withContext(Dispatchers.IO){MazeLocalRouter(localPack.roads,localPack.dataAt).route(from,to,mode,approach,startApproach)};if(token==routeGeneration)state=state.copy(routeBusy=false,routes=reply.routes,selectedRoute=0,fetchedAt=System.currentTimeMillis(),provider=reply.provider,dataAt=reply.dataAt,sample=reply.sample,savedPreview=false)}catch(e:CancellationException){throw e}catch(e:Exception){if(token==routeGeneration){val trip=state.savedTrips.firstOrNull{it.id==tripKey(from,to)};if(state.routes.isEmpty()&&trip!=null)openTrip(trip);state=state.copy(routeBusy=false,savedPreview=state.routes.isNotEmpty(),message=e.message?.take(300) ?: "Offline route could not be calculated.")}}}
 }
 fun chooseRoute(index:Int){if(index in state.routes.indices)state=state.copy(selectedRoute=index)}
 fun message(value:String){state=state.copy(message=value)}
 fun appearance(value:Appearance){preferences.edit().putString("appearance",value.name).apply();state=state.copy(appearance=value)}
 fun routeSearchMode(value:RouteSearchMode){preferences.edit().putString("route-search",value.name).apply();state=state.copy(routeSearchMode=value,message="Search effort changed. Refresh the route to apply it.")}
 fun saveTrip(){val route=state.routes.getOrNull(state.selectedRoute) ?: return;if(state.saving)return;val trip=SavedTrip(tripKey(state.origin,state.destination),state.origin,state.destination,route,System.currentTimeMillis(),state.provider,state.dataAt,state.sample);val trips=(listOf(trip)+state.savedTrips.filter{it.id!=trip.id}).take(4);state=state.copy(saving=true);viewModelScope.launch{try{withContext(Dispatchers.IO){store.write(trips)};state=state.copy(savedTrips=trips,saving=false,message="Trip saved on this device. Directions remain available offline.")}catch(e:Exception){state=state.copy(saving=false,message="Trip could not be saved. Check free storage and try again.")}}}
 fun openTrip(trip:SavedTrip){clearApproachPreview();state=state.copy(originAccuracyMeters=null);state=state.copy(approachReviewActive=false,destinationApproach=null,originApproach=null,roadChoices=emptyList());routeGeneration++;routeJob?.cancel();state=state.copy(origin=trip.origin,destination=trip.destination,routes=listOf(trip.route),selectedRoute=0,fetchedAt=trip.savedAt,provider=trip.provider,dataAt=trip.dataAt,sample=trip.sample,savedPreview=true,routeBusy=false,message="Saved preview. Road conditions may have changed.")}
 fun clearTrips(){viewModelScope.launch{try{withContext(Dispatchers.IO){store.clear()};state=state.copy(savedTrips=emptyList(),message="Saved trips removed from this device.")}catch(e:Exception){state=state.copy(message="Could not remove saved trips. Try again.")}}}
 override fun onCleared(){super.onCleared();connectivity.unregisterNetworkCallback(networkCallback);localPack.close()}
}
