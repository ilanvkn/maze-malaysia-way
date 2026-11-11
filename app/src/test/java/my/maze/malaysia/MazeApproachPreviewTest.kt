package my.maze.malaysia
import org.junit.Assert.*
import org.junit.Test

class MazeApproachPreviewTest {
 private fun graph(points:List<GeoPoint>,reverse:Boolean=false,twoWay:Boolean=false):ApproachGraph {
  val meters=points.zipWithNext{a,b->groundDistance(a,b)}.sum()
  val road=DirectedRoad(if(reverse)2 else 1,if(reverse)1 else 2,10,meters,meters/8,1,reverse)
  val g=RoadGeometry(1,points,"Test road",if(twoWay)listOf(road,road.copy(src=road.dst,dst=road.src,reverse=!reverse))else listOf(road))
  return object:ApproachGraph {
   override fun geometry(id:Long)=g.also{require(id==1L)}
   override fun nearby(point:GeoPoint)=sequenceOf(g)
   override fun outgoing(node:Long)=g.directions.filter{it.src==node}
   override fun position(node:Long)=if(node==1L)points.first() else points.last()
   override fun rules(node:Long,incoming:Long)=emptyList<TurnRule>()
  }
 }
 private val line=listOf(GeoPoint(3.0,101.0),GeoPoint(3.0,101.02))
 private val choice=RoadChoice(1,10,"Test road",20.0)
 @Test fun previewClipsOnlyMappedLineAndKeepsPlaceSeparate(){
  val place=GeoPoint(3.0002,101.01);val preview=approachPreview(graph(line),choice,place)
  assertTrue(preview.points.size>=2)
  assertEquals(400.0,preview.points.zipWithNext{a,b->groundDistance(a,b)}.sum(),.05)
  assertTrue(preview.points.all{projectRoad(it,line).gap<.01})
  assertTrue(groundDistance(place,preview.snap)>20)
  assertFalse(place in preview.points)
 }
 @Test fun arrowsFollowForwardAndReverseOneWayGeometry(){
  val point=GeoPoint(3.0,101.01)
  val east=approachPreview(graph(line),choice,point);val west=approachPreview(graph(line,true),choice,point)
  assertTrue(east.oneWay&&west.oneWay)
  assertEquals(3,east.arrows.size)
  assertTrue(east.arrows.all{it.bearing in 89.0..91.0})
  assertTrue(west.arrows.all{it.bearing in 269.0..271.0})
  assertTrue(west.points.first().lon>west.points.last().lon)
  val both=approachPreview(graph(line,twoWay=true),choice,point);assertFalse(both.oneWay);assertTrue(both.arrows.isEmpty())
 }
 @Test fun bendAndJunctionNormalizationMatchTheRoutingAnchor(){
  val curved=listOf(GeoPoint(3.0,101.0),GeoPoint(3.005,101.01),GeoPoint(3.0,101.02));val g=graph(curved)
  val preview=approachPreview(g,choice,GeoPoint(3.0051,101.01));assertEquals(3,preview.points.size)
  assertTrue(preview.points.all{projectRoad(it,curved).gap<.01})
  val nearEnd=GeoPoint(3.0000005,101.0199998);val straight=graph(line)
  assertEquals(roadAnchor(straight,nearEnd,preferredGeometry=1).projection.point,approachPreview(straight,choice,nearEnd).snap)
 }
 @Test fun mismatchedIdentityAndDistantChoicesNeverProduceAHighlight(){
  assertThrows(IllegalArgumentException::class.java){approachPreview(graph(line),choice.copy(way=999),GeoPoint(3.0,101.01))}
  assertThrows(IllegalArgumentException::class.java){approachPreview(graph(line),choice,GeoPoint(4.0,102.0))}
 }
 @Test fun reportedAccuracyRingIsBoundedClosedAndDoesNotInventQuality(){
  val point=GeoPoint(3.14,101.69)
  for(radius in listOf(20f,250f,5000f)){
   val ring=accuracyRing(point,radius);assertEquals(65,ring.size);assertTrue(groundDistance(ring.first(),ring.last())<.01)
   assertTrue(ring.all{kotlin.math.abs(groundDistance(point,it)-radius)<.02})
  }
  for(bad in listOf(null,Float.NaN,-1f,0f,5001f))assertTrue(accuracyRing(point,bad).isEmpty())
  assertTrue(accuracyRing(GeoPoint(50.0,1.0),100f).isEmpty())
  assertFalse(needsManualRoadStart(250f));assertFalse(needsManualRoadStart(350f));assertTrue(needsManualRoadStart(351f));assertTrue(needsManualRoadStart(5000f));assertTrue(needsManualRoadStart(Float.NaN))
 }
}
