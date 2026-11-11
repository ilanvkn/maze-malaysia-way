package my.maze.malaysia

import org.junit.Assert.*
import org.junit.Test

class MazeFixWindowTest {
 private val start=100_000_000_000L
 private fun fix(seconds:Double, north:Double=0.0, accuracy:Float=10f, provider:String="gps") = FixCandidate(
  GeoPoint(3.1341+north/111195.0,101.6869),accuracy,start+(seconds*1e9).toLong(),provider)
 private fun put(window:FixWindow,seconds:Double,north:Double=0.0,accuracy:Float=10f,provider:String="gps"):Boolean {
  val f=fix(seconds,north,accuracy,provider);return window.offer(f,f.elapsedNanos)
 }
 @Test fun oneOptimisticFixCannotEndSessionAndSamplesAreTimeSpaced(){
  val w=FixWindow(true,start);assertTrue(put(w,0.0));assertFalse(w.ready(start))
  assertFalse(put(w,0.0));assertFalse(put(w,.1));assertFalse(put(w,-1.0))
  assertTrue(put(w,1.0));assertFalse(w.ready(start+1_000_000_000L))
  assertTrue(put(w,2.0));assertTrue(w.ready(start+2_000_000_000L))
  assertEquals(3,w.assessment(start+2_000_000_000L)!!.samples)
 }
 @Test fun alternatingCarriagewaysAreNotAveragedOrCalledConsistent(){
  val w=FixWindow(true,start);put(w,0.0,0.0);put(w,1.0,30.0);put(w,2.0,0.0)
  val a=w.assessment(start+2_000_000_000L)!!
  assertFalse(w.ready(start+2_000_000_000L));assertFalse(a.consistent)
  assertEquals(fix(2.0).point,a.fix.point);assertEquals(30.0,a.spreadMeters,.1)
  assertTrue(a.uncertaintyMeters>=40f);assertTrue(estimateDescription(a,true).contains("road uncertain"))
 }
 @Test fun latestSampleWinsInsteadOfOldOptimisticAccuracy(){
  val w=FixWindow(true,start);put(w,0.0,0.0,5f);put(w,1.0,100.0,80f);put(w,2.0,30.0,120f)
  val a=w.assessment(start+2_000_000_000L)!!
  assertEquals(fix(2.0,30.0,120f).point,a.fix.point)
  assertEquals(120f,a.fix.accuracyMeters);assertTrue(a.uncertaintyMeters>=150f)
  assertTrue(estimateDescription(a,true).contains("reported ±120 m"))
 }
 @Test fun expiryRejectsStaleAndPreRequestData(){
  val w=FixWindow(true,start);assertFalse(w.offer(fix(-1.0),start));assertFalse(w.offer(fix(0.0),start+6_000_000_000L))
  put(w,1.0);assertNull(w.assessment(start+6_000_000_001L))
  assertFalse(w.offer(fix(10.0),start+9_000_000_000L))
 }
 @Test fun crossProviderConflictCannotProduceEarlySuccess(){
  val w=FixWindow(true,start);put(w,0.0);put(w,1.0,0.0,10f,"network");put(w,2.0)
  assertFalse(w.ready(start+2_000_000_000L));assertFalse(w.assessment(start+2_000_000_000L)!!.consistent)
 }
 @Test fun boundedWindowRecoversWithoutRetainingOldOutlierOrShrinkingReportedRadius(){
  val w=FixWindow(true,start);put(w,0.0,100.0)
  for(i in 1..19)put(w,i.toDouble(),0.0,20f)
  val a=w.assessment(start+19_000_000_000L)!!
  assertEquals(8,a.samples);assertEquals(20f,a.uncertaintyMeters,0f);assertTrue(a.consistent)
 }
 @Test fun enormousDisagreementFailsInsteadOfClampingAFalseCircle(){
  val w=FixWindow(true,start);put(w,0.0);put(w,1.0,6000.0)
  assertNull(w.assessment(start+1_000_000_000L))
 }
 @Test fun weakAndCoarseEstimatesWaitAndPreserveSourceLabels(){
  val w=FixWindow(true,start);for(i in 0..2)put(w,i.toDouble(),0.0,250f)
  val a=w.assessment(start+2_000_000_000L)!!;assertFalse(w.ready(start+2_000_000_000L));assertEquals(250f,a.uncertaintyMeters,0f)
  val c=FixWindow(false,start);assertFalse(put(c,0.0));assertTrue(c.offer(fix(1.0,accuracy=3000f,provider="network").copy(mock=true),start+1_000_000_000L))
  val b=c.assessment(start+1_000_000_000L)!!;assertTrue(estimateDescription(b,false).contains("Approximate location"));assertTrue(estimateDescription(b,false).contains("simulated location"));assertTrue(needsManualRoadStart(b.uncertaintyMeters))
 }
 @Test fun currentSnapshotExpiryUsesMonotonicAgeAndFailsMissingFuture(){
  assertTrue(expiredLocationSnapshot(null,start));assertTrue(expiredLocationSnapshot(0,start));assertTrue(expiredLocationSnapshot(start+1,start))
  assertFalse(expiredLocationSnapshot(start,start+120_000_000_000L));assertTrue(expiredLocationSnapshot(start,start+120_000_000_001L))
 }

}
