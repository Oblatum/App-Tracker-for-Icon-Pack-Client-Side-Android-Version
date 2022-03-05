package ren.imyan.app_tracker.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ren.imyan.app_tracker.R

class AboutActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.action_about)

        findViewById<TextView>(R.id.textView2).setOnClickListener {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://github.com/Oblatum/App-Tracker-for-Icon-Pack-Client-Side-Android-Version");
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.ali).setOnClickListener {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://qr.alipay.com/fkx11504wccj8dgl2dc41d1");
            }
            startActivity(intent)
        }
    }
}