package my.maze.malaysia
import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import org.json.JSONObject

/** Only the three pinned public datasets are accepted; ZIP paths and metadata never select output paths. */
suspend fun importMalaysiaPack(context:Context,uri:Uri,progress:suspend(Long,Long)->Unit) {
 val manifest=JSONObject(context.assets.open("offline/manifest.json").bufferedReader().use{it.readText()});val expected=manifest.getJSONObject("files")
 val dir=File(context.noBackupFilesDir,"regional-pack").apply{mkdirs()};val stage=File(context.noBackupFilesDir,"regional-import").apply{mkdirs()}
 stage.listFiles()?.filter{it.name.matches(Regex("[a-f0-9]{64}\\.part"))}?.forEach{it.delete()}
 val total=expected.keys().asSequence().sumOf{expected.getJSONObject(it).getLong("bytes")};var writtenTotal=0L;val seen=mutableSetOf<String>();val pending=mutableListOf<Pair<File,File>>()
 try {
  ZipInputStream(context.contentResolver.openInputStream(uri) ?: error("This pack could not be opened.")).use{zip->
   while(true){coroutineContext.ensureActive();val entry=zip.nextEntry ?: break;val name=entry.name
    if(name in listOf("manifest.json","NOTICE.txt")){var size=0;val buffer=ByteArray(8192);while(true){val n=zip.read(buffer);if(n<0)break;size+=n;check(size<=65536){"Pack metadata is too large."}};zip.closeEntry();continue}
    check(expected.has(name)&&seen.add(name)&&!entry.isDirectory){"This is not the reviewed Malaysia pack, or it contains duplicate files."}
    val meta=expected.getJSONObject(name);val hash=meta.getString("sha256");val tmp=File(stage,"$hash.part");val destination=File(dir,"$hash-$name");pending.add(tmp to destination);val digest=MessageDigest.getInstance("SHA-256");var size=0L;var notified=0L
    tmp.outputStream().use{out->val buffer=ByteArray(65536);while(true){coroutineContext.ensureActive();val n=zip.read(buffer);if(n<0)break;size+=n;check(size<=meta.getLong("bytes")){"Pack file is larger than expected."};digest.update(buffer,0,n);out.write(buffer,0,n);if(size-notified>=8*1024*1024){progress(writtenTotal+size,total);notified=size}}}
    check(size==meta.getLong("bytes")&&digest.digest().joinToString(""){"%02x".format(it)}==hash){"Pack verification failed. Choose the matching, complete Malaysia pack."};writtenTotal+=size;progress(writtenTotal,total);zip.closeEntry()
   }
  }
  check(seen.size==expected.length()){"This pack is incomplete."};coroutineContext.ensureActive()
  for((tmp,dest)in pending){if(dest.exists())check(dest.delete());check(tmp.renameTo(dest)){"The verified pack could not be saved. Check free storage."}}
 } finally {pending.forEach{it.first.delete()}}
}
