package my.maze.malaysia

import okhttp3.*
import okhttp3.HttpUrl
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.json.JSONObject

data class RouteReply(val routes:List<RoadRoute>,val provider:String,val dataAt:Long?,val sample:Boolean)
class MazeRepository(base:String=BuildConfig.GATEWAY_BASE,private val lab:Boolean=BuildConfig.IS_LAB,private val pause:suspend(Long)->Unit={delay(it)}) {
    private val baseUrl:HttpUrl?=runCatching{validateGateway(base,lab)}.getOrNull()
    private val client=OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(8,TimeUnit.SECONDS).callTimeout(9,TimeUnit.SECONDS).followRedirects(false).retryOnConnectionFailure(false).build()
    private val searchGate=ServiceGate();private val routeGate=ServiceGate()
    private val searchCache=mutableMapOf<String,List<Place>>()
    val configured:Boolean get()=baseUrl!=null
    private suspend fun fetch(url:HttpUrl):String=suspendCancellableCoroutine { continuation ->
        val call=client.newCall(Request.Builder().url(url).header("User-Agent","MazeMalaysiaWay/0.2 Android").header("Accept","application/json").build())
        continuation.invokeOnCancellation{call.cancel()}
        call.enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException){if(continuation.isActive)continuation.resumeWithException(if(e is SocketTimeoutException)ServiceProblem(ProblemKind.TIMEOUT) else ServiceProblem(ProblemKind.OFFLINE))}
            override fun onResponse(call:Call,response:Response){
                try{val text=response.use { r ->
                    when { r.code==429->throw ServiceProblem(ProblemKind.RATE_LIMITED,r.header("Retry-After")?.toIntOrNull()?.coerceIn(1,300) ?: 30)
                        r.code>=500->throw ServiceProblem(ProblemKind.UNAVAILABLE)
                        !r.isSuccessful->throw ServiceProblem(ProblemKind.INVALID_DATA) }
                    val body=r.body ?: throw ServiceProblem(ProblemKind.INVALID_DATA)
                    body.byteStream().use { stream -> val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=stream.read(buffer);if(n<0)break;out.write(buffer,0,n);if(out.size()>8*1024*1024)throw ServiceProblem(ProblemKind.INVALID_DATA)};out.toString("UTF-8") }
                };if(continuation.isActive)continuation.resume(text)}catch(e:Exception){if(continuation.isActive)continuation.resumeWithException(e)}
            }
        })
    }
    private suspend fun request(path:String,parameters:Map<String,String>,gate:ServiceGate):JSONObject {
        val root=baseUrl ?: throw ServiceProblem(ProblemKind.NOT_CONFIGURED)
        val url=root.newBuilder().addPathSegments("v1/"+path).apply{parameters.forEach{(k,v)->addQueryParameter(k,v)}}.build()
        for(attempt in 0..1){gate.permit();try{val text=fetch(url);val parsed=withContext(Dispatchers.Default){runCatching{JSONObject(text)}.getOrElse{throw ServiceProblem(ProblemKind.INVALID_DATA)}};val mode=parsed.optJSONObject("maze")?.optString("mode") ?: "";if(mode !in listOf("gateway","fixture")||(!lab&&mode=="fixture"))throw ServiceProblem(ProblemKind.INVALID_DATA);gate.success();return parsed}catch(e:Exception){if(e is kotlinx.coroutines.CancellationException)throw e;val p=e as? ServiceProblem ?: ServiceProblem(ProblemKind.INVALID_DATA);if(p.kind==ProblemKind.RATE_LIMITED){gate.limited(p.waitSeconds);throw p};gate.failure();if(attempt==1||p.kind !in listOf(ProblemKind.UNAVAILABLE,ProblemKind.TIMEOUT,ProblemKind.OFFLINE))throw p;pause(400)}}
        throw ServiceProblem(ProblemKind.UNAVAILABLE)
    }
    suspend fun search(query:String):List<Place> {
        val key=query.trim().lowercase(java.util.Locale.ROOT);require(key.length in 2..160)
        searchCache[key]?.let{return it}
        val root=request("search",mapOf("q" to key,"country" to "my","limit" to "5"),searchGate)
        val places=withContext(Dispatchers.Default){runCatching{parsePlaces(root.getJSONArray("places").toString())}.getOrElse{throw ServiceProblem(ProblemKind.INVALID_DATA)}}
        if(searchCache.size>=30)searchCache.clear();searchCache[key]=places;return places
    }
    suspend fun routes(origin:Place,destination:Place):RouteReply {
        if(origin.point==destination.point)throw ServiceProblem(ProblemKind.INVALID_DATA)
        val root=request("routes",mapOf("from" to (origin.point.lon.toString()+","+origin.point.lat),"to" to (destination.point.lon.toString()+","+destination.point.lat)),routeGate)
        val routes=withContext(Dispatchers.Default){runCatching{parseRoutes(root.toString())}.getOrElse{throw ServiceProblem(ProblemKind.INVALID_DATA)}}
        val metadata=root.getJSONObject("maze")
        return RouteReply(routes,metadata.optString("provider","Configured road service").take(120),metadata.optLong("recorded_at_ms",0).takeIf{it>0},metadata.optString("mode")=="fixture")
    }
}
