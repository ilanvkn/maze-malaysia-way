package my.maze.malaysia

import android.app.Application
import android.net.Uri
import android.util.AtomicFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalDate

internal class MazeGateModel(app:Application):AndroidViewModel(app){
 var dataset by mutableStateOf<GateDataset?>(null);private set
 var pending by mutableStateOf<GateDataset?>(null);private set
 var busy by mutableStateOf(false);private set
 var message by mutableStateOf<String?>(null);private set
 var today by mutableStateOf(LocalDate.now());private set
 private var pendingText:String?=null
 private val storage=AtomicFile(File(app.noBackupFilesDir,"gate-pilot.json"))
 init{busy=true;viewModelScope.launch{try{dataset=withContext(Dispatchers.IO){if(storage.baseFile.exists())parseGateDataset(storage.openRead().use{readBounded(it)},LocalDate.now())else null}}catch(_:Exception){message="Saved pilot records could not be read. Import a reviewed file to replace them."}finally{busy=false}}}
 fun refreshDate(){today=LocalDate.now()}
 fun demo(){if(busy)return;dataset=demoGateDataset(LocalDate.now());message="Test scenarios only. Your imported file is kept until explicitly replaced or removed."}
 fun import(uri:Uri){if(busy)return;busy=true;message=null;viewModelScope.launch{try{
  val text=withContext(Dispatchers.IO){getApplication<Application>().contentResolver.openInputStream(uri)?.use{readBounded(it)} ?: error("File unavailable")}
  pending=parseGateDataset(text,LocalDate.now());pendingText=text
 }catch(e:CancellationException){throw e}catch(_:Exception){message="Pilot file rejected. Check the schema, permission, dates and coordinate rules. Existing records are unchanged."}finally{busy=false}}}
 fun dismissImport(){pending=null;pendingText=null}
 fun acceptImport(){val text=pendingText ?: return;if(busy)return;busy=true
  viewModelScope.launch{try{
   val parsed=parseGateDataset(text,LocalDate.now())
   withContext(Dispatchers.IO){val out=storage.startWrite();try{out.write(text.toByteArray(Charsets.UTF_8));storage.finishWrite(out)}catch(e:Exception){storage.failWrite(out);throw e}}
   dataset=parsed;dismissImport();message="Pilot records saved on this device. Imported checks remain source claims, not Maze verification."
  }catch(e:CancellationException){throw e}catch(_:Exception){message="Pilot records could not be saved. Previous stored records remain available."}finally{busy=false}}
 }
 fun clear(){if(busy)return;busy=true;viewModelScope.launch{try{withContext(Dispatchers.IO){storage.delete()};dataset=null;message="Pilot records removed. Saved map trips are managed separately."}catch(_:Exception){message="Pilot records could not be removed. Try again."}finally{busy=false}}}
 companion object{
  private fun readBounded(input:java.io.InputStream):String{val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=GATE_MAX_BYTES);out.write(buffer,0,n)};return out.toString("UTF-8")}
 }
}
