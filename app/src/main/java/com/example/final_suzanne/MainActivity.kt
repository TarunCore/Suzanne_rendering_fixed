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
import android.annotation.SuppressLint
import android.app.Activity
import android.opengl.Matrix
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView

import com.google.android.filament.*
import com.google.android.filament.RenderableManager.*
import com.google.android.filament.VertexBuffer.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity() {
    // Make sure to initialize Filament first
    // This loads the JNI library needed by most API calls
    companion object {
        init {
            Filament.init()
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
    private lateinit var vertexBuffer: VertexBuffer
    private lateinit var indexBuffer: IndexBuffer

    // Filament entity representing a renderable object
    @Entity private var renderable = 0

    // A swap chain is Filament's representation of a surface
    private var swapChain: SwapChain? = null

    // Performs the rendering and schedules new frames
    private val frameScheduler = FrameCallback()

    private val animator = ValueAnimator.ofFloat(0.0f, 360.0f)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        choreographer = Choreographer.getInstance()

        displayHelper = DisplayHelper(this)

        surfaceView = SurfaceView(this)

        val textView = TextView(this).apply {
            val d = resources.displayMetrics.density
            text = "This TextView is under the Filament SurfaceView."
            textSize = 32.0f
            setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
        }

        setContentView(FrameLayout(this).apply {
            addView(textView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
            ))
            addView(surfaceView)
        })

        setupSurfaceView()
        setupFilament()
        setupView()
        setupScene()
    }

    private fun setupSurfaceView() {
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()

        // Make the render target transparent
        uiHelper.isOpaque = false

        uiHelper.attachTo(surfaceView)
    }

    private fun setupFilament() {
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())

        // clear the swapchain with transparent pixels
        renderer.clearOptions = renderer.clearOptions.apply {
            clear = true
        }
    }

    private fun setupView() {
        // Tell the view which camera we want to use
        view.camera = camera

        // Tell the view which scene we want to render
        view.scene = scene
    }

    private fun setupScene() {
        loadMaterial()
        createMesh()

        // To create a renderable we first create a generic entity
        renderable = EntityManager.get().create()

        // We then create a renderable component on that entity
        // A renderable is made of several primitives; in this case we declare only 1
        RenderableManager.Builder(1)
            // Overall bounding box of the renderable
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.01f))
            // Sets the mesh data of the first primitive
            .geometry(0, PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer, 0, 3)
            // Sets the material of the first primitive
            .material(0, material.defaultInstance)
            .build(engine, renderable)

        // Add the entity to the scene to render it
        scene.addEntity(renderable)

        startAnimation()
    }

    private fun loadMaterial() {
        readUncompressedAsset("materials/baked_color.filamat").let {
            material = Material.Builder().payload(it, it.remaining()).build(engine)
        }
    }

    private fun createMesh() {
        val intSize = 4
        val floatSize = 4
        val shortSize = 2
        // A vertex is a position + a color:
        // 3 floats for XYZ position, 1 integer for color
        val vertexSize = 3 * floatSize + intSize

        // Define a vertex and a function to put a vertex in a ByteBuffer
        data class Vertex(val x: Float, val y: Float, val z: Float, val color: Int)
        fun ByteBuffer.put(v: Vertex): ByteBuffer {
            putFloat(v.x)
            putFloat(v.y)
            putFloat(v.z)
            putInt(v.color)
            return this
        }

        // We are going to generate a single triangle
        val vertexCount = 3
        val a1 = PI * 2.0 / 3.0
        val a2 = PI * 4.0 / 3.0

        val vertexData = ByteBuffer.allocate(vertexCount * vertexSize)
            // It is important to respect the native byte order
            .order(ByteOrder.nativeOrder())
            .put(Vertex(1.0f,              0.0f,              0.0f, 0xffff0000.toInt()))
            .put(Vertex(cos(a1).toFloat(), sin(a1).toFloat(), 0.0f, 0xff00ff00.toInt()))
            .put(Vertex(cos(a2).toFloat(), sin(a2).toFloat(), 0.0f, 0xff0000ff.toInt()))
            // Make sure the cursor is pointing in the right place in the byte buffer
            .flip()

        // Declare the layout of our mesh
        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(vertexCount)
            // Because we interleave position and color data we must specify offset and stride
            // We could use de-interleaved data by declaring two buffers and giving each
            // attribute a different buffer index
            .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0,             vertexSize)
            .attribute(VertexAttribute.COLOR,    0, AttributeType.UBYTE4, 3 * floatSize, vertexSize)
            // We store colors as unsigned bytes but since we want values between 0 and 1
            // in the material (shaders), we must mark the attribute as normalized
            .normalized(VertexAttribute.COLOR)
            .build(engine)

        // Feed the vertex data to the mesh
        // We only set 1 buffer because the data is interleaved
        vertexBuffer.setBufferAt(engine, 0, vertexData)

        // Create the indices
        val indexData = ByteBuffer.allocate(vertexCount * shortSize)
            .order(ByteOrder.nativeOrder())
            .putShort(0)
            .putShort(1)
            .putShort(2)
            .flip()

        indexBuffer = IndexBuffer.Builder()
            .indexCount(3)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)
        indexBuffer.setBuffer(engine, indexData)
    }

    private fun startAnimation() {
        // Animate the triangle
        animator.interpolator = LinearInterpolator()
        animator.duration = 4000
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            val transformMatrix = FloatArray(16)
            override fun onAnimationUpdate(a: ValueAnimator) {
                Matrix.setRotateM(transformMatrix, 0, -(a.animatedValue as Float), 0.0f, 0.0f, 1.0f)
                val tcm = engine.transformManager
                tcm.setTransform(tcm.getInstance(renderable), transformMatrix)
            }
        })
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
        engine.destroyEntity(renderable)
        engine.destroyRenderer(renderer)
        engine.destroyVertexBuffer(vertexBuffer)
        engine.destroyIndexBuffer(indexBuffer)
        engine.destroyMaterial(material)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)

        // Engine.destroyEntity() destroys Filament related resources only
        // (components), not the entity itself
        val entityManager = EntityManager.get()
        entityManager.destroy(renderable)
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
            swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
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
            val zoom = 1.5
            val aspect = width.toDouble() / height.toDouble()
            camera.setProjection(Camera.Projection.ORTHO,
                -aspect * zoom, aspect * zoom, -zoom, zoom, 0.0, 10.0)

            view.viewport = Viewport(0, 0, width, height)

            FilamentHelper.synchronizePendingFrames(engine)
        }
    }

    @Suppress("SameParameterValue")
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
