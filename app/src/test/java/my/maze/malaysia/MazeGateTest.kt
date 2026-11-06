package my.maze.malaysia

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MazeGateTest{
 private val today=LocalDate.of(2026,11,7)
 private fun text(demo:Boolean=true):String=JSONObject().put("schema",1).put("site","Fictional test site").put("source","Synthetic test author").put("permission","approved_for_pilot").put("demo",demo).put("gates",org.json.JSONArray().put(JSONObject().put("id","a").put("name","Test gate").put("access","public").put("motorcar","yes").put("availability","open").put("instructions","Check hours").put("checked_on","2026-11-07").put("checked_by","Test author").put("evidence","Synthetic observation").put("lat",if(demo)JSONObject.NULL else 3.14).put("lon",if(demo)JSONObject.NULL else 101.69))).toString()
 @Test fun visitPurposeCannotGrantPrivateAccess(){
  val gates=demoGateDataset(today).gates
  assertFalse(assessGate(gates[1],GatePurpose.VISITOR,today).candidate)
  assertTrue(assessGate(gates[1],GatePurpose.DELIVERY,today).candidate)
  for(p in GatePurpose.entries){assertFalse(assessGate(gates[2],p,today).candidate);assertFalse(assessGate(gates[3],p,today).candidate);assertFalse(assessGate(gates[4],p,today).candidate);assertFalse(assessGate(gates[5],p,today).candidate)}
 }
 @Test fun freshnessBoundaryAndFutureDatesFailClosed(){
  val g=demoGateDataset(today).gates.first()
  assertTrue(assessGate(g.copy(checkedOn=today.minusDays(30)),GatePurpose.VISITOR,today).candidate)
  assertFalse(assessGate(g.copy(checkedOn=today.minusDays(31)),GatePurpose.VISITOR,today).candidate)
  assertFalse(assessGate(g.copy(checkedOn=today.plusDays(1)),GatePurpose.VISITOR,today).candidate)
 }
 @Test fun demoHasNoRouteCoordinatesAndRealFilesRequireConsent(){
  assertNull(parseGateDataset(text(),today).gates.first().point)
  assertNotNull(parseGateDataset(text(false),today).gates.first().point)
  assertThrows(IllegalArgumentException::class.java){parseGateDataset(text(false).replace("approved_for_pilot","unknown"),today)}
  assertThrows(IllegalArgumentException::class.java){parseGateDataset(text(false).replace("\"demo\":false","\"demo\":true"),today)}
 }
 @Test fun malformedDatesEvidenceDuplicateIdsAndOversizeAreRejected(){
  val valid=JSONObject(text());val gate=valid.getJSONArray("gates").getJSONObject(0)
  for(key in listOf("checked_on","checked_by","access")){
   val copy=JSONObject(valid.toString());copy.getJSONArray("gates").getJSONObject(0).put(key,when(key){"checked_on"->"2026-11-08";"checked_by"->"";else->"magic"})
   assertTrue(runCatching{parseGateDataset(copy.toString(),today)}.isFailure)
  }
  valid.getJSONArray("gates").put(gate);assertTrue(runCatching{parseGateDataset(valid.toString(),today)}.isFailure)
  assertTrue(runCatching{parseGateDataset(" ".repeat(GATE_MAX_BYTES+1),today)}.isFailure)
  assertTrue(runCatching{parseGateDataset(text(false).replace("101.69","180.0"),today)}.isFailure)
 }
 @Test fun savedGateIdentityDoesNotCollideAcrossSites(){
  val a=parseGateDataset(text(false),today);val b=a.copy(site="Another fictional site")
  assertNotEquals(pilotDestination(a,a.gates.first()).id,pilotDestination(b,b.gates.first()).id)
  assertThrows(IllegalArgumentException::class.java){val d=parseGateDataset(text(),today);pilotDestination(d,d.gates.first())}
 }
 @Test fun publicGateDoesNotImplyMotorcarPermission(){
  val g=demoGateDataset(today).gates.first()
  for(value in listOf("no","unknown"))assertFalse(assessGate(g.copy(motorcar=value),GatePurpose.VISITOR,today).candidate)
 }
 @Test fun nestedAndConcatenatedPayloadsAreRejectedBeforeUse(){
  assertThrows(IllegalArgumentException::class.java){parseGateDataset("[".repeat(200)+"]".repeat(200),today)}
  assertThrows(IllegalArgumentException::class.java){parseGateDataset(text()+" true",today)}
 }
}
