package xyz.herelookingatyoukid.svgpaintinglibrary

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var loadFinished = false
        GlobalScope.launch(Dispatchers.IO) {
            val str = getFileContent(assets.open("flower.svg"))
            withContext(Dispatchers.Main) {
                svg.loadMap("flower.svg", str)
                loadFinished = true
            }
        }
        pre.setOnClickListener {
            if (loadFinished) {
                svg.preStep()
            }
        }
        post.setOnClickListener {
            if (loadFinished) {
                svg.postStep()
            }
        }
        var currentColorIsRed = false
        color.setOnClickListener {
            if (loadFinished) {
                if (currentColorIsRed) {
                    svg.setPaintColor("#FF0000")
                } else {
                    svg.setPaintColor("#888888")
                }
                currentColorIsRed = !currentColorIsRed
            }
        }
    }

    private fun getFileContent(inputStream: InputStream): String? {
        var inputReader: InputStreamReader? = null
        var bufReader: BufferedReader? = null
        val result = StringBuilder()
        try {
            inputReader = InputStreamReader(inputStream)
            bufReader = BufferedReader(inputReader)
            var line: String?
            while (bufReader.readLine().also { line = it } != null) {
                result.append(line)
            }
            bufReader.close()
            inputReader.close()
            inputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bufReader?.close()
            inputReader?.close()
            inputStream.close()
        }
        return result.toString()
    }
}
