package my.maze.malaysia
import android.app.Instrumentation
import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
/** Explicit emulator-only proof controller; never auto-runs at application startup. */
class OfflineProof:Instrumentation() {
 private var mode=RouteSearchMode.QUICK
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);mode=if(arguments?.getString("routeMode")=="CAREFUL")RouteSearchMode.CAREFUL else RouteSearchMode.QUICK;start()}
 override fun onStart(){Thread {
  val report=JSONObject().put("route_mode",mode.name);val pack=MazeLocalPack(targetContext)
  try {check(android.os.Build.FINGERPRINT.contains("generic")||android.os.Build.MODEL.contains("sdk"));check(Settings.Global.getInt(targetContext.contentResolver,Settings.Global.AIRPLANE_MODE_ON,0)==1){"Airplane mode required"}
   val cm=targetContext.getSystemService(android.net.ConnectivityManager::class.java);check(cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)!=true){"Validated external network must be absent"}
   runBlocking {pack.open();report.put("pack",pack.summary)
    val diagnostics=JSONObject();pack.places.rawQuery("SELECT sqlite_version()",null).use{it.moveToFirst();diagnostics.put("sqlite",it.getString(0))};pack.places.rawQuery("SELECT count(*) FROM places",null).use{it.moveToFirst();diagnostics.put("places",it.getLong(0))};for(exp in listOf("7-Eleven","\"7*\" AND \"Eleven*\"","\"7*\" \"Eleven*\"")){pack.places.rawQuery("SELECT count(*) FROM search WHERE search MATCH ?",arrayOf(exp)).use{it.moveToFirst();diagnostics.put(exp,it.getLong(0))}};report.put("diagnostics",diagnostics)
    val searches=JSONArray();report.put("search",searches);for(q in listOf("7-Eleven","entrance","motorway exit","Kuching","Kota Kinabalu","George Town","Shah Alam","noSuchPlace934982")){val t=SystemClock.elapsedRealtime();val found=pack.search(q);if(q=="noSuchPlace934982")check(found.isEmpty())else check(found.isNotEmpty()){q};searches.put(JSONObject().put("query",q).put("count",found.size).put("elapsed_ms",SystemClock.elapsedRealtime()-t).put("first",found.firstOrNull()?.name ?: ""))};report.put("search",searches)
    val access=JSONArray();for(place in listOf(landmarks[1],landmarks[0],Place("viva","Vivacity Megamall","Kuching",GeoPoint(1.52674275,110.3692989)))){val found=pack.nearbyAccess(place.point);check(found.all{groundDistance(place.point,it.point)<=300});if(place.id=="viva")check(found.isEmpty())else check(found.isNotEmpty());access.put(JSONObject().put("place",place.name).put("count",found.size).put("first",found.firstOrNull()?.area ?: "No mapped points"))};report.put("nearby_access",access)
    val styles=JSONArray();for(mode in listOf("day","night")){val style=JSONObject(java.net.URL(pack.base+"/$mode.json").readText());check(style.getJSONObject("sources").getJSONObject("region").getJSONArray("tiles").getString(0).startsWith(pack.base));check(style.getString("glyphs").startsWith(pack.base));styles.put(mode)};report.put("styles",styles)
    val tiles=JSONArray();for(place in listOf(landmarks[0],landmarks[5],landmarks[6],landmarks[7])){val z=12;val x=((place.point.lon+180)/360*(1 shl z)).toInt();val lat=Math.toRadians(place.point.lat);val y=((1-kotlin.math.ln(kotlin.math.tan(lat)+1/kotlin.math.cos(lat))/Math.PI)/2*(1 shl z)).toInt();val b=java.net.URL(pack.base+"/tiles/$z/$x/$y.pbf").readBytes();check(b.size>100);tiles.put(JSONObject().put("place",place.name).put("bytes",b.size))};report.put("tiles",tiles)
    check(java.net.URL(pack.base+"/glyphs/Noto%20Sans%20Regular/0-255.pbf").readBytes().size>1000)
    val routes=JSONArray();report.put("routes",routes);val router=MazeLocalRouter(pack.roads)
    for(pair in listOf(landmarks[0] to landmarks[1],landmarks[2] to landmarks[0],landmarks[5] to Place("gurney","Gurney Paragon","Penang",GeoPoint(5.4398311,100.3091867)),landmarks[6] to Place("viva","Vivacity Megamall","Kuching",GeoPoint(1.52674275,110.3692989)),landmarks[7] to Place("suria","Suria Sabah","Sabah",GeoPoint(5.98761945,116.07716655)),landmarks[0] to landmarks[5],landmarks[0] to pack.search("Shah Alam").first())){val t=SystemClock.elapsedRealtime();val route=router.route(pair.first,pair.second,mode).routes.single();check(route.geometry.size>2&&route.meters>100);routes.put(JSONObject().put("from",pair.first.name).put("to",pair.second.name).put("meters",route.meters).put("elapsed_ms",SystemClock.elapsedRealtime()-t).put("vertices",route.geometry.size).put("steps",route.steps.size))};report.put("routes",routes)
   };report.put("passed",true)
  } catch(e:Throwable){report.put("passed",false).put("error",e.stackTraceToString())}finally{pack.close()}
  val result=Bundle();result.putString("stream",report.toString(2));finish(if(report.optBoolean("passed"))Activity.RESULT_OK else Activity.RESULT_CANCELED,result)
 }.start()}
}
