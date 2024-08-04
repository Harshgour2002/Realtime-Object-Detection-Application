package com.example.imagerecognition

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.imagerecognition.ml.AutoModel1
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import android.graphics.Color
import org.tensorflow.lite.support.common.FileUtil

class MainActivity : AppCompatActivity() {
    lateinit var textureView:TextureView
    lateinit var CameraManager:CameraManager
    lateinit var handler:Handler
    lateinit var cameraDevice:CameraDevice
    lateinit var imageView:ImageView
    lateinit var bitmap:Bitmap
    lateinit var model:AutoModel1
    lateinit var imageProcessor:ImageProcessor
    val paint=Paint()
    val colors = listOf<Int>(
        android.graphics.Color.BLUE,
        android.graphics.Color.GREEN,
        android.graphics.Color.RED,
        android.graphics.Color.CYAN,
        android.graphics.Color.DKGRAY,
        android.graphics.Color.MAGENTA,
        android.graphics.Color.YELLOW,
        android.graphics.Color.BLACK
    )
    lateinit var labels:List<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getCameraPermission()
        labels= FileUtil.loadLabels(this,"labels.txt")
        val handlerThread=HandlerThread("videoThread")
            handlerThread.start()
        handler= Handler(handlerThread.looper)
        imageView=findViewById(R.id.imageView)
        textureView=findViewById(R.id.textureView)
        model = AutoModel1.newInstance(this@MainActivity)
        imageProcessor=ImageProcessor.Builder().add(ResizeOp(300,300, ResizeOp.ResizeMethod.BILINEAR)).build()
        textureView.surfaceTextureListener=object :TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
               open_Camera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
              return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
              bitmap=textureView.bitmap!!


// Creates inputs for reference.
                var image = TensorImage.fromBitmap(bitmap)
                image=imageProcessor.process(image)
// Runs model inference and gets result.
                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

// Releases model resources if no longer used.
                var mutable=bitmap.copy(Bitmap.Config.ARGB_8888,true)
                val canves=Canvas(mutable)

                val h=mutable.height
                val w=mutable.width
                paint.textSize=h/15f
                paint.strokeWidth=h/85f
                var x=0
                scores.forEachIndexed { index, fl ->
                x=index
                    x*=4
                    if(fl>0.5){
                        paint.setColor(colors.get(index))
                        paint.style=Paint.Style.STROKE
                        canves.drawRect(RectF(locations.get(x+1)*w,locations.get(x)*h,
                                    locations.get(x+3)*w,locations.get(x+2)*h),paint)
                        paint.style=Paint.Style.FILL
                        canves.drawText(labels.get(classes.get(index).toInt())+" "+fl.toString(),
                                        locations.get(x+1)*w,locations.get(x)*h,paint)
                    }

                }
                imageView.setImageBitmap(mutable)

            }

        }
        CameraManager=getSystemService(Context.CAMERA_SERVICE)as CameraManager
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
    @SuppressLint("MissingPermission")
    private fun open_Camera(){
        CameraManager.openCamera(CameraManager.cameraIdList[0],object :CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice=p0
                var surfaceTexture=textureView.surfaceTexture
                var surface=Surface(surfaceTexture)
                var captureRequest=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface),object:CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                    p0.setRepeatingRequest(captureRequest.build(),null,null)
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {

                    }
                },handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            @SuppressLint("MissingPermission")
            override fun onError(p0: CameraDevice, p1: Int) {


            }
        }, handler)
    }
     private fun getCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)

        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    )  {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0]!=PackageManager.PERMISSION_GRANTED){
            getCameraPermission()
        }
    }



}