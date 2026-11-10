package my.maze.malaysia

import android.database.sqlite.SQLiteDatabase
import android.os.SystemClock
import kotlinx.coroutines.ensureActive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

internal class SqliteApproachGraph(private val db:SQLiteDatabase,private val scanCheckpoint:()->Unit={}):ApproachGraph {
 private val positions=RouteCache<Long,GeoPoint>(8192)
 private val edges=RouteCache<Long,List<DirectedRoad>>(4096)
 private val turnRules=RouteCache<Pair<Long,Long>,List<TurnRule>>(8192)
 private val geometries=RouteCache<Long,RoadGeometry>(128)
 private fun road(c:android.database.Cursor)=DirectedRoad(c.getLong(0),c.getLong(1),c.getLong(2),c.getDouble(3),c.getDouble(4),c.getLong(5),c.getInt(6)!=0)
 override fun position(node:Long)=positions.getOrPut(node){db.rawQuery("SELECT lat,lon FROM nodes WHERE id=?",arrayOf(node.toString())).use{check(it.moveToFirst());GeoPoint(it.getDouble(0),it.getDouble(1))}}
 override fun outgoing(node:Long)=edges.getOrPut(node){db.rawQuery("SELECT e.src,e.dst,e.way,e.meters,e.seconds,e.geom,e.reverse,n.lat,n.lon FROM edges e JOIN nodes n ON n.id=e.dst WHERE e.src=?",arrayOf(node.toString())).use{c->buildList{while(c.moveToNext()){positions[c.getLong(1)]=GeoPoint(c.getDouble(7),c.getDouble(8));add(road(c))}}}}
 override fun rules(node:Long,incoming:Long)=turnRules.getOrPut(node to incoming){db.rawQuery("SELECT toway,onlyturn,kind FROM turns WHERE via=? AND fromway=?",arrayOf(node.toString(),incoming.toString())).use{c->buildList{while(c.moveToNext())add(TurnRule(c.getLong(0),c.getInt(1)!=0,c.getString(2)))}}}
 override fun geometry(id:Long)=geometries.getOrPut(id){
  val directions=db.rawQuery("SELECT src,dst,way,meters,seconds,geom,reverse FROM edges WHERE geom=?",arrayOf(id.toString())).use{c->buildList{while(c.moveToNext())add(road(c))}}
  db.rawQuery("SELECT points,name FROM geometry WHERE id=?",arrayOf(id.toString())).use{c->check(c.moveToFirst());val bytes=ByteBuffer.wrap(c.getBlob(0)).order(ByteOrder.LITTLE_ENDIAN);val points=buildList{while(bytes.remaining()>=8)add(GeoPoint(bytes.float.toDouble(),bytes.float.toDouble()))};RoadGeometry(id,points,c.getString(1),directions)}
 }
 override fun nearby(point:GeoPoint):Sequence<RoadGeometry> {
  val cells=buildList{for(lat in ((point.lat-.004+90)*100).toInt()..((point.lat+.004+90)*100).toInt())for(lon in ((point.lon-.004+180)*100).toInt()..((point.lon+.004+180)*100).toInt())add((lat.toLong()*36000+lon).toString())}
  data class Raw(val id:Long,val bytes:ByteArray,val name:String,val directions:MutableList<DirectedRoad>)
  val raw=mutableListOf<Raw>();var retainedBytes=0L
  // One joined scan avoids two SQLite round trips for every candidate shape.
  db.rawQuery("SELECT g.id,g.points,g.name,e.src,e.dst,e.way,e.meters,e.seconds,e.geom,e.reverse FROM (SELECT DISTINCT geom FROM segment_cells WHERE cell IN (${cells.joinToString(","){"?"}})) s JOIN geometry g ON g.id=s.geom JOIN edges e ON e.geom=g.id ORDER BY g.id,e.reverse",cells.toTypedArray()).use{c->
   while(c.moveToNext()){
    scanCheckpoint()
    val id=c.getLong(0)
    if(raw.lastOrNull()?.id!=id){
     val bytes=c.getBlob(1);retainedBytes+=bytes.size
     check(raw.size<8192&&retainedBytes<=16*1024*1024){"Too many nearby road segments. Choose a more specific public road point."}
     raw.add(Raw(id,bytes,c.getString(2),mutableListOf()))
    }
    raw.last().directions.add(DirectedRoad(c.getLong(3),c.getLong(4),c.getLong(5),c.getDouble(6),c.getDouble(7),c.getLong(8),c.getInt(9)!=0))
   }
  }
  return raw.asSequence().map{r->
   val buffer=ByteBuffer.wrap(r.bytes).order(ByteOrder.LITTLE_ENDIAN)
   RoadGeometry(r.id,buildList{while(buffer.remaining()>=8)add(GeoPoint(buffer.float.toDouble(),buffer.float.toDouble()))},r.name,r.directions).also{geometries[r.id]=it}
  }
 }
}
internal class MazeLocalRouter(private val db:SQLiteDatabase,private val dataAt:Long?=null) {
 suspend fun approaches(point:GeoPoint):List<RoadChoice>{
  val context=coroutineContext;val deadline=SystemClock.elapsedRealtime()+15000
  val check={context.ensureActive();if(SystemClock.elapsedRealtime()>=deadline)throw SearchBudgetExceeded()}
  return roadChoices(SqliteApproachGraph(db,check),point,check)
 }
 suspend fun route(origin:Place,destination:Place,mode:RouteSearchMode=RouteSearchMode.QUICK,destinationApproach:Long?=null,originApproach:Long?=null):RouteReply {
  val context=coroutineContext;val began=SystemClock.elapsedRealtime()
  val deadline=began+if(groundDistance(origin.point,destination.point)>50000)45000 else 15000
  val graph=SqliteApproachGraph(db){context.ensureActive();if(SystemClock.elapsedRealtime()>=deadline)throw SearchBudgetExceeded()}
  val found=routeApproaches(graph,origin.point,destination.point,mode,{SystemClock.elapsedRealtime()},{context.ensureActive()},destinationApproach,originApproach)
  val route=approachRoadRoute(graph,found,mode)
  android.util.Log.i("MazeOffline","route states=${found.expanded} elapsedMs=${SystemClock.elapsedRealtime()-began} meters=${route.meters.toInt()}")
  return RouteReply(listOf(route),"On-device OSM · directed segment approaches · ${mode.label.lowercase()} · not guaranteed fastest",dataAt,false)
 }
}
