package my.maze.malaysia
import android.app.Instrumentation
import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.database.sqlite.SQLiteDatabase
import android.provider.Settings
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
/** Explicit, bounded proof controller in the isolated QA preview only. No arbitrary paths or requests. */
class ApproachProof:Instrumentation(){
 private var scope="fixture"
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);scope=arguments?.getString("scope") ?: "fixture";start()}
 override fun onStart(){Thread{
  val report=JSONObject().put("scope",scope);val rows=JSONArray();report.put("routes",rows)
  try{
   check(android.os.Build.FINGERPRINT.contains("generic")||android.os.Build.MODEL.contains("sdk"))
   check(Settings.Global.getInt(targetContext.contentResolver,Settings.Global.AIRPLANE_MODE_ON,0)==1)
   val cm=targetContext.getSystemService(android.net.ConnectivityManager::class.java)
   check(cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)!=true)
   runBlocking{
    if(scope=="fixture"){
     val file=java.io.File(targetContext.cacheDir,"approach-proof.sqlite")
     check(!file.exists()||file.delete())
     val db=SQLiteDatabase.openOrCreateDatabase(file,null)
     targetContext.assets.open("proof/fixture.sql").bufferedReader().useLines{lines->lines.filter{it.isNotBlank()}.forEach{db.execSQL(it)}}
     try{val queries=JSONObject(targetContext.assets.open("proof/scenarios.json").bufferedReader().readText()).getJSONArray("queries")
      val baseline=JSONArray(targetContext.assets.open("proof/graphhopper.json").bufferedReader().readText())
      for(i in 0 until queries.length()){val q=queries.getJSONObject(i);val a=q.getJSONArray("origin");val b=q.getJSONArray("destination");val from=Place("synthetic-a","Synthetic A","Not real roads",GeoPoint(a.getDouble(0),a.getDouble(1)));val to=Place("synthetic-b","Synthetic B","Not real roads",GeoPoint(b.getDouble(0),b.getDouble(1)))
       for(mode in RouteSearchMode.entries){val row=JSONObject().put("name",q.getString("name")).put("mode",mode.name);rows.put(row);val began=SystemClock.elapsedRealtime()
        val result=runCatching{MazeLocalRouter(db).route(from,to,mode).routes.single()};row.put("elapsed_ms",SystemClock.elapsedRealtime()-began)
        if(q.getString("status")=="route"){val route=result.getOrThrow();val gh=baseline.getJSONObject(i).getDouble("meters");check(kotlin.math.abs(route.meters-gh)<2);check(groundDistance(route.geometry.first(),from.point)<1&&groundDistance(route.geometry.last(),to.point)<1);row.put("meters",route.meters).put("gh_meters",gh).put("vertices",route.geometry.size)}
        else {check(result.isFailure);val message=result.exceptionOrNull()!!.message!!;check(message.contains(if(q.getString("status")=="ambiguous")"Ambiguous" else "No supported driving route"));row.put("expected_rejection",message)}
       }
      }
     }finally{db.close();check(file.delete())}
    }else if(scope=="state"){
     val owner=object:androidx.lifecycle.ViewModelStoreOwner{override val viewModelStore=androidx.lifecycle.ViewModelStore()}
     lateinit var vm:MazeViewModel
     runOnMainSync{vm=androidx.lifecycle.ViewModelProvider(owner,object:androidx.lifecycle.ViewModelProvider.Factory{
      @Suppress("UNCHECKED_CAST") override fun<T:androidx.lifecycle.ViewModel> create(modelClass:Class<T>):T=MazeViewModel(targetContext.applicationContext as android.app.Application,androidx.lifecycle.SavedStateHandle()) as T
     })[MazeViewModel::class.java]}
     fun awaitState(test:()->Boolean){val deadline=SystemClock.elapsedRealtime()+10000;while(true){var ok=false;runOnMainSync{ok=test()};if(ok)return;check(SystemClock.elapsedRealtime()<deadline){"State proof timed out"};Thread.sleep(25)}}
     try{
      awaitState{vm.state.packReady}
      runOnMainSync{vm.select(Place("current","Simulated coarse estimate","Synthetic state fixture",landmarks[0].point),true,5000f);vm.findRoutes();check(!vm.state.approachReviewActive&&!vm.state.routeBusy&&vm.state.message!!.contains("uncertainty"))}
      rows.put(JSONObject().put("check","5000m estimate requires manual start").put("passed",true))
      runOnMainSync{vm.select(landmarks[0],true);vm.reviewRoadApproach(true);check(vm.state.approachReviewActive&&vm.state.routeBusy);vm.dismissRoadChoices()}
      Thread.sleep(250);runOnMainSync{check(!vm.state.approachReviewActive&&vm.state.roadChoices.isEmpty()&&!vm.state.routeBusy)}
      rows.put(JSONObject().put("check","cancel in-flight road loading stays closed").put("passed",true))
      runOnMainSync{vm.reviewRoadApproach(true)};awaitState{vm.state.roadChoices.size>=2}
      runOnMainSync{vm.previewRoadApproach(vm.state.roadChoices[0]);vm.previewRoadApproach(vm.state.roadChoices[1]);vm.dismissRoadChoices()}
      Thread.sleep(250);runOnMainSync{check(vm.state.approachPreview==null&&vm.state.previewChoice==null&&!vm.state.approachReviewActive)}
      rows.put(JSONObject().put("check","late preview cannot reopen cancelled chooser").put("passed",true))
     }finally{runOnMainSync{owner.viewModelStore.clear()}}
    }else{check(scope in listOf("country","previews"));val pack=MazeLocalPack(targetContext)
     try{pack.open();report.put("pack",pack.summary)
      if(scope=="previews"){
       val router=MazeLocalRouter(pack.roads)
       for(place in listOf(landmarks[0],landmarks[1]))for(choice in router.approaches(place.point)){
        val began=SystemClock.elapsedRealtime();val preview=router.preview(choice,place.point)
        val g=SqliteApproachGraph(pack.roads).geometry(choice.geometry)
        check(preview.points.size in 2..2048&&preview.points.all{projectRoad(it,g.points).gap<.01})
        check(kotlin.math.abs(preview.gap-groundDistance(place.point,preview.snap))<.01)
        check(preview.arrows.all{it.bearing.isFinite()&&it.bearing in 0.0..360.0&&projectRoad(it.point,g.points).gap<.01})
        check(preview.oneWay==(g.directions.map{it.reverse}.distinct().size==1))
        rows.put(JSONObject().put("place",place.name).put("way",choice.way).put("geometry",choice.geometry).put("name",choice.name).put("gap",preview.gap).put("vertices",preview.points.size).put("arrows",preview.arrows.size).put("one_way",preview.oneWay).put("elapsed_ms",SystemClock.elapsedRealtime()-began))
       }
      }else{
      val pairs=listOf(landmarks[0] to landmarks[1],landmarks[1] to landmarks[0],landmarks[2] to landmarks[0],landmarks[5] to Place("gurney","Gurney","Penang",GeoPoint(5.4398311,100.3091867)),landmarks[6] to Place("viva","Vivacity","Kuching",GeoPoint(1.52674275,110.3692989)),landmarks[7] to Place("suria","Suria","Sabah",GeoPoint(5.98761945,116.07716655)),landmarks[0] to landmarks[5])
      val declared=listOf(754190230L to 754168118L,754168118L to 754190229L,24581627L to 754190229L,31285089L to 579785022L,637964718L to 129145719L,176131728L to 1344916159L,754190230L to 787243333L)
      for(mode in RouteSearchMode.entries)for((index,pair) in pairs.take(if(mode==RouteSearchMode.CAREFUL)6 else 7).withIndex()){
       val(a,b)=pair
       val row=JSONObject().put("from",a.name).put("to",b.name).put("mode",mode.name);rows.put(row);val began=SystemClock.elapsedRealtime()
       val router=MazeLocalRouter(pack.roads)
       val choicesA=router.approaches(a.point);val choicesB=router.approaches(b.point)
       val startChoice=choicesA.firstOrNull{it.way==declared[index].first}
       val selected=choicesB.firstOrNull{it.way==declared[index].second}
       if(startChoice==null||selected==null){row.put("error","Declared mapped approach absent from offered choices").put("start_choices",JSONArray(choicesA.map{it.way})).put("end_choices",JSONArray(choicesB.map{it.way}));continue}
       row.put("explicit_test_approach",selected.name).put("approach_way",selected.way).put("approach_gap",selected.gap).put("start_approach",startChoice.name).put("start_way",startChoice.way).put("start_gap",startChoice.gap)
       val result=runCatching{router.route(a,b,mode,selected.geometry,startChoice.geometry).routes.single()}
       if(result.isFailure){row.put("error",result.exceptionOrNull()!!.message);continue}
       val route=result.getOrThrow();row.put("elapsed_ms",SystemClock.elapsedRealtime()-began).put("meters",route.meters).put("seconds",route.seconds).put("start",JSONArray(listOf(route.geometry.first().lat,route.geometry.first().lon))).put("end",JSONArray(listOf(route.geometry.last().lat,route.geometry.last().lon))).put("summary",route.summary).put("vertices",route.geometry.size)
       if(a.id=="sentral"&&b.id=="klcc")check(route.meters<8500)
      }
      }
     }finally{pack.close()}
    }
   };report.put("passed",(0 until rows.length()).none{rows.getJSONObject(it).has("error")})
  }catch(e:Throwable){report.put("passed",false).put("error",e.stackTraceToString())}
  finish(if(report.optBoolean("passed"))Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{putString("stream",report.toString(2))})
 }.start()}
}
