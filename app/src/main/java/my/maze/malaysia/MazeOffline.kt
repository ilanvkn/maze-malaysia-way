package my.maze.malaysia

import android.content.Context
import org.maplibre.android.offline.*
import org.maplibre.android.geometry.LatLngBounds
import org.json.JSONObject

data class OfflineAreaState(val busy:Boolean=false,val ready:Boolean=false,val label:String="No saved map area",val bytes:Long=0,val style:String?=null)
const val DAY_STYLE="https://tiles.openfreemap.org/styles/positron"
const val NIGHT_STYLE="https://tiles.openfreemap.org/styles/dark"
class MazeOffline(context:Context,private val update:(OfflineAreaState)->Unit) {
    private val manager=OfflineManager.getInstance(context)
    private val pixelRatio=context.resources.displayMetrics.density
    private var region:OfflineRegion?=null
    private var current=OfflineAreaState()
    private var closed=false
    init {manager.setOfflineMapboxTileCountLimit(3000);reload()}
    private fun emit(value:OfflineAreaState){current=value;if(!closed)update(value)}
    fun reload(){manager.listOfflineRegions(object:OfflineManager.ListOfflineRegionsCallback{
        override fun onList(regions:Array<OfflineRegion>?){region=regions?.firstOrNull();val r=region ?: run{emit(OfflineAreaState());return};r.getStatus(object:OfflineRegion.OfflineRegionStatusCallback{override fun onStatus(status:OfflineRegionStatus?){if(status==null){emit(OfflineAreaState(label="Saved map status unavailable"));return};val name=runCatching{JSONObject(String(r.metadata)).optString("name","Saved area")}.getOrDefault("Saved area");val style=(r.definition as? OfflineTilePyramidRegionDefinition)?.styleURL;emit(OfflineAreaState(ready=status.isComplete,label=if(status.isComplete)name else "Incomplete area — remove and retry",bytes=status.completedResourceSize,style=style))};override fun onError(error:String?){emit(OfflineAreaState(label="Saved map could not be checked"))}})}
        override fun onError(error:String){emit(OfflineAreaState(label="Saved map unavailable"))}
    })}
    fun download(place:Place,night:Boolean){if(current.busy)return;if(region!=null){emit(current.copy(label="Remove the existing map before saving another area"));return}
        val p=place.point;val bounds=LatLngBounds.from((p.lat+.045).coerceAtMost(84.9),(p.lon+.045).coerceAtMost(179.9),(p.lat-.045).coerceAtLeast(-84.9),(p.lon-.045).coerceAtLeast(-179.9));val style=if(night)NIGHT_STYLE else DAY_STYLE
        val definition=OfflineTilePyramidRegionDefinition(style,bounds,11.0,14.0,pixelRatio)
        val name="Around "+place.name+if(night)" · Night" else " · Day"
        emit(OfflineAreaState(busy=true,label="Preparing a small map area…",style=style))
        manager.createOfflineRegion(definition,JSONObject().put("name",name).put("createdAt",System.currentTimeMillis()).toString().toByteArray(),object:OfflineManager.CreateOfflineRegionCallback{
            override fun onCreate(created:OfflineRegion){region=created;created.setObserver(object:OfflineRegion.OfflineRegionObserver{
                override fun onStatusChanged(status:OfflineRegionStatus){if(closed)return
                    if(status.completedResourceSize>50_000_000||status.completedTileCount>3000){created.setDownloadState(OfflineRegion.STATE_INACTIVE);emit(OfflineAreaState(label="Area exceeded the download budget. Remove it and try a smaller area.",bytes=status.completedResourceSize,style=style));return}
                    if(status.isComplete){created.setDownloadState(OfflineRegion.STATE_INACTIVE);emit(OfflineAreaState(ready=true,label=name,bytes=status.completedResourceSize,style=style));return}
                    val total=status.requiredResourceCount;val percent=if(total>0)(status.completedResourceCount*100/total).coerceAtMost(99)else 0
                    emit(OfflineAreaState(busy=true,label="Saving map · "+percent+"%",bytes=status.completedResourceSize,style=style))
                }
                override fun onError(error:OfflineRegionError){created.setDownloadState(OfflineRegion.STATE_INACTIVE);emit(OfflineAreaState(label="Map download paused. Remove it and retry when connected.",style=style))}
                override fun mapboxTileCountLimitExceeded(limit:Long){created.setDownloadState(OfflineRegion.STATE_INACTIVE);emit(OfflineAreaState(label="Map tile limit reached. Remove this incomplete area.",style=style))}
            });created.setDownloadState(OfflineRegion.STATE_ACTIVE)}
            override fun onError(error:String){emit(OfflineAreaState(label="Could not start map download. Try again later."))}
        })
    }
    fun remove(){val r=region ?: return;r.setDownloadState(OfflineRegion.STATE_INACTIVE);r.delete(object:OfflineRegion.OfflineRegionDeleteCallback{override fun onDelete(){region=null;emit(OfflineAreaState())};override fun onError(error:String){emit(current.copy(busy=false,label="Could not remove the map. Try again."))}})}
    fun close(){closed=true;region?.setDownloadState(OfflineRegion.STATE_INACTIVE)}
}
