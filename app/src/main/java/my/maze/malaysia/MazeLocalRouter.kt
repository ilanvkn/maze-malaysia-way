package my.maze.malaysia

import android.database.sqlite.SQLiteDatabase
import android.os.SystemClock
import kotlinx.coroutines.ensureActive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.PriorityQueue
import kotlin.coroutines.coroutineContext
import kotlin.math.*

internal fun groundDistance(a:GeoPoint,b:GeoPoint):Double {
 val p=Math.toRadians(a.lat);val q=Math.toRadians(b.lat);val d=Math.toRadians(b.lon-a.lon)
 return 12742000*asin(sqrt((sin((q-p)/2).pow(2)+cos(p)*cos(q)*sin(d/2).pow(2)).coerceIn(0.0,1.0)))
}
internal data class TurnRule(val toWay:Long,val only:Boolean,val kind:String="")
internal fun turnAllowed(outgoingWay:Long,rules:List<TurnRule>,incomingWay:Long=0,reversing:Boolean=false):Boolean {
 fun matches(rule:TurnRule):Boolean {if(rule.toWay!=outgoingWay)return false;if(rule.toWay!=incomingWay)return true;return when(rule.kind){"no_u_turn","only_u_turn"->reversing;"no_straight_on","only_straight_on"->!reversing;else->true}}
 return rules.none{it.toWay == -1L}&&rules.none{!it.only&&matches(it)}&&(rules.none{it.only}||rules.any{it.only&&matches(it)})
}
private data class RoadSnap(val node:Long,val meters:Double,val component:Long)
private data class RoadKey(val node:Long,val incomingWay:Long,val previousNode:Long=0)
private data class RoadEdge(val destination:Long,val way:Long,val meters:Double,val seconds:Double,val geometry:Long,val reverse:Boolean)
private data class Frontier(val key:RoadKey,val cost:Double,val score:Double)
private data class Previous(val key:RoadKey,val edge:RoadEdge)
class MazeLocalRouter(private val db:SQLiteDatabase,private val dataAt:Long?=null) {
 private fun node(id:Long)=db.rawQuery("SELECT lat,lon FROM nodes WHERE id=?",arrayOf(id.toString())).use{check(it.moveToFirst());GeoPoint(it.getDouble(0),it.getDouble(1))}
 private fun snaps(point:GeoPoint,origin:Boolean):List<RoadSnap> {
  val candidates=mutableListOf<RoadSnap>()
  val cells=buildList {for(lat in ((point.lat-.004+90)*100).toInt()..((point.lat+.004+90)*100).toInt())for(lon in ((point.lon-.004+180)*100).toInt()..((point.lon+.004+180)*100).toInt())add((lat.toLong()*36000+lon).toString())}
  val args=(cells+listOf((point.lat-.004).toString(),(point.lat+.004).toString(),(point.lon-.004).toString(),(point.lon+.004).toString())).toTypedArray()
  db.rawQuery("SELECT id,lat,lon,component FROM spatial WHERE ${if(origin)"can_start" else "can_end"}=1 AND cell IN (${cells.joinToString(","){"?"}}) AND lat BETWEEN ? AND ? AND lon BETWEEN ? AND ?",args).use{c->while(c.moveToNext()){val d=groundDistance(point,GeoPoint(c.getDouble(1),c.getDouble(2)));if(d<=350)candidates.add(RoadSnap(c.getLong(0),d,c.getLong(3)))}}
  check(candidates.isNotEmpty()){"No supported public driving junction within 350 m. Choose a nearby road point; a shop point is not a verified entrance."};return candidates.groupBy{it.component}.values.map{it.minBy{candidate->candidate.meters}}
 }
 suspend fun route(origin:Place,destination:Place):RouteReply {
  val starts=snaps(origin.point,true);val ends=snaps(destination.point,false).associateBy{it.component};val pair=starts.mapNotNull{a->ends[a.component]?.let{b->a to b}}.minByOrNull{it.first.meters+it.second.meters} ?: error("Nearby supported driving roads are disconnected. A sea crossing or missing access may prevent this trip.")
  val start=pair.first;val end=pair.second;check(start.node!=end.node){"Both places snap to the same road junction. Choose points farther apart."}
  val longTrip=groundDistance(origin.point,destination.point)>50000;val maxStates=if(longTrip)250000 else 120000;val maxMillis=if(longTrip)45000 else 15000;val heuristicWeight=8.0
  val target=node(end.node);val first=RoadKey(start.node,0);val costs=hashMapOf(first to 0.0);val previous=HashMap<RoadKey,Previous>();val frontier=PriorityQueue<Frontier>(compareBy{it.score});frontier.add(Frontier(first,0.0,0.0));val began=SystemClock.elapsedRealtime();var expanded=0;var goal:RoadKey?=null
  while(frontier.isNotEmpty()) {
   coroutineContext.ensureActive();if(expanded>=maxStates||SystemClock.elapsedRealtime()-began>=maxMillis)android.util.Log.w("MazeOffline","route limit states=$expanded elapsedMs=${SystemClock.elapsedRealtime()-began} start=${start.node} end=${end.node}");check(expanded<maxStates&&SystemClock.elapsedRealtime()-began<maxMillis){"Offline route reached its ${maxMillis/1000}-second / $maxStates-state limit. Try a shorter trip."}
   val current=frontier.poll() ?: break;if(current.cost!=costs[current.key])continue
   if(current.key.node==end.node){goal=current.key;break};expanded++
   val rules=db.rawQuery("SELECT toway,onlyturn,kind FROM turns WHERE via=? AND fromway=?",arrayOf(current.key.node.toString(),current.key.incomingWay.toString())).use{c->buildList{while(c.moveToNext())add(TurnRule(c.getLong(0),c.getInt(1)!=0,c.getString(2)))}}
   db.rawQuery("SELECT e.dst,e.way,e.meters,e.seconds,e.geom,e.reverse,n.lat,n.lon FROM edges e JOIN nodes n ON n.id=e.dst WHERE e.src=?",arrayOf(current.key.node.toString())).use{c->while(c.moveToNext()){
    val edge=RoadEdge(c.getLong(0),c.getLong(1),c.getDouble(2),c.getDouble(3),c.getLong(4),c.getInt(5)!=0)
    if(!turnAllowed(edge.way,rules,current.key.incomingWay,edge.destination==current.key.previousNode))continue
    val next=RoadKey(edge.destination,edge.way,current.key.node);val cost=current.cost+edge.seconds
    if(cost<(costs[next] ?: Double.POSITIVE_INFINITY)){costs[next]=cost;previous[next]=Previous(current.key,edge);frontier.add(Frontier(next,cost,cost+heuristicWeight*groundDistance(GeoPoint(c.getDouble(6),c.getDouble(7)),target)/40.0))}
   }}
  }
  var cursor=goal ?: error("No supported offline driving route connects these points. Access exclusions, missing roads or a sea crossing may prevent a route.")
  val edges=mutableListOf<RoadEdge>();while(cursor!=first){val p=previous.getValue(cursor);edges.add(p.edge);cursor=p.key};edges.reverse()
  val points=mutableListOf<GeoPoint>();val steps=mutableListOf<RoadStep>();var total=0.0;var seconds=0.0
  for(edge in edges){db.rawQuery("SELECT points,name FROM geometry WHERE id=?",arrayOf(edge.geometry.toString())).use{c->check(c.moveToFirst());val bytes=ByteBuffer.wrap(c.getBlob(0)).order(ByteOrder.LITTLE_ENDIAN);val segment=mutableListOf<GeoPoint>();while(bytes.remaining()>=8)segment.add(GeoPoint(bytes.float.toDouble(),bytes.float.toDouble()));if(edge.reverse)segment.reverse();points.addAll(if(points.isEmpty())segment else segment.drop(1));val name=c.getString(1)
    if(steps.lastOrNull()?.text==name){val old=steps.removeAt(steps.lastIndex);steps.add(RoadStep(name,old.meters+edge.meters))}else steps.add(RoadStep(name,edge.meters))
   };total+=edge.meters;seconds+=edge.seconds}
  check(points.size in 2..50000){"Offline route is too large to display safely."}
  val summary="Quick preview · not guaranteed fastest · "+"Offline driving preview · snap ${start.meters.roundToInt()} m / ${end.meters.roundToInt()} m"
  android.util.Log.i("MazeOffline","route states=$expanded elapsedMs=${SystemClock.elapsedRealtime()-began} meters=${total.roundToInt()} snap=${start.meters.roundToInt()}/${end.meters.roundToInt()}")
  return RouteReply(listOf(RoadRoute(points,steps,total,seconds,summary)),"On-device OSM · "+"quick preview, not guaranteed fastest · "+"${start.meters.roundToInt()}/${end.meters.roundToInt()} m junction snap",dataAt,false)
 }
}
