package ren.imyan.app_tracker.common.ktx

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService

fun String.copy(){
    val cm: ClipboardManager = get<Context>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val data = ClipData.newPlainText("Label",this)
    cm.setPrimaryClip(data)
    Toast.makeText(get(),"ε€εΆζε",Toast.LENGTH_SHORT).show()
}
