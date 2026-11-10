package my.maze.malaysia

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One map, one highlighted option, and a separate deliberate confirmation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun MazeRoadChooser(
 state:MazeState,wide:Boolean,maxPanel:androidx.compose.ui.unit.Dp,rendered:ApproachPreview?,
 onPreview:(RoadChoice)->Unit,onConfirm:(Boolean)->Unit,onCancel:()->Unit,
 onHeader:(Int)->Unit,onPanel:(Int,Int)->Unit
){
 val colors=MaterialTheme.colorScheme;val choice=state.previewChoice;val preview=state.approachPreview
 val bodyScroll=rememberScrollState()
 LaunchedEffect(choice?.geometry){bodyScroll.scrollTo(0)}
 val index=state.roadChoices.indexOf(choice);
 val gpsStart=state.reviewingOrigin&&state.origin.id=="current"
 var roadChecked by remember(choice?.geometry,placeKey(state)){mutableStateOf(false)};val place=if(state.reviewingOrigin)state.origin else state.destination
 var listOpen by remember{mutableStateOf(false)};var details by remember{mutableStateOf(false)}
 BackHandler{if(listOpen)listOpen=false else onCancel()}
 BoxWithConstraints(Modifier.fillMaxSize()){
  val chooserWidth=(maxWidth*.56f).coerceAtMost(370.dp)
  val headingWidth=(maxWidth-chooserWidth-24.dp).coerceAtMost(310.dp)
  Surface(Modifier.align(Alignment.TopStart).then(if(wide)Modifier.width(headingWidth)else Modifier.fillMaxWidth()).onSizeChanged{onHeader(it.height)}.statusBarsPadding().padding(12.dp),shape=RoundedCornerShape(20.dp),shadowElevation=2.dp,color=colors.surfaceContainerLowest){
   Row(Modifier.padding(start=18.dp,end=8.dp,top=4.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){
     Text(if(state.reviewingOrigin)"START · 1 OF 2" else "ARRIVAL · 2 OF 2",style=MaterialTheme.typography.labelMedium,color=colors.primary,maxLines=1,overflow=TextOverflow.Ellipsis)
     Text(place.name,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
    }
    TextButton(onClick=onCancel,modifier=Modifier.heightIn(min=48.dp)){Text("Cancel")}
   }
  }
  Surface(Modifier.align(if(wide)Alignment.BottomEnd else Alignment.BottomCenter).then(if(wide)Modifier.width(chooserWidth)else Modifier.fillMaxWidth()).padding(12.dp).onSizeChanged{onPanel(it.height,it.width)},shape=RoundedCornerShape(20.dp),shadowElevation=3.dp,color=colors.surfaceContainerLowest){
   Column(Modifier.navigationBarsPadding().heightIn(max=maxPanel)){
    Column(Modifier.weight(1f,fill=false).verticalScroll(bodyScroll).padding(start=20.dp,end=20.dp,top=16.dp,bottom=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
      Text("ROAD ON MAP",style=MaterialTheme.typography.labelMedium,color=colors.primary,modifier=Modifier.weight(1f))
      TextButton(onClick={listOpen=true},enabled=state.roadChoices.isNotEmpty(),modifier=Modifier.heightIn(min=48.dp)){Text("All roads")}
     }
     Text(choice?.let{approachDisplayName(it.name)} ?: if(state.routeBusy)"Finding nearby roads…" else "Choose your starting road",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold,modifier=Modifier.semantics{liveRegion=LiveRegionMode.Polite})
     if(state.approachPreviewBusy||state.routeBusy){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Highlighting this segment…",style=MaterialTheme.typography.bodyMedium)}
     state.approachPreviewError?.let{Text(it,color=colors.error,style=MaterialTheme.typography.bodyMedium);TextButton(onClick={choice?.let(onPreview)}){Text("Retry highlight")}}
     if(preview!=null){
      Text("${routeDistanceLabel(preview.gap)} from place · straight-line gap",style=MaterialTheme.typography.bodyLarge)
      Surface(color=colors.secondaryContainer,shape=RoundedCornerShape(8.dp)){
       Text(if(preview.oneWay)"Mapped one-way · follow the arrows" else "Mapped two-way road",Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.labelLarge)
      }
     }
     if(gpsStart){
      Text(if(state.roadEstimateOverlap>1)"GPS overlaps ${state.roadEstimateOverlap} mapped roads" else "GPS cannot confirm your carriageway",style=MaterialTheme.typography.titleMedium,color=colors.primary)
      Text(state.origin.area+". Check the road side and direction. If unsure, cancel and choose a known start.",style=MaterialTheme.typography.bodyMedium)
      if(preview!=null)Row(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value=roadChecked,role=Role.Checkbox,onValueChange={roadChecked=it}),verticalAlignment=Alignment.CenterVertically){Checkbox(checked=roadChecked,onCheckedChange=null);Text("I checked this road and direction",Modifier.weight(1f))}
     }
     if(state.roadChoiceLimited)Text("Showing the nearest 20 mapped roads. Other roads may also be nearby.",style=MaterialTheme.typography.bodySmall)
     Text(if(gpsStart&&preview==null)"The dashed circle shows location uncertainty. Choose a road to see its separate ROAD marker." else if(gpsStart)"Dashed circle: location estimate. ROAD ring: selected snap. A nearby road is not verified entry or a confirmed connection." else "The ROAD ring marks the road snap. The place stays separate. A nearby road is not verified entry or a confirmed connection.",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
     TextButton(onClick={details=true},modifier=Modifier.heightIn(min=48.dp)){Text("Road details")}
    }
    HorizontalDivider(Modifier.padding(horizontal=20.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
     IconButton(onClick={onPreview(state.roadChoices[index-1])},enabled=index>0,modifier=Modifier.size(48.dp)){Icon(painterResource(R.drawable.ic_road_previous),"Previous road")}
     Text(if(state.roadChoices.isEmpty())"Finding roads" else if(index<0)"${state.roadChoices.size} road options" else "Option ${index+1} of ${state.roadChoices.size}",style=MaterialTheme.typography.labelLarge)
     IconButton(onClick={onPreview(state.roadChoices[index+1])},enabled=index<state.roadChoices.lastIndex,modifier=Modifier.size(48.dp)){Icon(painterResource(R.drawable.ic_road_next),"Next road")}
    }
    Button(onClick={onConfirm(roadChecked)},enabled=(!gpsStart||roadChecked)&&preview!=null&&rendered==preview&&!state.approachPreviewBusy&&state.approachPreviewError==null,modifier=Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,bottom=12.dp).heightIn(min=52.dp),shape=RoundedCornerShape(14.dp)){
     Text(if(state.reviewingOrigin)"Use starting road" else "Use arrival road")
    }
    Text("© OpenStreetMap contributors · ODbL",Modifier.padding(start=20.dp,end=20.dp,bottom=8.dp),style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
   }
  }
 }
 if(listOpen)ModalBottomSheet(onDismissRequest={listOpen=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)){
  Column(Modifier.fillMaxWidth().heightIn(max=maxPanel*1.5f).verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding()){
   Text("Preview a road",style=MaterialTheme.typography.headlineSmall)
   Text(if(gpsStart)"No road is preselected. GPS can overlap opposite carriageways. Tap a road to inspect its mapped side and arrows." else "Tap an option to highlight it. Confirm after checking the map.",Modifier.padding(vertical=12.dp))
   state.roadChoices.forEachIndexed{i,item->
    Surface(Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(12.dp),color=if(item==choice)colors.secondaryContainer else colors.surfaceContainerLowest,border=if(item==choice)BorderStroke(1.dp,colors.primary) else null){
     ListItem(colors=ListItemDefaults.colors(containerColor=if(item==choice)colors.secondaryContainer else colors.surfaceContainerLowest),headlineContent={Text("${i+1}. ${approachDisplayName(item.name)}")},supportingContent={Text("${routeDistanceLabel(item.gap)} from place"+(if(item==choice)" · selected preview" else ""))},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).selectable(selected=item==choice,role=Role.RadioButton,onClick={listOpen=false;onPreview(item)}))
    }
   }
  }
 }
 if(details)AlertDialog(onDismissRequest={details=false},title={Text("Mapped road details")},text={Text("OpenStreetMap way ${choice?.way ?: "unavailable"}. Highlight shows up to 400 m of the selected segment. Directions come from the offline map; turns, access and today's conditions still need checking. No connection across the gap is drawn. Location circles are estimates, not guaranteed bounds.")},confirmButton={TextButton(onClick={details=false}){Text("Done")}})
}

private fun placeKey(state:MazeState)=if(state.reviewingOrigin)state.origin.point else state.destination.point
