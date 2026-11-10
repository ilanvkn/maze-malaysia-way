package my.maze.malaysia

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.time.LocalDate

/** Uses the enclosing bounded sheet's scroll owner. No automatic uploads or route requests. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun MazeGatePilot(model:MazeGateModel,packReady:Boolean,onDestination:(Place)->Unit){
 var purposeName by rememberSaveable{mutableStateOf(GatePurpose.VISITOR.name)}
 val purpose=GatePurpose.valueOf(purposeName)
 var routeGate by remember{mutableStateOf<PilotGate?>(null)}
 var remove by remember{mutableStateOf(false)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{model.import(it)}}
 LifecycleEventEffect(Lifecycle.Event.ON_RESUME){model.refreshDate()}
 Text("Gate pilot",style=MaterialTheme.typography.headlineSmall)
 Text("Find the appropriate entrance",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=8.dp))
 Text("Experimental · no participating site has been verified by Maze. Checks in imported files are source-reported. No account, GPS collection or automatic feedback upload.",Modifier.padding(vertical=12.dp))
 if(model.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
 model.message?.let{Text(it,Modifier.padding(vertical=8.dp).semantics{liveRegion=LiveRegionMode.Polite},style=MaterialTheme.typography.bodySmall)}
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  OutlinedButton(onClick={model.demo()},enabled=!model.busy){Text("Try test scenarios")}
  OutlinedButton(onClick={picker.launch(arrayOf("application/json","text/plain","application/octet-stream"))},enabled=!model.busy){Text("Import pilot JSON")}
 }
 val data=model.dataset
 if(data==null)Text("Start with fictional scenarios, or import permission-cleared records. Nothing is labelled as a field-tested pilot.",Modifier.padding(vertical=12.dp))
 else{
  HorizontalDivider(Modifier.padding(vertical=12.dp))
  Text(if(data.demo)"FICTIONAL SCENARIOS · ROUTING DISABLED" else "IMPORTED PILOT · SOURCE CLAIMS",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
  Text(data.site,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=8.dp))
  Text("Source: ${data.source}",style=MaterialTheme.typography.bodySmall)
  Text("Purpose of visit",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=16.dp))
  FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){GatePurpose.entries.forEach{p->FilterChip(selected=p==purpose,onClick={purposeName=p.name},label={Text(p.label)})}}
  Text("Records older than 30 days require another check. This pilot threshold uses the device date; it does not establish today's opening hours or your permission.",style=MaterialTheme.typography.bodySmall)
  for(g in data.gates){
   val assessment=assessGate(g,purpose,model.today)
   Card(Modifier.fillMaxWidth().padding(vertical=8.dp)){
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Text(g.name,style=MaterialTheme.typography.titleMedium)
     Text(assessment.label,color=if(assessment.candidate)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
     Text("Record ${g.id} · access: ${g.access} · motorcar: ${g.motorcar} · reported status: ${g.availability}",style=MaterialTheme.typography.bodySmall)
     Text(g.instructions.ifBlank{"No operating instructions supplied."})
     Text("${if(data.demo)"Simulated check" else "Reported check"}: ${g.checkedOn ?: "not supplied"}\nChecker: ${g.checkedBy.ifBlank{"not supplied"}}\nEvidence: ${g.evidence.ifBlank{"not supplied"}}",style=MaterialTheme.typography.bodySmall)
     if(data.demo)Text("Fictional gate · no location or route",style=MaterialTheme.typography.labelMedium)
     else Button(onClick={routeGate=g},enabled=!model.busy&&packReady&&assessment.candidate&&g.point!=null,modifier=Modifier.heightIn(min=48.dp)){Text("Review road preview")}
    }
   }
  }
  if(!packReady&&!data.demo)Text("Import the matching Malaysia map pack before previewing nearby roads.")
  Text("For feedback: note the gate ID, visit purpose, expected access and what confused you. Field arrival success, delays and willingness to pay are not measured by this prototype.",Modifier.padding(vertical=12.dp))
  TextButton(onClick={remove=true},enabled=!model.busy){Text("Remove pilot records")}
 }
 model.pending?.let{pending->AlertDialog(onDismissRequest={if(!model.busy)model.dismissImport()},title={Text("Review pilot import")},text={Text("${pending.site} · ${pending.gates.size} gates\nSource: ${pending.source}\n\nThis replaces the stored pilot file. Imported checks are not independently verified by Maze. Confirm you have permission to store and use these records. They stay on this device; explicitly saved trips are separate.")},confirmButton={TextButton(onClick=model::acceptImport,enabled=!model.busy){Text("Keep on this device")}},dismissButton={TextButton(onClick=model::dismissImport,enabled=!model.busy){Text("Cancel import")}})}
 routeGate?.let{g->AlertDialog(onDismissRequest={routeGate=null},title={Text("A nearby road, not verified gate access")},text={Text("Confirm the operator's current hours and your right of entry. This is a generic car preview, not truck routing. Maze projects to the nearest supported road segment within 350 m; it has not verified a road connection to this gate. Selecting it does not start navigation.")},confirmButton={TextButton(onClick={
  val current=model.dataset
  if(current!=null&&!current.demo&&current.gates.contains(g)&&assessGate(g,purpose,LocalDate.now()).candidate&&g.point!=null&&packReady){onDestination(pilotDestination(current,g))}
  routeGate=null
 }){Text("Use as destination")}},dismissButton={TextButton(onClick={routeGate=null}){Text("Cancel preview")}})}
 if(remove)AlertDialog(onDismissRequest={remove=false},title={Text("Remove pilot records?")},text={Text("Removes the imported pilot file from this device. Your current map destination and saved trips are kept; they are managed separately.")},confirmButton={TextButton(onClick={model.clear();remove=false}){Text("Remove records")}},dismissButton={TextButton(onClick={remove=false}){Text("Keep records")}})
}
