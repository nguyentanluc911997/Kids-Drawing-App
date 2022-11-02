package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.example.kidsdrawingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mimageButtonCurrentPaint: ImageButton
    var openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if (result.resultCode == RESULT_OK && result.data != null){
                binding.ivBackground.setImageURI(result.data?.data)
            }
        }
    val reuquestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                    ).show()

                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this@MainActivity,
                            "Oops you just denied the permision.",
                            Toast.LENGTH_LONG
                        )
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.drawingView.setSizeForBrush(20.toFloat())

        mimageButtonCurrentPaint = binding.lnPaintColors[1] as ImageButton

        mimageButtonCurrentPaint.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        binding.idBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        binding.idGallery.setOnClickListener {
            requestStoragePermission()
        }

        binding.idUndo.setOnClickListener{
            binding.drawingView.onClickUndo()
//            lifecycleScope.launch{
//                count("dmmm")
//            }

        }
        binding.idSave.setOnClickListener {

            if (isReadStorageAllowed()){
                lifecycleScope.launch{
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    val myBitmap:Bitmap = getBitmapFromView(flDrawingView)
                    saveBitmapFile(myBitmap)
                }
            }
        }


    }
    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path,uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))

        }

    }


    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,
        Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){

        }else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if (mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "kidsdrawingapp" + System.currentTimeMillis()/1000 + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File successfully", Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }else {
                            Toast.makeText(this@MainActivity, "File error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result


    }

    private suspend fun count(abc:String){
        withContext(Dispatchers.IO){
            for (i in 1..100000){
                Log.e("count count","$i")
            }
            runOnUiThread{
                Toast.makeText(this@MainActivity,abc,Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean{
        var result = ContextCompat.checkSelfPermission(
            this,Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,

                )
        ) {
            showRationaleDialog("Lực đẹp trai", "Needs to Access Your External Storage")
        } else {
            reuquestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
            )
        }
    }


    private fun showRationaleDialog(
        title: String,
        message: String
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    fun paintClicked(view: View) {
        if (view !== mimageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            binding.drawingView.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            mimageButtonCurrentPaint.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mimageButtonCurrentPaint = view

        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.id_small_brush)
        smallBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.id_medium_brush)
        mediumBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.id_large_brush)
        largeBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()

    }

}