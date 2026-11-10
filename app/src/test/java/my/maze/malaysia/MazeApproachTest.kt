package my.maze.malaysia

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MazeApproachTest {
 private class Fixture(val json:JSONObject):ApproachGraph {
  val roads=json.getJSONArray("edges").let{a->(0 until a.length()).map{i->val r=a.getJSONArray(i);DirectedRoad(r.getLong(0),r.getLong(1),r.getLong(2),r.getDouble(3),r.getDouble(4),r.getLong(5),r.getInt(6)!=0)}}
  val nodes=json.getJSONArray("nodes").let{a->(0 until a.length()).associate{i->val r=a.getJSONArray(i);r.getLong(0) to GeoPoint(r.getDouble(1),r.getDouble(2))}}
  val shapes=json.getJSONArray("geometry").let{a->(0 until a.length()).associate{i->val r=a.getJSONObject(i);val ps=r.getJSONArray("points");val id=r.getLong("id");id to RoadGeometry(id,(0 until ps.length()).map{j->val p=ps.getJSONArray(j);GeoPoint(p.getDouble(0),p.getDouble(1))},r.getString("name"),roads.filter{it.geometry==id})}}
  override fun nearby(point:GeoPoint)=shapes.values.asSequence()
  override fun outgoing(node:Long)=roads.filter{it.src==node}
  override fun position(node:Long)=nodes.getValue(node)
  override fun geometry(id:Long)=shapes.getValue(id)
  override fun rules(node:Long,incoming:Long):List<TurnRule>{val a=json.getJSONArray("turns");return (0 until a.length()).map{a.getJSONArray(it)}.filter{it.getLong(0)==node&&it.getLong(1)==incoming}.map{TurnRule(it.getLong(2),it.getInt(3)!=0,it.getString(4))}}
 }
 private fun fixture()=Fixture(JSONObject(javaClass.getResource("/approaches/graph.json")!!.readText()))
 @Test fun directedApproachesMatchScenarioExpectations(){
  val graph=fixture();val queries=graph.json.getJSONArray("queries");val reports=org.json.JSONArray()
  for(i in 0 until queries.length()){val q=queries.getJSONObject(i);val name=q.getString("name");val a=q.getJSONArray("origin");val b=q.getJSONArray("destination");val start=GeoPoint(a.getDouble(0),a.getDouble(1));val end=GeoPoint(b.getDouble(0),b.getDouble(1))
   for(mode in RouteSearchMode.entries){val row=JSONObject().put("name",name).put("mode",mode.name);reports.put(row)
    val found=runCatching{routeApproaches(graph,start,end,mode,{0L})}
    if(q.getString("status")!="route"){
     assertTrue("$name should reject a direction, disconnection or ambiguity",found.isFailure)
     val message=found.exceptionOrNull()!!.message!!;row.put("error",message)
     assertTrue(name, message.contains(if(q.getString("status")=="ambiguous")"Ambiguous" else "No supported driving route"))
    }else{
     val path=found.getOrThrow();val route=approachRoadRoute(graph,path,mode)
     row.put("meters",route.meters).put("seconds",route.seconds).put("ways",org.json.JSONArray(path.parts.map{it.road.way})).put("geometry",org.json.JSONArray(route.geometry.map{listOf(it.lat,it.lon)}))
     assertTrue(name,route.meters>0&&route.seconds>0)
     assertTrue(name,groundDistance(route.geometry.first(),start)<1)
     assertTrue(name,groundDistance(route.geometry.last(),end)<1)
     if(name=="divided-turnaround"){assertTrue(route.meters>3300);assertTrue(path.parts.any{it.road.way==22L})}
     if(name=="restricted-partial-destination"){assertTrue(path.parts.any{it.road.way==52L});assertTrue(route.meters>3000)}
     if(name.startsWith("curved-same-edge")){assertEquals(1,path.parts.size);assertEquals(3,route.geometry.size)}
     if(name=="oneway-forward")assertEquals(1,path.parts.size)
    }
   }
  }
  File("build/approach-fixture-results.json").writeText(reports.toString(2))
 }
 @Test fun noisyMedianDoesNotChooseCarriagewayAndExplicitSideDoesNotFlip(){
  val g=fixture();val positions=listOf(GeoPoint(3.01002,101.005),GeoPoint(3.01018,101.005),GeoPoint(3.01008,101.005))
  val first=roadReview(g,positions.first(),30f);val selected=first.choices.first{it.way==20L}
  assertEquals(2,first.withinEstimate)
  assertNotEquals(roadChoices(g,positions[0]).first().way,roadChoices(g,positions[1]).first().way)
  for(point in positions){
   val review=roadReview(g,point,30f);assertEquals(2,review.withinEstimate)
   val anchor=roadAnchor(g,point,preferredGeometry=selected.geometry)
   assertEquals(20L,anchor.geometry.directions.first().way)
   assertEquals(3.01,anchor.projection.point.lat,.000001)
  }
 }
 @Test fun overlapCountsAreNotHiddenByTheTwentyOptionLimit(){
  val shapes=(1L..25L).map{id->val points=listOf(GeoPoint(3.0+id*.000001,101.0),GeoPoint(3.0+id*.000001,101.01));RoadGeometry(id,points,"Same mapped name",listOf(DirectedRoad(id*2,id*2+1,id,1110.0,100.0,id,false)))}
  val graph=object:ApproachGraph by fixture(){override fun nearby(point:GeoPoint)=shapes.asSequence()}
  val review=roadReview(graph,GeoPoint(3.0,101.005),10f)
  assertEquals(25,review.withinEstimate);assertEquals(20,review.choices.size);assertTrue(review.limited)
 }
 @Test fun reverseTravelOnOneWayRemainsRejected(){
  val graph=fixture();val wrapped=object:ApproachGraph by graph{override fun rules(node:Long,incoming:Long)=if(node==2L&&incoming==10L)listOf(TurnRule(10,false,"no_u_turn"))else graph.rules(node,incoming)}
  assertTrue(runCatching{routeApproaches(wrapped,GeoPoint(3.0,101.015),GeoPoint(3.0,101.005),RouteSearchMode.CAREFUL,{0L})}.isFailure)
 }
 @Test fun explicitApproachChoiceResolvesMedianWithoutGrantingReverseTravel(){
  val g=fixture();val point=GeoPoint(3.0101,101.005)
  val choices=roadChoices(g,point);val south=choices.first{it.way==20L}
  val route=routeApproaches(g,GeoPoint(3.01,101.002),point,RouteSearchMode.CAREFUL,{0L},destinationApproach=south.geometry)
  assertTrue(route.parts.all{it.road.way==20L})
  assertTrue(runCatching{routeApproaches(g,GeoPoint(3.01,101.005),point,RouteSearchMode.CAREFUL,{0L},destinationApproach=south.geometry)}.isFailure)
  assertTrue(runCatching{roadAnchor(g,point,preferredGeometry=999999L)}.isFailure)
  assertEquals(choices.map{it.way}.distinct().size,choices.size)
 }
 @Test fun subMetreProjectionsAtOneJunctionShareItsTopology(){
  val anchor=roadAnchor(fixture(),GeoPoint(3.0400005,101.0100005))
  assertEquals(52L,anchor.node)
  assertTrue(anchor.projection.fraction==0.0||anchor.projection.fraction==1.0)
 }
 @Test fun denseCandidateScanChecksCancellationBeforeExhaustingInput(){
  val g=fixture();var consumed=0
  val wrapped=object:ApproachGraph by g{
   override fun nearby(point:GeoPoint)=sequence{
    val far=g.shapes.values.first().copy(points=listOf(GeoPoint(4.0,102.0),GeoPoint(4.0,102.01)))
    repeat(4000){consumed++;yield(far)}
    yieldAll(g.nearby(point))
   }
  }
  var checks=0
  assertThrows(kotlinx.coroutines.CancellationException::class.java){roadAnchor(wrapped,GeoPoint(3.0,101.005),checkpoint={if(++checks==20)throw kotlinx.coroutines.CancellationException()})}
  assertTrue(consumed<4000)
  consumed=0
  assertEquals(1L,roadAnchor(wrapped,GeoPoint(3.0,101.005)).geometry.id)
  assertEquals(4000,consumed)
 }
 @Test fun cancellationDeadlineAndNoNearbyRoadFailExplicitly(){
  val g=fixture()
  assertThrows(kotlinx.coroutines.CancellationException::class.java){routeApproaches(g,GeoPoint(3.0,101.005),GeoPoint(3.0,101.015),RouteSearchMode.QUICK,{0L},checkpoint={throw kotlinx.coroutines.CancellationException()})}
  var ticks=0L
  assertThrows(SearchBudgetExceeded::class.java){routeApproaches(g,GeoPoint(3.0,101.005),GeoPoint(3.0,101.015),RouteSearchMode.QUICK,{ticks.also{ticks+=20000}})}
  assertTrue(runCatching{roadAnchor(g,GeoPoint(5.0,102.0))}.exceptionOrNull()!!.message!!.contains("within 350 m"))
 }
}
