package my.maze.malaysia

import kotlin.math.*

/** Display geometry only. Every point lies on the selected mapped segment. */
data class ApproachArrow(val point:GeoPoint,val bearing:Double)
data class ApproachPreview(
 val choice:RoadChoice,val points:List<GeoPoint>,val snap:GeoPoint,
 val arrows:List<ApproachArrow>,val oneWay:Boolean,val gap:Double
)
internal fun approachDisplayName(name:String)=when(name){
 "service"->"Service road";"residential"->"Local road";"unclassified"->"Unnamed road";else->name
}
private fun bearing(a:GeoPoint,b:GeoPoint):Double {
 val p=Math.toRadians(a.lat);val q=Math.toRadians(b.lat);val d=Math.toRadians(b.lon-a.lon)
 return (Math.toDegrees(atan2(sin(d)*cos(q),cos(p)*sin(q)-sin(p)*cos(q)*cos(d)))+360)%360
}
internal fun approachPreview(graph:ApproachGraph,choice:RoadChoice,place:GeoPoint):ApproachPreview {
 val g=graph.geometry(choice.geometry)
 require(g.directions.isNotEmpty()&&g.directions.all{it.way==choice.way}){"This road choice is no longer available. Review the roads again."}
 val projected=projectApproach(place,g)
 require(projected.gap<=350){"This road is outside the nearby approach area. Review the roads again."}
 val meters=g.points.zipWithNext{a,b->groundDistance(a,b)}.sum();require(meters>0)
 // A local 400 m window keeps the road/place relationship readable on small screens.
 val half=200/meters
 val clipped=roadSlice(g.points,(projected.fraction-half).coerceAtLeast(0.0),(projected.fraction+half).coerceAtMost(1.0))
 require(clipped.size in 2..2048){"This segment is too detailed to preview safely. Choose another road."}
 val oneWay=g.directions.map{it.reverse}.distinct().size==1
 val points=if(oneWay&&g.directions.first().reverse)clipped.reversed() else clipped
 val arrows=if(oneWay)listOf(.25,.5,.75).mapNotNull{fraction->
  val span=roadSlice(points,(fraction-.015).coerceAtLeast(0.0),(fraction+.015).coerceAtMost(1.0))
  if(span.size<2||groundDistance(span.first(),span.last())<.05)null
  else ApproachArrow(roadSlice(points,0.0,fraction).last(),bearing(span.first(),span.last()))
 }else emptyList()
 return ApproachPreview(choice,points,projected.point,arrows,oneWay,projected.gap)
}
/** Provider estimate, not an entrance confidence boundary or a manufactured GPS fix. */
internal fun accuracyRing(point:GeoPoint,meters:Float?):List<GeoPoint> {
 if(!inMalaysiaRegion(point)||meters==null||!meters.isFinite()||meters<=0||meters>5000)return emptyList()
 val p=Math.toRadians(point.lat);val q=Math.toRadians(point.lon);val radius=meters/6371000.0
 return (0..64).map{i->val angle=2*Math.PI*i/64;val lat=asin(sin(p)*cos(radius)+cos(p)*sin(radius)*cos(angle));val lon=q+atan2(sin(angle)*sin(radius)*cos(p),cos(radius)-sin(p)*sin(lat));GeoPoint(Math.toDegrees(lat),Math.toDegrees(lon))}
}

internal fun needsManualRoadStart(accuracy:Float?)=accuracy!=null&&(!accuracy.isFinite()||accuracy<=0||accuracy>350)
