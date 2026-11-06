package my.maze.malaysia

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import android.content.Context
import android.util.AtomicFile
import java.io.File

data class SavedTrip(val id:String,val origin:Place,val destination:Place,val route:RoadRoute,val savedAt:Long,val provider:String,val dataAt:Long?,val sample:Boolean)
fun tripKey(origin:Place,destination:Place)=listOf(origin.point.lat,origin.point.lon,destination.point.lat,destination.point.lon).joinToString("|")
private fun digest(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString(""){"%02x".format(it)}
private fun placeJson(p:Place)=JSONObject().put("id",p.id).put("name",p.name).put("area",p.area).put("lat",p.point.lat).put("lon",p.point.lon)
private fun readPlace(r:JSONObject)=Place(r.getString("id").take(150),r.getString("name").take(150),r.getString("area").take(300),GeoPoint(r.getDouble("lat"),r.getDouble("lon")))
object TripCodec {
    const val MAX_BYTES=8*1024*1024
    fun encode(trips:List<SavedTrip>):String {
        require(trips.size<=4&&trips.map{it.id}.distinct().size==trips.size&&trips.all{it.id==tripKey(it.origin,it.destination)})
        val rows=JSONArray()
        for(t in trips){require(t.savedAt>0&&t.route.meters>0&&t.route.seconds>0&&t.route.geometry.size in 2..50000)
            val points=JSONArray();t.route.geometry.forEach{points.put(JSONArray().put(it.lon).put(it.lat))}
            val steps=JSONArray();t.route.steps.forEach{steps.put(JSONObject().put("text",it.text).put("meters",it.meters))}
            rows.put(JSONObject().put("id",t.id).put("origin",placeJson(t.origin)).put("destination",placeJson(t.destination)).put("savedAt",t.savedAt).put("provider",t.provider).put("sample",t.sample).put("dataAt",t.dataAt ?: JSONObject.NULL).put("route",JSONObject().put("points",points).put("steps",steps).put("meters",t.route.meters).put("seconds",t.route.seconds).put("summary",t.route.summary)))
        }
        val payload=rows.toString();val text=JSONObject().put("schema",1).put("sha256",digest(payload)).put("payload",payload).toString();require(text.toByteArray().size<=MAX_BYTES);return text
    }
    fun decode(text:String):List<SavedTrip>{
        require(text.toByteArray().size<=MAX_BYTES);val envelope=JSONObject(text);require(envelope.getInt("schema")==1);val payload=envelope.getString("payload");require(digest(payload)==envelope.getString("sha256"));val rows=JSONArray(payload);require(rows.length()<=4)
        val trips=(0 until rows.length()).map { i -> val t=rows.getJSONObject(i);val r=t.getJSONObject("route");val points=r.getJSONArray("points");require(points.length() in 2..50000);val geometry=(0 until points.length()).map{p->val pair=points.getJSONArray(p);GeoPoint(pair.getDouble(1),pair.getDouble(0))};val meters=r.getDouble("meters");val seconds=r.getDouble("seconds");require(meters.isFinite()&&seconds.isFinite()&&meters>0&&seconds>0);val stepRows=r.getJSONArray("steps");require(stepRows.length()<=500);val steps=(0 until stepRows.length()).map{s->val v=stepRows.getJSONObject(s);val distance=v.getDouble("meters");require(distance.isFinite()&&distance>=0);RoadStep(v.getString("text").take(300),distance)};val saved=t.getLong("savedAt");require(saved>0);SavedTrip(t.getString("id"),readPlace(t.getJSONObject("origin")),readPlace(t.getJSONObject("destination")),RoadRoute(geometry,steps,meters,seconds,r.getString("summary").take(500)),saved,t.getString("provider").take(150),if(t.isNull("dataAt"))null else t.getLong("dataAt"),t.getBoolean("sample"))}
        require(trips.map{it.id}.distinct().size==trips.size&&trips.all{it.id==tripKey(it.origin,it.destination)});return trips
    }
}
class TripStore(context:Context) {
    private val file=AtomicFile(File(context.filesDir,"saved-trips-v1.json"))
    fun read():List<SavedTrip>{if(!file.baseFile.exists()&&!File(file.baseFile.path+".bak").exists())return emptyList();return file.openRead().use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;out.write(buffer,0,n);require(out.size()<=TripCodec.MAX_BYTES)};TripCodec.decode(out.toString("UTF-8"))}}
    fun write(trips:List<SavedTrip>){val bytes=TripCodec.encode(trips).toByteArray();val stream=file.startWrite();try{stream.write(bytes);file.finishWrite(stream)}catch(e:Exception){file.failWrite(stream);throw e}}
    fun clear(){file.delete()}
}
