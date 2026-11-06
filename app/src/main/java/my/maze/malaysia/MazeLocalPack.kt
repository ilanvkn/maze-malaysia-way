package my.maze.malaysia

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.security.MessageDigest
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal fun isRegionalPackFile(name:String,partialOnly:Boolean=false)=Regex("[a-f0-9]{64}-(places[.]sqlite|roads[.]sqlite|region[.]mbtiles)"+if(partialOnly)"[.]part" else "([.]part)?").matches(name)

/** Public map data only. Loopback listener never exposes files or accepts arbitrary SQL. */
class MazeLocalPack(private val context:Context):AutoCloseable {
 lateinit var places:SQLiteDatabase;private set
 lateinit var roads:SQLiteDatabase;private set
 private lateinit var tiles:SQLiteDatabase
 private var listener:ServerSocket?=null
 private val workers=Executors.newFixedThreadPool(4)
 private val slots=Semaphore(4)
 private val closed=java.util.concurrent.atomic.AtomicBoolean(false)
 private val token=UUID.randomUUID().toString()
 var base="";private set
 var summary="";private set
 var dataAt:Long?=null;private set
 suspend fun open(progress:suspend(Long,Long)->Unit={_,_->}) {
  val manifest=JSONObject(context.assets.open("offline/manifest.json").bufferedReader().use{it.readText()})
  val files=manifest.getJSONObject("files");val dir=File(context.noBackupFilesDir,"regional-pack").apply{mkdirs()}
  dir.listFiles()?.filter{isRegionalPackFile(it.name,true)}?.forEach{it.delete()}
  val total=files.keys().asSequence().sumOf{files.getJSONObject(it).getLong("bytes")};var completed=0L
  suspend fun install(name:String):File {
   val m=files.getJSONObject(name);val hash=m.getString("sha256");val file=File(dir,hash+"-"+name)
   check(file.exists()&&file.length()==m.getLong("bytes")){"Import the Malaysia offline pack to start maps, search and routing."}
   completed+=file.length();progress(completed,total)
   return file
  }
  coroutineContext.ensureActive()
  places=SQLiteDatabase.openDatabase(install("places.sqlite").path,null,SQLiteDatabase.OPEN_READONLY)
  roads=SQLiteDatabase.openDatabase(install("roads.sqlite").path,null,SQLiteDatabase.OPEN_READONLY)
  tiles=SQLiteDatabase.openDatabase(install("region.mbtiles").path,null,SQLiteDatabase.OPEN_READONLY)
  summary=manifest.getString("summary");dataAt=runCatching{java.time.Instant.parse(manifest.getString("osm_timestamp")).toEpochMilli()}.getOrNull()
  coroutineContext.ensureActive()
  val active=files.keys().asSequence().map{files.getJSONObject(it).getString("sha256")+"-"+it}.toSet()
  dir.listFiles()?.filter{isRegionalPackFile(it.name)&&it.name !in active}?.forEach{it.delete()}
  val server=ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));listener=server;base="http://127.0.0.1:${server.localPort}/$token"
  Thread({while(!server.isClosed){val socket=try{server.accept()}catch(e:Exception){break};if(!slots.tryAcquire()){socket.close();continue};workers.execute{try{socket.soTimeout=3000;socket.use{s->val input=s.getInputStream();val header=java.io.ByteArrayOutputStream();var tail=0;while(header.size()<4096){val next=input.read();if(next<0)return@use;header.write(next);tail=(tail shl 8) or next;if(tail==0x0d0a0d0a)break};if(tail!=0x0d0a0d0a)return@use;val request=header.toString("US-ASCII").substringBefore("\r\n");if(!request.startsWith("GET "))return@use;val path=request.split(' ').getOrNull(1) ?: "";val payload=serve(path);val out=s.getOutputStream();out.write(("HTTP/1.1 ${if(payload==null)"404 Not Found" else "200 OK"}\r\nContent-Type: ${payload?.second ?: "text/plain"}\r\nContent-Length: ${payload?.first?.size ?: 0}\r\nConnection: close\r\n"+(if(payload?.third==true)"Content-Encoding: gzip\r\n" else "")+"\r\n").toByteArray());payload?.let{out.write(it.first)}}}catch(_:Exception){}finally{slots.release()}}}},"maze-public-map-loopback").apply{isDaemon=true;start()}
 }
 private fun serve(path:String):Triple<ByteArray,String,Boolean>? {
  val prefix="/$token/";if(!path.startsWith(prefix))return null;val relative=path.removePrefix(prefix)
  if(relative=="day.json" || relative=="night.json")return Triple(style(relative=="night.json").toByteArray(),"application/json",false)
  val glyph=Regex("glyphs/[^/]+/(\\d+-\\d+)\\.pbf").matchEntire(relative)
  if(glyph!=null)return runCatching{Triple(context.assets.open("offline/glyphs/${glyph.groupValues[1]}.pbf").use{it.readBytes()},"application/x-protobuf",false)}.getOrNull()
  val match=Regex("tiles/(\\d{1,2})/(\\d{1,5})/(\\d{1,5})\\.pbf").matchEntire(relative) ?: return null
  val (z,x,y)=match.destructured.toList().map{it.toInt()};if(z !in 0..14 || x !in 0 until (1 shl z) || y !in 0 until (1 shl z))return null
  return tiles.rawQuery("SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?",arrayOf(z.toString(),x.toString(),((1 shl z)-1-y).toString())).use{c->if(c.moveToFirst()){val b=c.getBlob(0);Triple(b,"application/x-protobuf",b.size>2&&b[0]==31.toByte()&&b[1]==(-117).toByte())}else null}
 }
 private fun style(night:Boolean):String {
  val bg=if(night)"#172824" else "#F3F1E8";val land=if(night)"#203D32" else "#E1E9D5";val water=if(night)"#183D4D" else "#B7D9E3";val road=if(night)"#9BAF9C" else "#FFFFFF";val ink=if(night)"#DCE8DB" else "#30433A"
  fun layer(id:String,type:String,color:String,width:Double=1.0)=JSONObject().put("id",id).put("type",type).put("source","region").put("source-layer",id).put("paint",JSONObject().put("$type-color",color).apply{if(type=="line")put("line-width",width)})
  val layers=org.json.JSONArray().put(JSONObject().put("id","background").put("type","background").put("paint",JSONObject().put("background-color",bg)))
  layers.put(layer("land","fill",land));layers.put(layer("sites","fill",if(night)"#263830" else "#E9E4D6"));layers.put(layer("ocean","fill",water));layers.put(layer("water_polygons","fill",water));layers.put(layer("water_lines","line",water,1.5));layers.put(layer("buildings","fill",if(night)"#3A4D41" else "#DAD6C7"));layers.put(layer("streets","line",road,2.5).put("filter",org.json.JSONArray(listOf("all",org.json.JSONArray(listOf("!=","rail",true)),org.json.JSONArray(listOf("!in","kind","path","footway","steps","pedestrian","cycleway"))))).put("paint",JSONObject().put("line-color",org.json.JSONArray(listOf("match",org.json.JSONArray(listOf("get","kind")),"motorway",if(night)"#D6B770" else "#E6B75D","trunk",if(night)"#BEBC8B" else "#EDCD8B",road))).put("line-width",org.json.JSONArray(listOf("interpolate",org.json.JSONArray(listOf("linear")),org.json.JSONArray(listOf("zoom")),5,0.3,10,0.5,14,org.json.JSONArray(listOf("match",org.json.JSONArray(listOf("get","kind")),"motorway",2.8,"trunk",2.4,"primary",2.0,"secondary",1.6,"tertiary",1.2,"residential",0.8,"service",0.4,0.5)),18,5.0)))));layers.put(layer("rail-lines","line",if(night)"#7F9789" else "#9A9F91",0.7).put("source-layer","streets").put("filter",org.json.JSONArray(listOf("==","rail",true))).put("paint",JSONObject().put("line-color",if(night)"#7F9789" else "#9A9F91").put("line-width",0.7).put("line-dasharray",org.json.JSONArray(listOf(2,2)))))
  layers.put(layer("footpaths","line",if(night)"#A3B5A5" else "#A9B39A",0.7).put("source-layer","streets").put("minzoom",15).put("filter",org.json.JSONArray(listOf("in","kind","path","footway","steps","pedestrian","cycleway"))).put("paint",JSONObject().put("line-color",if(night)"#A3B5A5" else "#A9B39A").put("line-width",0.7).put("line-dasharray",org.json.JSONArray(listOf(1,2)))))
  layers.put(layer("boundaries","line",if(night)"#6B8479" else "#B6B7A2",0.7))
  for(id in listOf("place_labels","street_labels","pois"))layers.put(JSONObject().put("id",id).put("type","symbol").put("source","region").put("source-layer",id).put("minzoom",if(id=="pois")14 else if(id=="street_labels")12 else 0).put("layout",JSONObject().put("text-field",org.json.JSONArray(listOf("get","name"))).put("text-font",org.json.JSONArray(listOf("Noto Sans Regular"))).put("text-size",if(id=="place_labels")13 else 11).apply{if(id=="street_labels")put("symbol-placement","line")}).put("paint",JSONObject().put("text-color",ink).put("text-halo-color",bg).put("text-halo-width",1)))
  return JSONObject().put("version",8).put("glyphs","$base/glyphs/{fontstack}/{range}.pbf").put("sources",JSONObject().put("region",JSONObject().put("type","vector").put("tiles",org.json.JSONArray(listOf("$base/tiles/{z}/{x}/{y}.pbf"))).put("minzoom",0).put("maxzoom",14).put("attribution","© OpenStreetMap contributors · ODbL 1.0 · Geofabrik Shortbread"))).put("layers",layers).toString()
 }
 private fun row(c:android.database.Cursor):Place {
  val tags=JSONObject(c.getString(5));val extra=listOf("entrance","wheelchair","level","opening_hours","ref","access").mapNotNull{k->tags.optString(k).takeIf{it.isNotBlank()}?.let{" · "+when(k){"entrance"->"Entrance: ";"wheelchair"->"Wheelchair: ";"level"->"Level: ";"opening_hours"->"Mapped hours: ";"access"->"Mapped access: ";else->"Reference: "}+it}}.joinToString("")
  return Place("osm-"+c.getString(0),c.getString(1),c.getString(2)+extra,GeoPoint(c.getDouble(3),c.getDouble(4)))
 }
 fun nearbyAccess(point:GeoPoint):List<Place> {
  val delta=.003;val lonDelta=delta/kotlin.math.cos(Math.toRadians(point.lat))
  return places.rawQuery("SELECT id,name,detail,lat,lon,tags FROM places WHERE (kind IN ('entrance','motorway exit') OR tags LIKE ?) AND lat BETWEEN ? AND ? AND lon BETWEEN ? AND ? LIMIT 1000",arrayOf("%\"entrance\":%",(point.lat-delta).toString(),(point.lat+delta).toString(),(point.lon-lonDelta).toString(),(point.lon+lonDelta).toString())).use{c->buildList{while(c.moveToNext()){if(JSONObject(c.getString(5)).optString("entrance")=="no")continue;val place=row(c);val distance=groundDistance(point,place.point);if(distance<=300)add(place to distance)}}}.sortedBy{it.second}.take(30).map{(place,distance)->place.copy(name=accessDisplayName(place.name,place.area),area="${kotlin.math.round(distance).toInt()} m from selected point · "+place.area)}
 }
 fun search(query:String):List<Place> {
  val tokens=Regex("[\\p{L}\\p{N}]+").findAll(query.take(160)).map{it.value}.take(8).toList();if(tokens.isEmpty())return emptyList()
  val expression=tokens.joinToString(" "){"\"$it*\""}
  return places.rawQuery("SELECT p.id,p.name,p.detail,p.lat,p.lon,p.tags FROM search JOIN places p ON p.rowid=search.docid WHERE search MATCH ? ORDER BY CASE WHEN p.name=? COLLATE NOCASE THEN 0 WHEN p.name LIKE ? THEN 1 ELSE 2 END,CASE p.kind WHEN 'shop' THEN 0 WHEN 'place' THEN 1 ELSE 2 END,p.name LIMIT 20",arrayOf(expression,query.trim(),query.trim()+"%")).use{c->buildList{while(c.moveToNext())add(row(c))}}
 }
 override fun close(){if(!closed.compareAndSet(false,true))return;listener?.close();workers.shutdownNow();if(::places.isInitialized)places.close();if(::roads.isInitialized)roads.close();if(::tiles.isInitialized)tiles.close()}
}
