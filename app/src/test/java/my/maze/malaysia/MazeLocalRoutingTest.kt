package my.maze.malaysia
import org.junit.Assert.*
import org.junit.Test
class MazeLocalRoutingTest {
 @Test fun forbiddenAndOnlyTurnsAreNotInterchangeable(){
  assertFalse(turnAllowed(9,listOf(TurnRule(9,false))))
  assertTrue(turnAllowed(8,listOf(TurnRule(9,false))))
  assertFalse(turnAllowed(8,listOf(TurnRule(9,true))))
  assertTrue(turnAllowed(9,listOf(TurnRule(9,true))))
  assertFalse(turnAllowed(9,listOf(TurnRule(9,true),TurnRule(-1,true))))
  assertFalse(turnAllowed(9,listOf(TurnRule(9,true),TurnRule(9,false))))
 }
 @Test fun noUTurnDoesNotForbidContinuingOnTheSameWay(){
  val noU=listOf(TurnRule(7,false,"no_u_turn"));assertFalse(turnAllowed(7,noU,7,true));assertTrue(turnAllowed(7,noU,7,false))
  val onlyStraight=listOf(TurnRule(7,true,"only_straight_on"));assertFalse(turnAllowed(7,onlyStraight,7,true));assertTrue(turnAllowed(7,onlyStraight,7,false))
  val noStraight=listOf(TurnRule(7,false,"no_straight_on"));assertTrue(turnAllowed(7,noStraight,7,true));assertFalse(turnAllowed(7,noStraight,7,false))
  assertFalse(turnAllowed(7,noU,8,false))
 }
 @Test fun packCleanupOnlyMatchesGeneratedSpatialFiles(){
  val prefix="a".repeat(64);assertTrue(isRegionalPackFile("$prefix-roads.sqlite"));assertTrue(isRegionalPackFile("$prefix-roads.sqlite.part",true));assertFalse(isRegionalPackFile("$prefix-roads.sqlite",true));assertFalse(isRegionalPackFile("saved-trips.json"));assertFalse(isRegionalPackFile("$prefix-roadssqlite"));assertFalse(isRegionalPackFile("../$prefix-roads.sqlite"))
 }
 @Test fun distanceUsesMetersAcrossMalaysia(){
  assertEquals(0.0,groundDistance(GeoPoint(3.0,101.0),GeoPoint(3.0,101.0)),0.001)
  assertEquals(111195.0,groundDistance(GeoPoint(3.0,101.0),GeoPoint(4.0,101.0)),2.0)
  assertTrue(groundDistance(landmarks[0].point,landmarks[1].point) in 3000.0..5000.0)
 }
}
