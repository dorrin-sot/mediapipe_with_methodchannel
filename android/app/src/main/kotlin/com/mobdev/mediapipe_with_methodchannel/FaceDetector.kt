package com.mobdev.mediapipe_with_methodchannel

import android.graphics.BitmapFactory
import com.google.mediapipe.solutions.facedetection.FaceDetection
import com.google.mediapipe.solutions.facedetection.FaceDetectionOptions
import com.google.mediapipe.solutions.facedetection.FaceDetectionResult
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlin.math.pow
import kotlin.math.roundToInt


class FaceDetector(private val activity: MainActivity, private val faceEventChannel: EventChannel) : MethodChannel.MethodCallHandler {
    private var faceDetection: FaceDetection? = null

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "start_detection" -> {
                startFaceDetection(call, result)
            }
            "send_image" -> {
                sendImageToFaceDetection(call, result);
            }
        }
    }

    private fun startFaceDetection(call: MethodCall, result: MethodChannel.Result) {
        faceDetection = FaceDetection(
            activity,
            FaceDetectionOptions.builder()
                .setStaticImageMode(true)
                .setModelSelection(0)
                .setMinDetectionConfidence(0.5f)
                .build()
        )

        var sink: EventChannel.EventSink? = null;
        faceEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                sink = events

                faceDetection!!.apply {
                    setResultListener { detection ->
                        activity.runOnUiThread {
                            sink!!.success(data(detection))
                        }
                    }
                }
            }

            private fun data(detection: FaceDetectionResult?): String? {
                val locationData = detection?.multiFaceDetections()
                    ?.takeUnless { it.isEmpty() }
                    ?.get(0)
                    ?.locationData
                    ?: return null
                val keypointNames = arrayOf(
                    "Right Eye",
                    "Left Eye",
                    "Nose Tip",
                    "Mouth Center",
                    "Right Ear Tragion",
                    "Left Ear Tragion",
                )
                val prec = 10.0.pow(2.0)
                return List(6) { it }
                    .map { locationData.getRelativeKeypoints(it)!! }
                    .mapIndexed { i, it ->
                        "${keypointNames[i]}: " +
                                "(" +
                                "${it.x.times(prec).roundToInt().toDouble().div(prec)}, " +
                                "${it.y.times(prec).roundToInt().toDouble().div(prec)}" +
                                ")"
                    }
                    .joinToString("\n")
            }

            override fun onCancel(arguments: Any?) {
                faceDetection?.setResultListener(null)
                faceDetection?.close()
                sink?.endOfStream()
            }
        })
        result.success("started");
    }

    private fun sendImageToFaceDetection(call: MethodCall, result: MethodChannel.Result) {
        val image = call.argument<ByteArray>("image")
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image!!.size)
        faceDetection!!.send(bitmap)
        result.success("image sent")
    }
}