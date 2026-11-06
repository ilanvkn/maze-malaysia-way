package my.maze.malaysia

import org.json.JSONArray
import org.json.JSONObject

/** Original Shortbread cartography. Every feature comes from the installed OSM tiles. */
internal fun mazeMapStyle(base:String,night:Boolean):String {
 fun a(vararg values:Any):JSONArray=JSONArray(values.toList())
 val paper=if(night)"#111A2A" else "#F4F5F9"
 val ink=if(night)"#E1E8FA" else "#35415B"
 val quiet=if(night)"#A8B6CE" else "#5B6981"
 val water=if(night)"#1B3A53" else "#BEDCEB"
 val layers=JSONArray()
 fun add(id:String,type:String,source:String?=null,paint:JSONObject=JSONObject(),layout:JSONObject?=null,filter:JSONArray?=null,min:Int=0){
  layers.put(JSONObject().put("id",id).put("type",type).put("minzoom",min).put("paint",paint).apply{if(source!=null){put("source","region");put("source-layer",source)};if(layout!=null)put("layout",layout);if(filter!=null)put("filter",filter)})
 }
 fun fill(color:String)=JSONObject().put("fill-color",color)
 fun line(color:String,width:Any)=JSONObject().put("line-color",color).put("line-width",width)
 fun zoom(vararg stops:Any)=a("interpolate",a("linear"),a("zoom"),*stops)
 val rounded=JSONObject().put("line-cap","round").put("line-join","round")
 add("background","background",paint=JSONObject().put("background-color",paper))
 add("land","fill","land",fill(if(night)"#1A263B" else "#E9EDF4"))
 add("green-spaces","fill","land",fill(if(night)"#213A36" else "#DCE8DE"),filter=a("in","kind","forest","wood","grass","grassland","meadow","park","garden","scrub","heath","recreation_ground","village_green","nature_reserve","orchard","farmland"))
 add("sites","fill","sites",fill(if(night)"#1C2638" else "#EAEDF4"))
 for(source in listOf("ocean","water_polygons"))add(source,"fill",source,fill(water))
 add("water-lines","line","water_lines",line(water,zoom(10,.5,16,3)),min=10)
 add("buildings","fill","buildings",fill(if(night)"#27344B" else "#DDE2EC").put("fill-outline-color",if(night)"#303F57" else "#CFD6E3"),min=14)
 add("pedestrian-areas","fill","street_polygons",fill(if(night)"#263044" else "#E8E6EC"),min=14)
 val tiers=listOf(
  Triple(listOf("service","living_street","unclassified","residential"),13,listOf(12,.4,14,1.4,16,4.0,19,12)),
  Triple(listOf("tertiary","tertiary_link"),10,listOf(10,.5,14,2.0,16,5.0,19,14)),
  Triple(listOf("secondary","secondary_link"),8,listOf(8,.4,14,2.5,16,6.0,19,16)),
  Triple(listOf("primary","primary_link","trunk","trunk_link"),6,listOf(6,.4,12,1.4,14,3.2,16,7.0,19,18)),
  Triple(listOf("motorway","motorway_link"),5,listOf(5,.5,12,1.8,14,3.8,16,8.0,19,20)))
 for((i,tier) in tiers.withIndex()){
  val (kinds,min,stops)=tier
  val filter=a("all",a("!=","rail",true),a("in","kind",*kinds.toTypedArray()))
  val width=zoom(*stops.toTypedArray())
  add("road-$i-casing","line","streets",line(if(night)"#131D30" else "#BBC5D6",a("+",width,1.4)),rounded,filter,min)
  add("road-$i","line","streets",line(if(i>=3){if(night)"#9DAED3" else "#FFFDF7"}else{if(night)"#53627D" else "#FFFFFF"},width),rounded,filter,min)
 }
 add("rail","line","streets",line(if(night)"#75819A" else "#9CA8BA",1.0).put("line-dasharray",a(3,2)),filter=a("==","rail",true),min=12)
 add("paths","line","streets",line(if(night)"#728B91" else "#94ABA6",.8).put("line-dasharray",a(1,2)),filter=a("in","kind","path","footway","steps","pedestrian","cycleway"),min=15)
 add("boundaries","line","boundaries",line(if(night)"#52617C" else "#AEB8CB",.6).put("line-dasharray",a(4,3)))
 fun labels(id:String,source:String,size:Any,min:Int,color:String=ink,filter:JSONArray?=null,along:Boolean=false){
  val layout=JSONObject().put("text-field",a("get","name")).put("text-font",a("Noto Sans Regular")).put("text-size",size).put("text-max-width",8).put("text-padding",8).put("text-optional",true)
  if(along)layout.put("symbol-placement","line").put("symbol-spacing",350)
  add(id,"symbol",source,JSONObject().put("text-color",color).put("text-halo-color",paper).put("text-halo-width",1.5),layout,filter,min)
 }
 labels("cities","place_labels",zoom(5,12,12,18),0,filter=a("in","kind","city","town"))
 labels("districts","place_labels",zoom(12,11,16,14),12,quiet,a("!in","kind","city","town"))
 labels("road-names","street_labels",zoom(12,10,17,12),12,quiet,a("in","kind","motorway","trunk","primary","secondary","tertiary","residential","unclassified"),true)
 labels("stations","public_transport",11,14,filter=a("in","kind","station","halt"))
 labels("landmarks","pois",11,15,filter=a("any",a("in","amenity","hospital","university","place_of_worship"),a("in","tourism","museum","attraction")))
 labels("local-places","pois",11,17,quiet,a("all",a("!in","amenity","hospital","university","place_of_worship"),a("!in","tourism","museum","attraction")))
 return JSONObject().put("version",8).put("glyphs","$base/glyphs/{fontstack}/{range}.pbf").put("sources",JSONObject().put("region",JSONObject().put("type","vector").put("tiles",a("$base/tiles/{z}/{x}/{y}.pbf")).put("minzoom",0).put("maxzoom",14).put("attribution","© OpenStreetMap contributors · ODbL 1.0 · Geofabrik Shortbread"))).put("layers",layers).toString()
}
