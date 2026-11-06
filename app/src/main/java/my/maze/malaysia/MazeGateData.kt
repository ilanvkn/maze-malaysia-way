package my.maze.malaysia

import org.json.JSONObject
import java.time.LocalDate

enum class GatePurpose(val label:String){VISITOR("Visitor"),DELIVERY("Delivery"),STAFF("Staff")}
internal data class PilotGate(val id:String,val name:String,val access:String,val availability:String,val instructions:String,val checkedOn:LocalDate?,val checkedBy:String,val evidence:String,val point:GeoPoint?,val motorcar:String="unknown")
internal data class GateDataset(val site:String,val source:String,val demo:Boolean,val gates:List<PilotGate>)
internal data class GateAssessment(val label:String,val candidate:Boolean)
internal const val GATE_MAX_BYTES=65536
/** Imported assertions are not authentication or a guarantee of legal/physical access. */
internal fun assessGate(g:PilotGate,purpose:GatePurpose,today:LocalDate):GateAssessment=when{
 g.access=="restricted"->GateAssessment("Restricted · permission required",false)
 g.availability=="closed"->GateAssessment("Reported closed",false)
 g.checkedOn==null||g.checkedBy.isBlank()||g.evidence.isBlank()->GateAssessment("Check evidence missing",false)
 g.checkedOn>today->GateAssessment("Check date is in the future",false)
 g.checkedOn.plusDays(30)<today->GateAssessment("Check is older than 30 days",false)
 g.availability!="open"->GateAssessment("Operating status unknown",false)
 g.motorcar!="yes"->GateAssessment(if(g.motorcar=="no")"Motorcar access denied" else "Motorcar access unconfirmed",false)
 g.access=="unknown"->GateAssessment("Access type unknown",false)
 g.access=="staff"->GateAssessment("Staff permission must be confirmed",false)
 g.access=="delivery"&&purpose!=GatePurpose.DELIVERY->GateAssessment("Delivery access only",false)
 else->GateAssessment("Candidate · confirm hours and permission",true)
}
internal fun parseGateDataset(text:String,today:LocalDate):GateDataset {
 require(text.toByteArray(Charsets.UTF_8).size<=GATE_MAX_BYTES){"Pilot file exceeds 64 KiB."}
 var depth=0;var quoted=false;var escaped=false
 for(c in text){
  if(quoted){if(escaped)escaped=false else if(c=='\\')escaped=true else if(c=='"')quoted=false}
  else when(c){'"'->quoted=true;'[','{'->{depth++;require(depth<=8){"Pilot JSON is too deeply nested."}};']','}'->{depth--;require(depth>=0)}}
 }
 require(depth==0&&!quoted){"Incomplete pilot JSON."}
 val tokener=org.json.JSONTokener(text);val value=tokener.nextValue();require(value is JSONObject&&tokener.nextClean()=='\u0000'){"Expected one JSON object."};val root=value
 fun JSONObject.exact(keys:Set<String>){require(keys().asSequence().toSet()==keys){"Pilot file has missing or unsupported fields."}}
 fun JSONObject.string(key:String,max:Int=400):String{val v=get(key);require(v is String&&v.length<=max&&v.none{it.isISOControl()}){"Invalid $key."};return v.trim()}
 root.exact(setOf("schema","site","source","demo","permission","gates"))
 require(root.get("schema") is Number&&root.getDouble("schema")==1.0){"Unsupported pilot schema."}
 require(root.get("demo") is Boolean){"demo must be true or false."};val demo=root.getBoolean("demo")
 val permission=root.string("permission");require(demo||permission=="approved_for_pilot"){"Real records require permission approval before import."}
 val site=root.string("site",100);val source=root.string("source");require(site.isNotBlank()&&source.isNotBlank()){"Site and source are required."}
 val array=root.getJSONArray("gates");require(array.length() in 1..50){"Import 1–50 gates."};val ids=mutableSetOf<String>()
 val gates=(0 until array.length()).map{i->
  val g=array.getJSONObject(i);g.exact(setOf("id","name","access","availability","instructions","checked_on","checked_by","evidence","lat","lon","motorcar"))
  val id=g.string("id",40);require(Regex("[A-Za-z0-9_-]{1,40}").matches(id)&&ids.add(id)){"Gate IDs must be unique."}
  val name=g.string("name",100);require(name.isNotBlank()){"Gate name is required."}
  val access=g.string("access");require(access in setOf("public","delivery","staff","restricted","unknown")){"Unknown access type."}
  val availability=g.string("availability");require(availability in setOf("open","closed","unknown")){"Unknown availability."}
  val date=g.string("checked_on",10).takeIf{it.isNotEmpty()}?.let{LocalDate.parse(it)}
  require(date==null||date<=today){"A check date cannot be in the future."}
  val by=g.string("checked_by",100);val evidence=g.string("evidence")
  require(date==null||(by.isNotBlank()&&evidence.isNotBlank())){"Dated checks require a checker and evidence."}
  val point=if(g.isNull("lat")&&g.isNull("lon"))null else{
   require(!demo&&g.get("lat") is Number&&g.get("lon") is Number){"Demo gates cannot have coordinates; real coordinates must be numbers."}
   val lat=g.getDouble("lat");val lon=g.getDouble("lon");require(lat.isFinite()&&lon.isFinite()&&lat in .8..7.8&&lon in 99.0..120.0){"Gate is outside the supported regional bounds."};GeoPoint(lat,lon)
  }
  require(demo||point!=null){"Real gate records require coordinates."}
  val motorcar=g.string("motorcar");require(motorcar in setOf("yes","no","unknown")){"Unknown motorcar access."}
  PilotGate(id,name,access,availability,g.string("instructions"),date,by,evidence,point,motorcar)
 }
 return GateDataset(site,source,demo,gates)
}
internal fun demoGateDataset(today:LocalDate)=GateDataset("Fictional industrial park","Synthetic usability scenarios · no real site or field checks",true,listOf(
 PilotGate("public","Main visitor gate","public","open","Scenario: visitors use this entrance.",today,"Simulated checker","Fictional observation",null,"yes"),
 PilotGate("loading","Delivery gate","delivery","open","Scenario: report to the loading reception.",today,"Simulated checker","Fictional observation",null,"yes"),
 PilotGate("staff","Staff gate","staff","open","Scenario: staff credential required.",today,"Simulated checker","Fictional observation",null,"yes"),
 PilotGate("closed","Closed gate","public","closed","Scenario: closure overrides an earlier check.",today,"Simulated checker","Fictional observation",null,"yes"),
 PilotGate("old","Old access record","public","open","Scenario: arrange a fresh check.",today.minusDays(31),"Simulated checker","Fictional observation",null,"yes"),
 PilotGate("unknown","Unconfirmed gate","unknown","unknown","Scenario: ask the operator for evidence.",null,"","",null)
))

internal fun pilotDestination(data:GateDataset,g:PilotGate):Place {
 require(!data.demo&&g in data.gates&&g.point!=null)
 val identity=java.security.MessageDigest.getInstance("SHA-256").digest((data.site+"\u0000"+data.source+"\u0000"+g.id).toByteArray()).joinToString(""){"%02x".format(it)}
 return Place("pilot-$identity",g.name,"Pilot source: ${data.source} · gate approach unverified",g.point)
}
