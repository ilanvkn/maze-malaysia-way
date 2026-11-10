package my.maze.malaysia

import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import org.maplibre.android.style.expressions.Expression

@Composable fun MazeMap(state:MazeState,modifier:Modifier=Modifier,topInset:Int=0,bottomInset:Int=0,rightInset:Int=0,styleUri:String=DAY_STYLE,onApproachApplied:(ApproachPreview?)->Unit={},onReady:(Boolean)->Unit) {
    val choosing=state.approachReviewActive
    val preview=if(choosing)state.approachPreview else null
    val route=if(choosing)null else state.routes.getOrNull(state.selectedRoute)
    val context=LocalContext.current; val lifecycle=LocalLifecycleOwner.current.lifecycle
    val mapView=remember(styleUri) { require(java.net.URI(styleUri).host=="127.0.0.1"){"Offline map requires the local pack"};MapLibre.getInstance(context);MapLibre.setConnected(true);MapView(context).apply { onCreate(Bundle()) } }
    var map by remember(styleUri) { mutableStateOf<MapLibreMap?>(null) }
    var ready by remember(styleUri) { mutableStateOf(false) }
    DisposableEffect(mapView,lifecycle) {
        onReady(false)
        var started=false;var resumed=false
        val observer=LifecycleEventObserver { _,event -> when(event) {
            Lifecycle.Event.ON_START->{if(!started){mapView.onStart();started=true}};Lifecycle.Event.ON_RESUME->{if(!resumed){mapView.onResume();resumed=true}}
            Lifecycle.Event.ON_PAUSE->{if(resumed){mapView.onPause();resumed=false}};Lifecycle.Event.ON_STOP->{if(started){mapView.onStop();started=false}}
            else -> Unit
        } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer);if(resumed)mapView.onPause();if(started)mapView.onStop();mapView.onDestroy() }
    }
    key(styleUri) { AndroidView(factory={mapView.apply { getMapAsync { m ->
        map=m;m.uiSettings.isLogoEnabled=false;m.uiSettings.isAttributionEnabled=false
        m.cameraPosition=CameraPosition.Builder().target(LatLng(3.1465,101.699)).zoom(12.5).build()
        m.setStyle(Style.Builder().fromUri(styleUri)) { style ->
            style.addSource(GeoJsonSource("maze-road"))
            val lastRoad=style.layers.indexOfLast{it is LineLayer || it is FillLayer}
            val firstLabel=style.layers.drop(lastRoad+1).firstOrNull{it is SymbolLayer}?.id
            val casing=LineLayer("maze-road-casing","maze-road").withProperties(lineColor("#FFFFFF"),lineWidth(10f),lineCap("round"),lineJoin("round"))
            val line=LineLayer("maze-road-line","maze-road").withProperties(lineColor("#354FD6"),lineWidth(6f),lineCap("round"),lineJoin("round"))
            if(firstLabel!=null){style.addLayerBelow(casing,firstLabel);style.addLayerBelow(line,firstLabel)}else{style.addLayer(casing);style.addLayer(line)}
            style.addSource(GeoJsonSource("maze-access"));style.addLayer(CircleLayer("maze-access-layer","maze-access").withProperties(circleColor("#6F819D"),circleRadius(4f),circleStrokeColor("#FFFFFF"),circleStrokeWidth(1.5f)))
            style.addSource(GeoJsonSource("maze-points"))
            style.addLayer(CircleLayer("maze-points-layer","maze-points").withProperties(circleColor("#B65330"),circleRadius(9f),circleStrokeColor("#FFFFFF"),circleStrokeWidth(2f)))
            style.addSource(GeoJsonSource("maze-origin"))
            style.addLayer(CircleLayer("maze-origin-layer","maze-origin").withProperties(circleColor("#354FD6"),circleRadius(9f),circleStrokeColor("#FFFFFF"),circleStrokeWidth(2f)))
            for((source,label) in listOf("maze-origin" to "A","maze-points" to "B"))style.addLayer(SymbolLayer("$source-label",source).withProperties(textField(label),textSize(11f),textFont(arrayOf("Noto Sans Regular")),textColor("#FFFFFF"),textAllowOverlap(true),textIgnorePlacement(true)))
            val accent=if(styleUri.endsWith("night.json"))"#80E0DC" else "#096D78"
            style.addSource(GeoJsonSource("maze-uncertainty"))
            style.addLayerBelow(FillLayer("maze-uncertainty-fill","maze-uncertainty").withProperties(fillColor(accent),fillOpacity(.10f)),"maze-points-layer")
            style.addLayerBelow(LineLayer("maze-uncertainty-edge","maze-uncertainty").withProperties(lineColor(accent),lineWidth(1.5f),lineDasharray(arrayOf(3f,3f))),"maze-points-layer")
            style.addSource(GeoJsonSource("maze-approach"))
            style.addLayerBelow(LineLayer("maze-approach-casing","maze-approach").withProperties(lineColor(if(styleUri.endsWith("night.json"))"#101B2C" else "#FFFFFF"),lineWidth(13f),lineCap("round"),lineJoin("round")),"maze-points-layer")
            style.addLayerBelow(LineLayer("maze-approach-line","maze-approach").withProperties(lineColor(accent),lineWidth(7f),lineCap("round"),lineJoin("round")),"maze-points-layer")
            style.addSource(GeoJsonSource("maze-approach-arrows"))
            style.addLayerBelow(SymbolLayer("maze-approach-arrows-layer","maze-approach-arrows").withProperties(textField("▲"),textFont(arrayOf("Noto Sans Regular")),textSize(16f),textColor(if(styleUri.endsWith("night.json"))"#101B2C" else "#FFFFFF"),textRotate(Expression.get("bearing")),textRotationAlignment("map"),textAllowOverlap(false),textIgnorePlacement(false)),"maze-points-layer")
            style.addSource(GeoJsonSource("maze-road-snap"))
            style.addLayer(CircleLayer("maze-road-snap-ring","maze-road-snap").withProperties(circleColor("#FFFFFF"),circleRadius(10f),circleOpacity(0f),circleStrokeColor(accent),circleStrokeWidth(3f)))
            style.addLayer(SymbolLayer("maze-road-snap-label","maze-road-snap").withProperties(textField("ROAD"),textFont(arrayOf("Noto Sans Regular")),textSize(11f),textColor(accent),textHaloColor(if(styleUri.endsWith("night.json"))"#101B2C" else "#FFFFFF"),textHaloWidth(2f),textOffset(arrayOf(0f,2f)),textAllowOverlap(true),textIgnorePlacement(true)))
            ready=true;onReady(true)
        }
    } }},modifier=modifier.semantics{contentDescription=if(choosing&&preview!=null)"Road preview: ${approachDisplayName(preview.choice.name)}. ${routeDistanceLabel(preview.gap)} from place. "+(if(preview.oneWay)"Mapped one-way road." else "Mapped two-way road.") else if(choosing)"Loading road preview" else "Offline Malaysia map"}) }
    LaunchedEffect(map,styleUri,ready,state.accessPoints){if(ready)map?.style?.getSourceAs<GeoJsonSource>("maze-access")?.setGeoJson(FeatureCollection.fromFeatures(state.accessPoints.map{Feature.fromGeometry(Point.fromLngLat(it.point.lon,it.point.lat))}))}
    LaunchedEffect(map,styleUri,ready,route,state.origin,state.destination,choosing,preview,state.originUncertaintyMeters,topInset,bottomInset,rightInset) {
        val m=map ?: return@LaunchedEffect;if(!ready)return@LaunchedEffect
        val style=m.style ?: return@LaunchedEffect
        val empty=FeatureCollection.fromFeatures(emptyArray<Feature>())
        fun line(points:List<GeoPoint>)=if(points.size>=2)FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(points.map{Point.fromLngLat(it.lon,it.lat)}))) else empty
        style.getSourceAs<GeoJsonSource>("maze-approach")?.setGeoJson(line(preview?.points.orEmpty()))
        style.getSourceAs<GeoJsonSource>("maze-road-snap")?.setGeoJson(preview?.let{FeatureCollection.fromFeature(Feature.fromGeometry(Point.fromLngLat(it.snap.lon,it.snap.lat)))} ?: empty)
        style.getSourceAs<GeoJsonSource>("maze-approach-arrows")?.setGeoJson(FeatureCollection.fromFeatures(preview?.arrows.orEmpty().map{Feature.fromGeometry(Point.fromLngLat(it.point.lon,it.point.lat)).apply{addNumberProperty("bearing",it.bearing)}}))
        val ring=if((!choosing||state.reviewingOrigin)&&state.origin.id=="current")accuracyRing(state.origin.point,state.originUncertaintyMeters) else emptyList()
        style.getSourceAs<GeoJsonSource>("maze-uncertainty")?.setGeoJson(if(ring.isNotEmpty())FeatureCollection.fromFeature(Feature.fromGeometry(Polygon.fromLngLats(listOf(ring.map{Point.fromLngLat(it.lon,it.lat)})))) else empty)
        val coordinates=route?.geometry?.map{Point.fromLngLat(it.lon,it.lat)} ?: emptyList()
        style.getSourceAs<GeoJsonSource>("maze-road")?.setGeoJson(if(coordinates.size>=2)FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(coordinates))) else FeatureCollection.fromFeatures(emptyArray<Feature>()))
        style.getSourceAs<GeoJsonSource>("maze-points")?.setGeoJson(FeatureCollection.fromFeatures((if(choosing&&state.reviewingOrigin)emptyList() else listOf(state.destination)).map { Feature.fromGeometry(Point.fromLngLat(it.point.lon,it.point.lat)) }))
        style.getSourceAs<GeoJsonSource>("maze-origin")?.setGeoJson(if(choosing&&!state.reviewingOrigin)empty else FeatureCollection.fromFeature(Feature.fromGeometry(Point.fromLngLat(state.origin.point.lon,state.origin.point.lat))))
        val points=if(choosing)preview?.points.orEmpty()+ring+listOf(if(state.reviewingOrigin)state.origin.point else state.destination.point) else route?.geometry.orEmpty()+ring+listOf(state.origin.point,state.destination.point)
        if(points.distinct().size>=2) {
            val bounds=LatLngBounds.Builder().includes(points.map{LatLng(it.lat,it.lon)}).build()
            m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,64,topInset.coerceAtLeast(64),rightInset.coerceAtLeast(64),bottomInset.coerceAtLeast(64)))
        }else if(choosing){m.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(points.first().lat,points.first().lon),16.0))}
        if(choosing&&m.cameraPosition.zoom>18)m.moveCamera(CameraUpdateFactory.zoomTo(18.0))
        onApproachApplied(preview)
    }
}
