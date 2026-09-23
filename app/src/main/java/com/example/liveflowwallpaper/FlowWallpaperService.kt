package com.example.liveflowwallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FlowWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return FlowEngine()
    }

    private inner class FlowEngine : Engine() {

        private var running = false
        private var animationThread: Thread? = null

        private var canvasWidth = 0
        private var canvasHeight = 0

        private var backgroundBitmap: Bitmap? = null
        private var flowBitmap: Bitmap? = null

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bitmapPaint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG
        )

        private val random =
            Random(System.currentTimeMillis())

        private val pieces =
            mutableListOf<Piece>()

        private var state =
            AnimationState.ASSEMBLING

        private var animationStart =
            System.currentTimeMillis()

        private val assembleDuration = 6500L
        private val staticDuration = 2500L
        private val breakDuration = 5000L

        override fun onVisibilityChanged(
            visible: Boolean
        ) {
            if (visible) {
                running = true
                startAnimation()
            } else {
                running = false
                stopAnimation()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(
                holder,
                format,
                width,
                height
            )

            canvasWidth = width
            canvasHeight = height

            loadSelectedPhotos()
            createPieces()

            state =
                AnimationState.ASSEMBLING

            animationStart =
                System.currentTimeMillis()

            if (running) {
                startAnimation()
            }
        }

        override fun onSurfaceDestroyed(
            holder: SurfaceHolder
        ) {
            running = false
            stopAnimation()

            backgroundBitmap?.recycle()
            flowBitmap?.recycle()

            backgroundBitmap = null
            flowBitmap = null
            pieces.clear()

            super.onSurfaceDestroyed(holder)
        }

        private fun startAnimation() {
            if (
                animationThread?.isAlive == true
            ) {
                return
            }

            animationThread = Thread {

                while (running) {

                    drawFrame()

                    try {
                        Thread.sleep(16L)
                    } catch (
                        _: InterruptedException
                    ) {
                        break
                    }
                }
            }

            animationThread?.start()
        }

        private fun stopAnimation() {
            animationThread?.interrupt()
            animationThread = null
        }

        private fun loadSelectedPhotos() {

            if (
                canvasWidth <= 0 ||
                canvasHeight <= 0
            ) {
                return
            }

            val preferences =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                )

            val backgroundUri =
                preferences.getString(
                    "background_photo",
                    null
                )

            val flowUri =
                preferences.getString(
                    "flow_photo",
                    null
                )

            backgroundBitmap =
                loadImageToScreen(
                    backgroundUri,
                    canvasWidth,
                    canvasHeight
                )

            flowBitmap =
                loadImageToScreen(
                    flowUri,
                    canvasWidth,
                    canvasHeight
                )

            if (backgroundBitmap == null) {
                backgroundBitmap =
                    Bitmap.createBitmap(
                        canvasWidth,
                        canvasHeight,
                        Bitmap.Config.ARGB_8888
                    )

                Canvas(backgroundBitmap!!)
                    .drawColor(
                        Color.rgb(
                            8,
                            12,
                            20
                        )
                    )
            }

            if (flowBitmap == null) {
                flowBitmap =
                    Bitmap.createBitmap(
                        canvasWidth,
                        canvasHeight,
                        Bitmap.Config.ARGB_8888
                    )

                Canvas(flowBitmap!!)
                    .drawColor(
                        Color.TRANSPARENT
                    )
            }
        }

        private fun loadImageToScreen(
            uriString: String?,
            targetWidth: Int,
            targetHeight: Int
        ): Bitmap? {

            if (uriString.isNullOrEmpty()) {
                return null
            }

            return try {

                val uri =
                    Uri.parse(uriString)

                val input =
                    contentResolver
                        .openInputStream(uri)

                val original =
                    BitmapFactory.decodeStream(input)

                input?.close()

                if (original == null) {
                    return null
                }

                val scale =
                    maxOf(
                        targetWidth.toFloat() /
                            original.width,
                        targetHeight.toFloat() /
                            original.height
                    )

                val scaledWidth =
                    (original.width * scale)
                        .toInt()

                val scaledHeight =
                    (original.height * scale)
                        .toInt()

                val scaled =
                    Bitmap.createScaledBitmap(
                        original,
                        scaledWidth,
                        scaledHeight,
                        true
                    )

                if (
                    scaled !== original &&
                    !original.isRecycled
                ) {
                    original.recycle()
                }

                val left =
                    (scaledWidth -
                        targetWidth) / 2

                val top =
                    (scaledHeight -
                        targetHeight) / 2

                val result =
                    Bitmap.createBitmap(
                        scaled,
                        left.coerceAtLeast(0),
                        top.coerceAtLeast(0),
                        targetWidth.coerceAtMost(
                            scaled.width
                        ),
                        targetHeight.coerceAtMost(
                            scaled.height
                        )
                    )

                if (
                    result !== scaled &&
                    !scaled.isRecycled
                ) {
                    scaled.recycle()
                }

                result

            } catch (_: Exception) {
                null
            }
        }

        private fun createPieces() {

            pieces.clear()

            val bitmap =
                flowBitmap ?: return

            if (
                canvasWidth <= 0 ||
                canvasHeight <= 0
            ) {
                return
            }

            val columns = 8
            val rows = 10

            val left =
                canvasWidth * 0.10f

            val top =
                canvasHeight * 0.18f

            val right =
                canvasWidth * 0.90f

            val bottom =
                canvasHeight * 0.82f

            val regionWidth =
                right - left

            val regionHeight =
                bottom - top

            val pieceWidth =
                regionWidth / columns

            val pieceHeight =
                regionHeight / rows

            val preferences =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                )

            val selectedShape =
                preferences.getString(
                    "piece_shape",
                    "Random"
                ) ?: "Random"

            for (row in 0 until rows) {

                for (column in 0 until columns) {

                    val targetLeft =
                        left +
                            column * pieceWidth

                    val targetTop =
                        top +
                            row * pieceHeight

                    val sourceLeft =
                        targetLeft
                            .toInt()
                            .coerceIn(
                                0,
                                bitmap.width - 1
                            )

                    val sourceTop =
                        targetTop
                            .toInt()
                            .coerceIn(
                                0,
                                bitmap.height - 1
                            )

                    val sourceRight =
                        (targetLeft +
                            pieceWidth)
                            .toInt()
                            .coerceIn(
                                sourceLeft + 1,
                                bitmap.width
                            )

                    val sourceBottom =
                        (targetTop +
                            pieceHeight)
                            .toInt()
                            .coerceIn(
                                sourceTop + 1,
                                bitmap.height
                            )

                    val sourceRect =
                        Rect(
                            sourceLeft,
                            sourceTop,
                            sourceRight,
                            sourceBottom
                        )

                    val pieceBitmap =
                        Bitmap.createBitmap(
                            bitmap,
                            sourceRect.left,
                            sourceRect.top,
                            sourceRect.width(),
                            sourceRect.height()
                        )

                    val shape =
                        chooseShape(
                            selectedShape
                        )

                    pieces.add(
                        Piece(
                            bitmap = pieceBitmap,
                            targetX =
                                targetLeft +
                                    pieceWidth / 2f,
                            targetY =
                                targetTop +
                                    pieceHeight / 2f,
                            width =
                                pieceWidth,
                            height =
                                pieceHeight,
                            scatterX =
                                random.nextFloat() *
                                    canvasWidth,
                            scatterY =
                                random.nextFloat() *
                                    canvasHeight,
                            startRotation =
                                random.nextFloat() *
                                    720f - 360f,
                            breakRotation =
                                random.nextFloat() *
                                    1080f - 540f,
                            startScale =
                                0.20f +
                                    random.nextFloat() *
                                    0.55f,
                            delay =
                                random.nextFloat() *
                                    0.30f,
                            shape = shape,
                            randomSides =
                                3 +
                                    random.nextInt(4)
                        )
                    )
                }
            }
        }

        private fun chooseShape(
            selectedShape: String
        ): ShapeType {

            return when (
                selectedShape.lowercase()
            ) {

                "circle" ->
                    ShapeType.CIRCLE

                "rectangle" ->
                    ShapeType.RECTANGLE

                "triangle" ->
                    ShapeType.TRIANGLE

                "pentagon" ->
                    ShapeType.PENTAGON

                "hexagon" ->
                    ShapeType.HEXAGON

                else -> {

                    val shapes =
                        ShapeType.values()
                            .filter {
                                it !=
                                    ShapeType.RANDOM
                            }

                    shapes.random(random)
                }
            }
        }
                private fun drawFrame() {

            val holder = surfaceHolder

            if (!holder.surface.isValid) {
                return
            }

            val canvas =
                try {
                    holder.lockCanvas()
                } catch (_: Exception) {
                    null
                } ?: return

            try {

                val now =
                    System.currentTimeMillis()

                val elapsed =
                    now - animationStart

                when (state) {

                    AnimationState.ASSEMBLING -> {

                        val progress =
                            (
                                elapsed.toFloat() /
                                    assembleDuration
                            ).coerceIn(
                                0f,
                                1f
                            )

                        drawBackground(canvas)

                        drawAssembling(
                            canvas,
                            progress
                        )

                        if (progress >= 1f) {

                            state =
                                AnimationState.STATIC

                            animationStart = now
                        }
                    }

                    AnimationState.STATIC -> {

                        drawBackground(canvas)

                        drawCompleteFlowPhoto(
                            canvas
                        )

                        if (
                            elapsed >=
                            staticDuration
                        ) {

                            state =
                                AnimationState.BREAKING

                            animationStart = now
                        }
                    }

                    AnimationState.BREAKING -> {

                        val progress =
                            (
                                elapsed.toFloat() /
                                    breakDuration
                            ).coerceIn(
                                0f,
                                1f
                            )

                        drawBackground(canvas)

                        drawBreaking(
                            canvas,
                            progress
                        )

                        if (progress >= 1f) {

                            createPieces()

                            state =
                                AnimationState.ASSEMBLING

                            animationStart = now
                        }
                    }
                }

            } finally {

                try {
                    holder.unlockCanvasAndPost(
                        canvas
                    )
                } catch (_: Exception) {
                }
            }
        }

        private fun drawBackground(
            canvas: Canvas
        ) {

            backgroundBitmap?.let { bitmap ->

                canvas.drawBitmap(
                    bitmap,
                    null,
                    Rect(
                        0,
                        0,
                        canvas.width,
                        canvas.height
                    ),
                    bitmapPaint
                )
            }
        }

        private fun drawCompleteFlowPhoto(
            canvas: Canvas
        ) {

            val bitmap =
                flowBitmap ?: return

            val left =
                canvasWidth * 0.10f

            val top =
                canvasHeight * 0.18f

            val right =
                canvasWidth * 0.90f

            val bottom =
                canvasHeight * 0.82f

            canvas.drawBitmap(
                bitmap,
                Rect(
                    left.toInt(),
                    top.toInt(),
                    right.toInt(),
                    bottom.toInt()
                ),
                RectF(
                    left,
                    top,
                    right,
                    bottom
                ),
                bitmapPaint
            )
        }

        private fun drawAssembling(
            canvas: Canvas,
            progress: Float
        ) {

            for (piece in pieces) {

                val pieceProgress =
                    (
                        (progress -
                            piece.delay) /
                            (1f -
                                piece.delay)
                    ).coerceIn(
                        0f,
                        1f
                    )

                val eased =
                    easeOutCubic(
                        pieceProgress
                    )

                drawPiece(
                    canvas,
                    piece,
                    eased,
                    false
                )
            }
        }

        private fun drawBreaking(
            canvas: Canvas,
            progress: Float
        ) {

            for (piece in pieces) {

                val pieceProgress =
                    (
                        (progress -
                            piece.delay) /
                            (1f -
                                piece.delay)
                    ).coerceIn(
                        0f,
                        1f
                    )

                val eased =
                    easeInCubic(
                        pieceProgress
                    )

                drawPiece(
                    canvas,
                    piece,
                    eased,
                    true
                )
            }
        }

        private fun drawPiece(
            canvas: Canvas,
            piece: Piece,
            progress: Float,
            breaking: Boolean
        ) {

            val startX =
                if (breaking)
                    piece.targetX
                else
                    piece.scatterX

            val startY =
                if (breaking)
                    piece.targetY
                else
                    piece.scatterY

            val endX =
                if (breaking)
                    piece.scatterX
                else
                    piece.targetX

            val endY =
                if (breaking)
                    piece.scatterY
                else
                    piece.targetY

            val x =
                lerp(
                    startX,
                    endX,
                    progress
                )

            val y =
                lerp(
                    startY,
                    endY,
                    progress
                )

            val rotation =
                if (breaking) {

                    lerp(
                        0f,
                        piece.breakRotation,
                        progress
                    )

                } else {

                    lerp(
                        piece.startRotation,
                        0f,
                        progress
                    )
                }

            val scale =
                if (breaking) {

                    lerp(
                        1f,
                        0.02f,
                        progress
                    )

                } else {

                    lerp(
                        piece.startScale,
                        1f,
                        progress
                    )
                }

            val alpha =
                if (breaking) {

                    (
                        (1f - progress) *
                            255f
                    ).toInt()

                } else {
                    255
                }

            val path =
                createShapePath(
                    piece,
                    scale
                )

            canvas.save()

            canvas.translate(
                x,
                y
            )

            canvas.rotate(
                rotation
            )

            canvas.clipPath(path)

            canvas.drawBitmap(
                piece.bitmap,
                null,
                RectF(
                    -piece.width *
                        scale / 2f,
                    -piece.height *
                        scale / 2f,
                    piece.width *
                        scale / 2f,
                    piece.height *
                        scale / 2f
                ),
                bitmapPaint.apply {
                    this.alpha =
                        alpha.coerceIn(
                            0,
                            255
                        )
                }
            )

            canvas.restore()

            bitmapPaint.alpha = 255
        }

        private fun createShapePath(
            piece: Piece,
            scale: Float
        ): Path {

            val width =
                piece.width * scale

            val height =
                piece.height * scale

            return when (piece.shape) {

                ShapeType.CIRCLE -> {

                    Path().apply {

                        addOval(
                            -width / 2f,
                            -height / 2f,
                            width / 2f,
                            height / 2f,
                            Path.Direction.CW
                        )
                    }
                }

                ShapeType.RECTANGLE -> {

                    Path().apply {

                        addRect(
                            -width / 2f,
                            -height / 2f,
                            width / 2f,
                            height / 2f,
                            Path.Direction.CW
                        )
                    }
                }

                ShapeType.TRIANGLE -> {

                    createPolygonPath(
                        3,
                        width / 2f,
                        height / 2f
                    )
                }

                ShapeType.PENTAGON -> {

                    createPolygonPath(
                        5,
                        width / 2f,
                        height / 2f
                    )
                }

                ShapeType.HEXAGON -> {

                    createPolygonPath(
                        6,
                        width / 2f,
                        height / 2f
                    )
                }

                ShapeType.RANDOM -> {

                    createPolygonPath(
                        piece.randomSides,
                        width / 2f,
                        height / 2f
                    )
                }
            }
        }

        private fun createPolygonPath(
            sides: Int,
            radiusX: Float,
            radiusY: Float
        ): Path {

            val path = Path()

            for (i in 0 until sides) {

                val angle =
                    -Math.PI / 2.0 +
                        (
                            2.0 *
                                Math.PI *
                                i /
                                sides
                        )

                val x =
                    cos(angle).toFloat() *
                        radiusX

                val y =
                    sin(angle).toFloat() *
                        radiusY

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            path.close()

            return path
        }

        private fun easeOutCubic(
            value: Float
        ): Float {

            val v =
                1f - value

            return 1f -
                v * v * v
        }

        private fun easeInCubic(
            value: Float
        ): Float {

            return value * value * value
        }

        private fun lerp(
            start: Float,
            end: Float,
            amount: Float
        ): Float {

            return start +
                (end - start) *
                amount
        }
    }

    private enum class AnimationState {
        ASSEMBLING,
        STATIC,
        BREAKING
    }

    private enum class ShapeType {
        CIRCLE,
        RECTANGLE,
        TRIANGLE,
        PENTAGON,
        HEXAGON,
        RANDOM
    }

    private data class Piece(
        val bitmap: Bitmap,
        val targetX: Float,
        val targetY: Float,
        val width: Float,
        val height: Float,
        val scatterX: Float,
        val scatterY: Float,
        val startRotation: Float,
        val breakRotation: Float,
        val startScale: Float,
        val delay: Float,
        val shape: ShapeType,
        val randomSides: Int
    )
}
