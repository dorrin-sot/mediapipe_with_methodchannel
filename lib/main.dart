import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: Scaffold(
        body: MyHomePage(),
      ),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  CameraController? _cameraController;
  bool cameraStarted = false;

  CameraImage? curImage;

  static const METHOD_CHANNEL = "mediapipe_with_methodchannel/method";
  static const EVENT_CHANNEL = "mediapipe_with_methodchannel/event";

  static const methodChannel = MethodChannel(METHOD_CHANNEL);
  static const eventChannel = EventChannel(EVENT_CHANNEL);

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Center(
        child: !cameraStarted
            ? TextButton(
                onPressed: startCamera,
                child: const Text("Start Camera"),
              )
            : Column(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  CameraPreview(_cameraController!),
                  StreamBuilder(
                    stream: eventChannel.receiveBroadcastStream().cast(),
                    builder: (BuildContext _, AsyncSnapshot<dynamic> snapshot) {
                      if (snapshot.hasData && snapshot.data != null) {
                        return Text("${snapshot.data}");
                      }
                      if (snapshot.hasError) return Text("${snapshot.error}");
                      return const Text("Nothingness...");
                    },
                  ),
                ],
              ),
      ),
    );
  }

  Future<void> startCamera() async {
    final camera = (await availableCameras()).firstWhere(
      (e) => e.lensDirection == CameraLensDirection.front,
    );
    _cameraController = CameraController(
      camera,
      ResolutionPreset.medium,
      enableAudio: false,
      imageFormatGroup: ImageFormatGroup.jpeg,
    );
    await _cameraController!.initialize();
    await _startDetection();
    setState(() => cameraStarted = true);
    _cameraController!.startImageStream(_sendImage);
  }

  _startDetection() async {
    print(await methodChannel.invokeMethod("start_detection"));
  }

  _sendImage(CameraImage image) async {
    curImage = image;

    final imageBytes = image.planes.first.bytes;
    print(await methodChannel.invokeMethod(
      "send_image",
      {'image': imageBytes},
    ));
  }
}
