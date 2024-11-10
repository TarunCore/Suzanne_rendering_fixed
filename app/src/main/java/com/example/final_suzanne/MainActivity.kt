//package com.example.suzanne
//
//
//
//import android.opengl.GLSurfaceView
//
//import javax.microedition.khronos.egl.EGLConfig
//
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.opengl.GLES30
//import android.os.Bundle
//import android.util.Log
//import android.view.*
//import android.view.GestureDetector
//import android.widget.TextView
//import android.widget.Toast
//import com.google.android.filament.Fence
//import com.google.android.filament.IndirectLight
//import com.google.android.filament.Skybox
//import com.google.android.filament.View
//import com.google.android.filament.utils.*
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.io.File
//import java.io.FileInputStream
//import java.io.RandomAccessFile
//import java.net.URI
//import java.nio.Buffer
//import java.nio.ByteBuffer
//import java.nio.charset.StandardCharsets
//import java.util.zip.ZipInputStream
//import javax.microedition.khronos.opengles.GL10
//
//
//class MainActivity : Activity() {
//
//    companion object {
//        // Load the library for the utility layer, which in turn loads gltfio and the Filament core.
//        init { Utils.init() }
//        private const val TAG = "gltf-viewer"
//    }
//
//    private lateinit var surfaceView: SurfaceView
//    private lateinit var choreographer: Choreographer
//    private val frameScheduler = FrameCallback()
//    private lateinit var modelViewer: ModelViewer
//    private lateinit var titlebarHint: TextView
//    private val doubleTapListener = DoubleTapListener()
//    private val singleTapListener = SingleTapListener()
//    private lateinit var doubleTapDetector: GestureDetector
//    private lateinit var singleTapDetector: GestureDetector
//    private var remoteServer: RemoteServer? = null
//    private var statusToast: Toast? = null
//    private var statusText: String? = null
//    private var latestDownload: String? = null
//    private val automation = AutomationEngine()
//    private var loadStartTime = 0L
//    private var loadStartFence: Fence? = null
//    private val viewerContent = AutomationEngine.ViewerContent()
//    private lateinit var glSurfaceView: GLSurfaceView
//    private var customShaderProgram: Int = 0
//
//
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        glSurfaceView = GLSurfaceView(this).apply {
//            setEGLContextClientVersion(3)
//            setRenderer(ModelRenderer())
//        }
//        setContentView(glSurfaceView)
//
//        setContentView(R.layout.activity_main)
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//
//        surfaceView = findViewById(R.id.main_sv)
//        choreographer = Choreographer.getInstance()
//
//        doubleTapDetector = GestureDetector(applicationContext, doubleTapListener)
//
//        singleTapDetector = GestureDetector(applicationContext, singleTapListener)
//
////        val vertexShader: String = loadShaderFromAssets("vertex_shader.glsl")
////        val fragmentShader: String = loadShaderFromAssets("fragment_shader.glsl")
//
//        modelViewer = ModelViewer(surfaceView)
//        viewerContent.view = modelViewer.view
//        viewerContent.sunlight = modelViewer.light
//        viewerContent.lightManager = modelViewer.engine.lightManager
//        viewerContent.scene = modelViewer.scene
//        viewerContent.renderer = modelViewer.renderer
//
//        surfaceView.setOnTouchListener { _, event ->
//            modelViewer.onTouchEvent(event)
//            doubleTapDetector.onTouchEvent(event)
//            singleTapDetector.onTouchEvent(event)
//            true
//        }
//
//        createDefaultRenderables()
//        createIndirectLight()
//
//        setStatusText("To load a new model, go to the above URL on your host machine.")
//
//        val view = modelViewer.view
//
//        /*
//         * Note: The settings below are overriden when connecting to the remote UI.
//         */
//
//        // on mobile, better use lower quality color buffer
//        view.renderQuality = view.renderQuality.apply {
//            hdrColorBuffer = View.QualityLevel.MEDIUM
//        }
//
//        // dynamic resolution often helps a lot
//        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
//            enabled = true
//            quality = View.QualityLevel.MEDIUM
//        }
//
//        // MSAA is needed with dynamic resolution MEDIUM
//        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
//            enabled = true
//        }
//
//        // FXAA is pretty cheap and helps a lot
//        view.antiAliasing = View.AntiAliasing.FXAA
//
//        // ambient occlusion is the cheapest effect that adds a lot of quality
//        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
//            enabled = true
//        }
//
//        // bloom is pretty expensive but adds a fair amount of realism
//        view.bloomOptions = view.bloomOptions.apply {
//            enabled = true
//        }
//
//        //applyCustomShader(vertexShader, fragmentShader);
//
//        remoteServer = RemoteServer(8082)
//    }
//
//
//    private val vertexShaderCode = """
//        attribute vec4 a_Position;
//        attribute vec3 a_Normal;
//        varying vec3 v_Normal;
//        void main() {
//            gl_Position = a_Position;
//            v_Normal = a_Normal;
//        }
//    """
//
//    // Fragment Shader Code (GLSL)
//    private val fragmentShaderCode = """
//        precision mediump float;
//        varying vec3 v_Normal;
//        uniform float iTime;
//        void main() {
//            vec3 color = vec3(abs(sin(iTime)), 0.5, 0.5);
//            gl_FragColor = vec4(color, 1.0);
//        }
//    """
//
//    private fun createDefaultRenderables() {
//        val buffer = assets.open("models/suzanne_skin_material_test.glb").use { input ->
//            val bytes = ByteArray(input.available())
//            input.read(bytes)
//            ByteBuffer.wrap(bytes)
//        }
//
//        modelViewer.loadModelGltfAsync(buffer) { uri -> readCompressedAsset("models/$uri") }
//        updateRootTransform()
//    }
//
//    private fun createIndirectLight() {
//        val engine = modelViewer.engine
//        val scene = modelViewer.scene
//        val ibl = "venetian_crossroads_2k"
//        readCompressedAsset("envs/$ibl/${ibl}_ibl.ktx").let {
//            scene.indirectLight = KTX1Loader.createIndirectLight(engine, it)
//            scene.indirectLight!!.intensity = 30_000.0f
//            viewerContent.indirectLight = modelViewer.scene.indirectLight
//        }
//        readCompressedAsset("envs/$ibl/${ibl}_skybox.ktx").let {
//            scene.skybox = KTX1Loader.createSkybox(engine, it)
//        }
//    }
//
//    private fun readCompressedAsset(assetName: String): ByteBuffer {
//        val input = assets.open(assetName)
//        val bytes = ByteArray(input.available())
//        input.read(bytes)
//        return ByteBuffer.wrap(bytes)
//    }
//
//    private fun clearStatusText() {
//        statusToast?.let {
//            it.cancel()
//            statusText = null
//        }
//    }
//
//    private fun setStatusText(text: String) {
//        runOnUiThread {
//            if (statusToast == null || statusText != text) {
//                statusText = text
//                statusToast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
//                statusToast!!.show()
//
//            }
//        }
//    }
//
//    private suspend fun loadGlb(message: RemoteServer.ReceivedMessage) {
//        withContext(Dispatchers.Main) {
//            modelViewer.destroyModel()
//            modelViewer.loadModelGlb(message.buffer)
//            updateRootTransform()
//            loadStartTime = System.nanoTime()
//            loadStartFence = modelViewer.engine.createFence()
//        }
//    }
//
//    private suspend fun loadHdr(message: RemoteServer.ReceivedMessage) {
//        withContext(Dispatchers.Main) {
//            val engine = modelViewer.engine
//            val equirect = HDRLoader.createTexture(engine, message.buffer)
//            if (equirect == null) {
//                setStatusText("Could not decode HDR file.")
//            } else {
//                setStatusText("Successfully decoded HDR file.")
//
//                val context = IBLPrefilterContext(engine)
//                val equirectToCubemap = IBLPrefilterContext.EquirectangularToCubemap(context)
//                val skyboxTexture = equirectToCubemap.run(equirect)!!
//                engine.destroyTexture(equirect)
//
//                val specularFilter = IBLPrefilterContext.SpecularFilter(context)
//                val reflections = specularFilter.run(skyboxTexture)
//
//                val ibl = IndirectLight.Builder()
//                    .reflections(reflections)
//                    .intensity(30000.0f)
//                    .build(engine)
//
//                val sky = Skybox.Builder().environment(skyboxTexture).build(engine)
//
//                specularFilter.destroy()
//                equirectToCubemap.destroy()
//                context.destroy()
//
//                // destroy the previous IBl
//                engine.destroyIndirectLight(modelViewer.scene.indirectLight!!)
//                engine.destroySkybox(modelViewer.scene.skybox!!)
//
//                modelViewer.scene.skybox = sky
//                modelViewer.scene.indirectLight = ibl
//                viewerContent.indirectLight = ibl
//
//            }
//        }
//    }
//
//    private suspend fun loadZip(message: RemoteServer.ReceivedMessage) {
//        // To alleviate memory pressure, remove the old model before deflating the zip.
//        withContext(Dispatchers.Main) {
//            modelViewer.destroyModel()
//        }
//
//        // Large zip files should first be written to a file to prevent OOM.
//        // It is also crucial that we null out the message "buffer" field.
//        val (zipStream, zipFile) = withContext(Dispatchers.IO) {
//            val file = File.createTempFile("incoming", "zip", cacheDir)
//            val raf = RandomAccessFile(file, "rw")
//            raf.channel.write(message.buffer)
//            message.buffer = null
//            raf.seek(0)
//            Pair(FileInputStream(file), file)
//        }
//
//        // Deflate each resource using the IO dispatcher, one by one.
//        var gltfPath: String? = null
//        var outOfMemory: String? = null
//        val pathToBufferMapping = withContext(Dispatchers.IO) {
//            val deflater = ZipInputStream(zipStream)
//            val mapping = HashMap<String, Buffer>()
//            while (true) {
//                val entry = deflater.nextEntry ?: break
//                if (entry.isDirectory) continue
//
//                // This isn't strictly required, but as an optimization
//                // we ignore common junk that often pollutes ZIP files.
//                if (entry.name.startsWith("__MACOSX")) continue
//                if (entry.name.startsWith(".DS_Store")) continue
//
//                val uri = entry.name
//                val byteArray: ByteArray? = try {
//                    deflater.readBytes()
//                }
//                catch (e: OutOfMemoryError) {
//                    outOfMemory = uri
//                    break
//                }
//                Log.i(TAG, "Deflated ${byteArray!!.size} bytes from $uri")
//                val buffer = ByteBuffer.wrap(byteArray)
//                mapping[uri] = buffer
//                if (uri.endsWith(".gltf") || uri.endsWith(".glb")) {
//                    gltfPath = uri
//                }
//            }
//            mapping
//        }
//
//        zipFile.delete()
//
//        if (gltfPath == null) {
//            setStatusText("Could not find .gltf or .glb in the zip.")
//            return
//        }
//
//        if (outOfMemory != null) {
//            setStatusText("Out of memory while deflating $outOfMemory")
//            return
//        }
//
//        val gltfBuffer = pathToBufferMapping[gltfPath]!!
//
//        // In a zip file, the gltf file might be in the same folder as resources, or in a different
//        // folder. It is crucial to test against both of these cases. In any case, the resource
//        // paths are all specified relative to the location of the gltf file.
//        var prefix = URI(gltfPath!!).resolve(".")
//
//        withContext(Dispatchers.Main) {
//            if (gltfPath!!.endsWith(".glb")) {
//                modelViewer.loadModelGlb(gltfBuffer)
//            } else {
//                modelViewer.loadModelGltf(gltfBuffer) { uri ->
//                    val path = prefix.resolve(uri).toString()
//                    if (!pathToBufferMapping.contains(path)) {
//                        Log.e(TAG, "Could not find '$uri' in zip using prefix '$prefix' and base path '${gltfPath!!}'")
//                        setStatusText("Zip is missing $path")
//                    }
//                    pathToBufferMapping[path]
//                }
//            }
//            updateRootTransform()
//            loadStartTime = System.nanoTime()
//            loadStartFence = modelViewer.engine.createFence()
//        }
//    }
//
//    inner class ModelRenderer : GLSurfaceView.Renderer {
//
//        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//            // Initialize and compile shaders
//            customShaderProgram = createShaderProgram()
//        }
//
//        override fun onDrawFrame(gl: GL10?) {
//            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
//
//            // Use the custom shader program
//            GLES30.glUseProgram(customShaderProgram)
//
//            // Add code here to bind 3D model data (e.g., vertices, textures) to the shader
//            // This is where you would draw your 3D model
//        }
//
//        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//            GLES30.glViewport(0, 0, width, height)
//        }
//
//        // Load and compile the shader program
//        private fun createShaderProgram(): Int {
//            val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
//            val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)
//
//            // Create shader program and link shaders
//            return GLES30.glCreateProgram().also { program ->
//                GLES30.glAttachShader(program, vertexShader)
//                GLES30.glAttachShader(program, fragmentShader)
//                GLES30.glLinkProgram(program)
//            }
//        }
//    }
//
//    private fun loadShader(type: Int, shaderCode: String): Int {
//        return GLES30.glCreateShader(type).also { shader ->
//            GLES30.glShaderSource(shader, shaderCode)
//            GLES30.glCompileShader(shader)
//
//            // Check for compile errors
//            val compileStatus = IntArray(1)
//            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
//            if (compileStatus[0] == 0) {
//                GLES30.glDeleteShader(shader)
//                throw RuntimeException("Error compiling shader: ${GLES30.glGetShaderInfoLog(shader)}")
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        choreographer.postFrameCallback(frameScheduler)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        choreographer.removeFrameCallback(frameScheduler)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        choreographer.removeFrameCallback(frameScheduler)
//        remoteServer?.close()
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        finish()
//    }
//
//    fun loadModelData(message: RemoteServer.ReceivedMessage) {
//        Log.i(TAG, "Downloaded model ${message.label} (${message.buffer.capacity()} bytes)")
//        clearStatusText()
//        titlebarHint.text = message.label
//        CoroutineScope(Dispatchers.IO).launch {
//            when {
//                message.label.endsWith(".zip") -> loadZip(message)
//                message.label.endsWith(".hdr") -> loadHdr(message)
//                else -> loadGlb(message)
//            }
//        }
//    }
//
//    fun loadSettings(message: RemoteServer.ReceivedMessage) {
//        val json = StandardCharsets.UTF_8.decode(message.buffer).toString()
//        viewerContent.assetLights = modelViewer.asset?.lightEntities
//        automation.applySettings(modelViewer.engine, json, viewerContent)
//        modelViewer.view.colorGrading = automation.getColorGrading(modelViewer.engine)
//        modelViewer.cameraFocalLength = automation.viewerOptions.cameraFocalLength
//        modelViewer.cameraNear = automation.viewerOptions.cameraNear
//        modelViewer.cameraFar = automation.viewerOptions.cameraFar
//        updateRootTransform()
//    }
//
//    private fun updateRootTransform() {
//        if (automation.viewerOptions.autoScaleEnabled) {
//            modelViewer.transformToUnitCube()
//        } else {
//            modelViewer.clearRootTransform()
//        }
//    }
//
//    inner class FrameCallback : Choreographer.FrameCallback {
//        private val startTime = System.nanoTime()
//        override fun doFrame(frameTimeNanos: Long) {
//            choreographer.postFrameCallback(this)
//
//            loadStartFence?.let {
//                if (it.wait(Fence.Mode.FLUSH, 0) == Fence.FenceStatus.CONDITION_SATISFIED) {
//                    val end = System.nanoTime()
//                    val total = (end - loadStartTime) / 1_000_000
//                    Log.i(TAG, "The Filament backend took $total ms to load the model geometry.")
//                    modelViewer.engine.destroyFence(it)
//                    loadStartFence = null
//                }
//            }
//
//            modelViewer.animator?.apply {
//                if (animationCount > 0) {
//                    val elapsedTimeSeconds = (frameTimeNanos - startTime).toDouble() / 1_000_000_000
//                    applyAnimation(0, elapsedTimeSeconds.toFloat())
//                }
//                updateBoneMatrices()
//            }
//
//            modelViewer.render(frameTimeNanos)
//
//            // Check if a new download is in progress. If so, let the user know with toast.
//            val currentDownload = remoteServer?.peekIncomingLabel()
//            if (RemoteServer.isBinary(currentDownload) && currentDownload != latestDownload) {
//                latestDownload = currentDownload
//                Log.i(TAG, "Downloading $currentDownload")
//                setStatusText("Downloading $currentDownload")
//            }
//
//            // Check if a new message has been fully received from the client.
//            val message = remoteServer?.acquireReceivedMessage()
//            if (message != null) {
//                if (message.label == latestDownload) {
//                    latestDownload = null
//                }
//                if (RemoteServer.isJson(message.label)) {
//                    loadSettings(message)
//                } else {
//                    loadModelData(message)
//                }
//            }
//        }
//    }
//
//    // Just for testing purposes, this releases the current model and reloads the default model.
//    inner class DoubleTapListener : GestureDetector.SimpleOnGestureListener() {
//        override fun onDoubleTap(e: MotionEvent): Boolean {
//            modelViewer.destroyModel()
//            createDefaultRenderables()
//            return super.onDoubleTap(e)
//        }
//    }
//    // Just for testing purposes
//    inner class SingleTapListener : GestureDetector.SimpleOnGestureListener() {
//        override fun onSingleTapUp(event: MotionEvent): Boolean {
//            modelViewer.view.pick(
//                event.x.toInt(),
//                surfaceView.height - event.y.toInt(),
//                surfaceView.handler
//            ) {
//                val name = modelViewer.asset!!.getName(it.renderable)
//                Log.v("Filament", "Picked ${it.renderable}: " + name)
//            }
//
//            return super.onSingleTapUp(event)
//        }
//    }
//}


/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.suzanne



import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.animation.LinearInterpolator

import com.google.android.filament.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.utils.*
import com.google.android.filament.android.UiHelper

import java.nio.ByteBuffer
import java.nio.channels.Channels
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


import com.google.android.filament.View

class MainActivity : Activity() {
    // Make sure to initialize the correct Filament JNI layer.
    companion object {
        init {
            Utils.init()
        }
    }

    // The View we want to render into
    private lateinit var surfaceView: SurfaceView
    // UiHelper is provided by Filament to manage SurfaceView and SurfaceTexture
    private lateinit var uiHelper: UiHelper
    // DisplayHelper is provided by Filament to manage the display
    private lateinit var displayHelper: DisplayHelper
    // Choreographer is used to schedule new frames
    private lateinit var choreographer: Choreographer

    // Engine creates and destroys Filament resources
    // Each engine must be accessed from a single thread of your choosing
    // Resources cannot be shared across engines
    private lateinit var engine: Engine
    // A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
    private lateinit var renderer: Renderer
    // A scene holds all the renderable, lights, etc. to be drawn
    private lateinit var scene: Scene
    // A view defines a viewport, a scene and a camera for rendering
    private lateinit var view: View
    // Should be pretty obvious :)
    private lateinit var camera: Camera

    private lateinit var material: Material
    private lateinit var materialInstance: MaterialInstance

    private lateinit var baseColor: Texture
    private lateinit var normal: Texture
    private lateinit var aoRoughnessMetallic: Texture

    private lateinit var mesh: Mesh
    private lateinit var ibl: Ibl

    // Filament entity representing a renderable object
    @Entity private var light = 0

    // A swap chain is Filament's representation of a surface
    private var swapChain: SwapChain? = null

    // Performs the rendering and schedules new frames
    private val frameScheduler = FrameCallback()

    private val animator = ValueAnimator.ofFloat(0.0f, (2.0 * PI).toFloat())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        choreographer = Choreographer.getInstance()

        displayHelper = DisplayHelper(this)

        setupSurfaceView()
        setupFilament()
        setupView()
        setupScene()
    }

    private fun setupSurfaceView() {
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()

        // NOTE: To choose a specific rendering resolution, add the following line:
        // uiHelper.setDesiredSize(1280, 720)

        uiHelper.attachTo(surfaceView)
    }

    private fun setupFilament() {
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())
    }

    private fun setupView() {
        // NOTE: Try to disable post-processing (tone-mapping, etc.) to see the difference
        // view.isPostProcessingEnabled = false

        // Tell the view which camera we want to use
        view.camera = camera

        // Tell the view which scene we want to render
        view.scene = scene

        // Enable dynamic resolution with a default target frame rate of 60fps
        val options = View.DynamicResolutionOptions()
        options.enabled = true

        view.dynamicResolutionOptions = options
    }

    private fun setupScene() {
        loadMaterial()
        setupMaterial()
        loadImageBasedLight()

        scene.skybox = ibl.skybox
        scene.indirectLight = ibl.indirectLight

        // This map can contain named materials that will map to the material names
        // loaded from the filamesh file. The material called "DefaultMaterial" is
        // applied when no named material can be found
        val materials = mapOf("DefaultMaterial" to materialInstance)

        // Load the mesh in the filamesh format (see filamesh tool)
        mesh = loadMesh(assets, "models/suzanne.filamesh", materials, engine)

        // Move the mesh down
        // Filament uses column-major matrices
        engine.transformManager.setTransform(engine.transformManager.getInstance(mesh.renderable),
            floatArrayOf(
                1.0f,  0.0f, 0.0f, 0.0f,
                0.0f,  1.0f, 0.0f, 0.0f,
                0.0f,  0.0f, 1.0f, 0.0f,
                0.0f, -1.2f, 0.0f, 1.0f
            ))

        // Add the entity to the scene to render it
        scene.addEntity(mesh.renderable)

        // We now need a light, let's create a directional light
        light = EntityManager.get().create()

        // Create a color from a temperature (D65)
        val (r, g, b) = Colors.cct(6_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(r, g, b)
            // Intensity of the sun in lux on a clear day
            .intensity(110_000.0f)
            // The direction is normalized on our behalf
            .direction(-0.753f, -1.0f, 0.890f)
            .castShadows(true)
            .build(engine, light)

        // Add the entity to the scene to light it
        scene.addEntity(light)

        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we've defined a light that has the same intensity as the sun, it
        // guarantees a proper exposure
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)

        startAnimation()
    }

    private fun loadMaterial() {
        readUncompressedAsset("materials/textured_pbr.filamat").let {
            material = Material.Builder().payload(it, it.remaining()).build(engine)
        }
    }

    private fun setupMaterial() {
        // Create an instance of the material to set different parameters on it
        materialInstance = material.createInstance()

        // Note that the textures are stored in drawable-nodpi to prevent the system
        // from automatically resizing them based on the display's density
        baseColor = loadTexture(engine, resources, R.drawable.floor_basecolor, TextureType.COLOR)
        normal = loadTexture(engine, resources, R.drawable.floor_normal, TextureType.NORMAL)
        aoRoughnessMetallic = loadTexture(engine, resources,
            R.drawable.floor_ao_roughness_metallic, TextureType.DATA)

        // A texture sampler does not need to be kept around or destroyed
        val sampler = TextureSampler()
        sampler.anisotropy = 8.0f

        materialInstance.setParameter("baseColor", baseColor, sampler)
        materialInstance.setParameter("normal", normal, sampler)
        materialInstance.setParameter("aoRoughnessMetallic", aoRoughnessMetallic, sampler)
    }

    private fun loadImageBasedLight() {
        ibl = loadIbl(assets, "envs/flower_road_no_sun_2k", engine)
        ibl.indirectLight.intensity = 40_000.0f
//        val engine = modelViewer.engine
//        val scene = modelViewer.scene
//        val ibl = "venetian_crossroads_2k"
//        readCompressedAsset("envs/$ibl/${ibl}_ibl.ktx").let {
//            scene.indirectLight = KTX1Loader.createIndirectLight(engine, it)
//            scene.indirectLight!!.intensity = 30_000.0f
//            viewerContent.indirectLight = modelViewer.scene.indirectLight
//        }
//        readCompressedAsset("envs/$ibl/${ibl}_skybox.ktx").let {
//            scene.skybox = KTX1Loader.createSkybox(engine, it)
//        }
    }

    private fun readCompressedAsset(assetName: String): ByteBuffer {
        // NOT USED ANYWAYS --gradle file fixed for compression issue
//        BELOW IS FROM GPT. 2ND SOURCE ABOVE COMMENTED CODE
            assets.open(assetName).use { inputStream ->
                val dst = ByteBuffer.allocate(inputStream.available())
                val src = Channels.newChannel(inputStream)
                src.read(dst)
                src.close()
                return dst.apply { rewind() }
            }
    }

    private fun startAnimation() {
        // Animate the triangle
        animator.interpolator = LinearInterpolator()
        animator.duration = 18_000
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { a ->
            val v = (a.animatedValue as Float)
            camera.lookAt(cos(v) * 5.5, 1.5, sin(v) * 5.5, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        }
        animator.start()
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameScheduler)
        animator.start()
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop the animation and any pending frame
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel()

        // Always detach the surface before destroying the engine
        uiHelper.detach()

        // Cleanup all resources
        destroyMesh(engine, mesh)
        destroyIbl(engine, ibl)
        engine.destroyTexture(baseColor)
        engine.destroyTexture(normal)
        engine.destroyTexture(aoRoughnessMetallic)
        engine.destroyEntity(light)
        engine.destroyRenderer(renderer)
        engine.destroyMaterialInstance(materialInstance)
        engine.destroyMaterial(material)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)

        // Engine.destroyEntity() destroys Filament related resources only
        // (components), not the entity itself
        val entityManager = EntityManager.get()
        entityManager.destroy(light)
        entityManager.destroy(camera.entity)

        // Destroying the engine will free up any resource you may have forgotten
        // to destroy, but it's recommended to do the cleanup properly
        engine.destroy()
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // Schedule the next frame
            choreographer.postFrameCallback(this)

            // This check guarantees that we have a swap chain
            if (uiHelper.isReadyToRender) {
                // If beginFrame() returns false you should skip the frame
                // This means you are sending frames too quickly to the GPU
                if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
            }
        }
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
            displayHelper.attach(renderer, surfaceView.display)
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                engine.destroySwapChain(it)
                // Required to ensure we don't return before Filament is done executing the
                // destroySwapChain command, otherwise Android might destroy the Surface
                // too early
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            val aspect = width.toDouble() / height.toDouble()
            camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)

            view.viewport = Viewport(0, 0, width, height)

            FilamentHelper.synchronizePendingFrames(engine)
        }
    }

    private fun readUncompressedAsset(assetName: String): ByteBuffer {
        assets.openFd(assetName).use { fd ->
            val input = fd.createInputStream()
            val dst = ByteBuffer.allocate(fd.length.toInt())

            val src = Channels.newChannel(input)
            src.read(dst)
            src.close()

            return dst.apply { rewind() }
        }
    }
}
