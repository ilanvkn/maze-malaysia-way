package my.maze.malaysia

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
internal data class DirectedRoad(val src:Long,val dst:Long,val way:Long,val meters:Double,val seconds:Double,val geometry:Long,val reverse:Boolean)
internal data class RoadGeometry(val id:Long,val points:List<GeoPoint>,val name:String,val directions:List<DirectedRoad>)
internal interface ApproachGraph {
 fun nearby(point:GeoPoint):Sequence<RoadGeometry>
 fun outgoing(node:Long):List<DirectedRoad>
 fun position(node:Long):GeoPoint
 fun rules(node:Long,incoming:Long):List<TurnRule>
 fun geometry(id:Long):RoadGeometry
}
internal data class RoadProjection(val point:GeoPoint,val fraction:Double,val gap:Double)
internal data class RoadAnchor(val geometry:RoadGeometry,val projection:RoadProjection,val node:Long?)
internal data class RoadPart(val road:DirectedRoad,val from:Double=0.0,val to:Double=1.0) {
 val meters get()=road.meters*(to-from)
 val seconds get()=road.seconds*(to-from)
}
internal data class ApproachRoute(val parts:List<RoadPart>,val start:RoadAnchor,val end:RoadAnchor,val expanded:Int)
private data class ApproachKey(val node:Long,val incoming:Long=0,val previous:Long=0)

private fun lengths(points:List<GeoPoint>):List<Double> = points.zipWithNext{a,b->groundDistance(a,b)}
private fun interpolate(a:GeoPoint,b:GeoPoint,t:Double)=GeoPoint(a.lat+(b.lat-a.lat)*t,a.lon+(b.lon-a.lon)*t)
/** Local tangent-plane projection. Distance/fractions use the complete polyline. */
internal fun projectRoad(point:GeoPoint,points:List<GeoPoint>):RoadProjection {
 require(points.size in 2..50000)
 val spans=lengths(points);val total=spans.sum();require(total>0)
 val scale=cos(Math.toRadians(point.lat));var walked=0.0;var best:RoadProjection?=null
 for(i in spans.indices){
  val a=points[i];val b=points[i+1];val x=(b.lon-a.lon)*scale;val y=b.lat-a.lat
  val t=if(x*x+y*y==0.0)0.0 else (((point.lon-a.lon)*scale*x+(point.lat-a.lat)*y)/(x*x+y*y)).coerceIn(0.0,1.0)
  val projected=interpolate(a,b,t);val gap=groundDistance(point,projected)
  if(best==null||gap<best.gap)best=RoadProjection(projected,(walked+spans[i]*t)/total,gap)
  walked+=spans[i]
 }
 return best!!
}
internal fun roadSlice(points:List<GeoPoint>,from:Double,to:Double):List<GeoPoint> {
 require(from in 0.0..1.0&&to in from..1.0)
 val spans=lengths(points);val total=spans.sum();require(total>0)
 val a=from*total;val b=to*total;var walked=0.0;val result=mutableListOf<GeoPoint>()
 for(i in spans.indices){val next=walked+spans[i]
  if(spans[i]>0&&next>=a&&walked<=b){
   val begin=interpolate(points[i],points[i+1],((a-walked)/spans[i]).coerceIn(0.0,1.0))
   val end=interpolate(points[i],points[i+1],((b-walked)/spans[i]).coerceIn(0.0,1.0))
   if(result.lastOrNull()!=begin)result.add(begin)
   if(result.lastOrNull()!=end)result.add(end)
  };walked=next
 }
 return result
}
internal fun projectApproach(point:GeoPoint,g:RoadGeometry):RoadProjection {
 var projection=projectRoad(point,g.points);val road=g.directions.first()
 // Share routing's sub-metre endpoint normalization with the visual preview.
 if(projection.fraction*road.meters<=0.5)projection=RoadProjection(g.points.first(),0.0,groundDistance(point,g.points.first()))
 else if((1-projection.fraction)*road.meters<=0.5)projection=RoadProjection(g.points.last(),1.0,groundDistance(point,g.points.last()))
 return projection
}
internal fun roadAnchor(graph:ApproachGraph,point:GeoPoint,checkpoint:()->Unit={},preferredGeometry:Long?=null):RoadAnchor {
 var nearest=Double.POSITIVE_INFINITY
 val candidates=mutableListOf<RoadAnchor>()
 for(g in graph.nearby(point)){
  checkpoint()
  if(g.directions.isEmpty()||(preferredGeometry!=null&&g.id!=preferredGeometry))continue
  val projection=projectApproach(point,g)
  val canonical=g.directions.first()
  if(projection.gap>350||projection.gap>nearest+1.0)continue
  val src=if(canonical.reverse)canonical.dst else canonical.src;val dst=if(canonical.reverse)canonical.src else canonical.dst
  val node=when {projection.fraction==0.0->src;projection.fraction==1.0->dst;else->null}
  if(projection.gap<nearest){nearest=projection.gap;candidates.removeAll{it.projection.gap>nearest+1.0}}
  candidates.add(RoadAnchor(g,projection,node))
  check(candidates.size<=128){"Ambiguous nearby road approaches. Choose a point clearly on the intended road."}
 }
 check(candidates.isNotEmpty()){"No supported driving segment within 350 m. Choose a mapped public road point; access to a place is not verified."}
 val best=candidates.minWith(compareBy<RoadAnchor>{it.projection.gap}.thenBy{it.geometry.id})
 check(candidates.all{it===best||(best.node!=null&&it.node==best.node)}){"Ambiguous nearby road approaches. Choose a point clearly on the intended road; Maze cannot infer a crossing or entry permission."}
 return best
}
/** Virtual endpoints retain incoming way and previous junction, including partial edges. */
internal fun routeApproaches(graph:ApproachGraph,origin:GeoPoint,destination:GeoPoint,mode:RouteSearchMode,nowMillis:()->Long,checkpoint:()->Unit={},destinationApproach:Long?=null,originApproach:Long?=null):ApproachRoute {
 val began=nowMillis();val longTrip=groundDistance(origin,destination)>50000
 val deadline=began+if(longTrip)45000 else 15000;val maxStates=if(longTrip)250000 else 120000
 fun checkWork(){checkpoint();if(nowMillis()>=deadline)throw SearchBudgetExceeded()}
 checkWork();val start=roadAnchor(graph,origin,::checkWork,originApproach);checkWork();val end=roadAnchor(graph,destination,::checkWork,destinationApproach);checkWork()
 check(start.geometry.id!=end.geometry.id||abs(start.projection.fraction-end.projection.fraction)*start.geometry.directions.first().meters>0.05){"Both points reach the same road position. Choose points farther apart."}
 val root=ApproachKey(start.node ?: -1L);val goal=end.node ?: -2L
 check(!(root.node==goal)||root.node<0){"Both points reach the same road position. Choose points farther apart."}
 fun fraction(anchor:RoadAnchor,road:DirectedRoad)=if(road.reverse)1-anchor.projection.fraction else anchor.projection.fraction
 fun partArc(road:DirectedRoad,from:Double,to:Double,target:Long):SearchArc<ApproachKey,RoadPart>? {
  if(to<=from)return null
  val part=RoadPart(road,from,to)
  return SearchArc(ApproachKey(target,road.way,road.src),part.seconds,part)
 }
 val found=boundedAStar<ApproachKey,RoadPart>(
  start=root,isGoal={it.node==goal},
  outgoing={key->
   checkWork()
   if(key.node == -1L){buildList {
    for(road in start.geometry.directions){
     val from=fraction(start,road)
     partArc(road,from,1.0,road.dst)?.let{add(it)}
     if(end.node==null&&end.geometry.id==start.geometry.id)partArc(road,from,fraction(end,road),goal)?.let{add(it)}
    }
   }}else{
    val rules=graph.rules(key.node,key.incoming)
    graph.outgoing(key.node).filter{turnAllowed(it.way,rules,key.incoming,it.dst==key.previous)}.flatMap{road->
     buildList {
      partArc(road,0.0,1.0,road.dst)?.let{add(it)}
      if(end.node==null&&road.geometry==end.geometry.id)partArc(road,0.0,fraction(end,road),goal)?.let{add(it)}
     }
    }
   }
  },
  heuristic={key->when(key.node){-1L->groundDistance(start.projection.point,end.projection.point)/40;-2L->0.0;else->groundDistance(graph.position(key.node),end.projection.point)/40}},
  weight=mode.weight,maxExpanded=maxStates,maxDiscovered=maxStates*4,deadlineMillis=deadline,nowMillis=nowMillis,checkpoint=checkpoint
 ) ?: error("No supported driving route connects the nearest road approaches. A barrier, one-way road, sea crossing or excluded access may prevent this trip. Choose road points; Maze will not move them to another road.")
 check(found.edges.isNotEmpty()){"Both points reach the same road position. Choose points farther apart."}
 return ApproachRoute(found.edges,start,end,found.expanded)
}
internal fun approachRoadRoute(graph:ApproachGraph,result:ApproachRoute,mode:RouteSearchMode):RoadRoute {
 val points=mutableListOf<GeoPoint>();val steps=mutableListOf<RoadStep>()
 for(part in result.parts){val geometry=graph.geometry(part.road.geometry);val oriented=if(part.road.reverse)geometry.points.reversed() else geometry.points
  val segment=roadSlice(oriented,part.from,part.to)
  // Topological joins may differ by float rounding, never insert an off-road connector.
  points.addAll(if(points.isEmpty())segment else segment.drop(1))
  check(points.size<=50000){"Offline route is too large to display safely."}
  if(steps.lastOrNull()?.text==geometry.name){val old=steps.removeAt(steps.lastIndex);steps.add(RoadStep(old.text,old.meters+part.meters))}else steps.add(RoadStep(geometry.name,part.meters))
 }
 check(points.size>=2){"Both points reach the same road position. Choose points farther apart."}
 val gaps="${result.start.projection.gap.roundToInt()}/${result.end.projection.gap.roundToInt()} m segment snap"
 return RoadRoute(points,steps,result.parts.sumOf{it.meters},result.parts.sumOf{it.seconds},"${mode.label} · not guaranteed fastest · $gaps · final access unverified")
}

data class RoadChoice(val geometry:Long,val way:Long,val name:String,val gap:Double)
internal fun roadChoices(graph:ApproachGraph,point:GeoPoint,checkpoint:()->Unit={}):List<RoadChoice>{
 val best=mutableListOf<RoadChoice>()
 for(g in graph.nearby(point)){
  checkpoint();if(g.directions.isEmpty())continue
  val gap=projectRoad(point,g.points).gap;if(gap>350)continue
  val choice=RoadChoice(g.id,g.directions.first().way,g.name,gap)
  val old=best.firstOrNull{it.way==choice.way}
  if(old!=null&&old.gap<=gap)continue
  best.removeAll{it.way==choice.way};best.add(choice);best.sortWith(compareBy<RoadChoice>{it.gap}.thenBy{it.geometry});if(best.size>20)best.removeAt(best.lastIndex)
 }
 return best
}
