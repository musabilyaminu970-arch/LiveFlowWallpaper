package com.example.liveflowwallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class FlowWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return FlowEngine()
    }

    private inner class FlowEngine : Engine() {

        private var running = false
        private var drawingThread: Thread? = null

        private var backgroundBitmap: Bitmap? = null
        private var flowBitmap: Bitmap? = null

        private var screenWidth = 0
        private var screenHeight = 0

        private val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
            )

        private val random =
            Random(System.currentTimeMillis())

        /*
         * 64 fragments.
         *
         * The photograph is divided into an 8 x 8 source grid.
         * Each fragment always keeps the correct part of Photo 2.
         */
        private val columns = 8
        private val rows = 8
        private val pieceCount = 64

        private val startX =
            FloatArray(pieceCount)

        private val startY =
            FloatArray(pieceCount)

        private val targetX =
            FloatArray(pieceCount)

        private val targetY =
            FloatArray(pieceCount)

        private val startScale =
            FloatArray(pieceCount)

        private val targetScale =
            FloatArray(pieceCount)

        private val startRotation =
            FloatArray(pieceCount)

        private val targetRotation =
            FloatArray(pieceCount)

        private val driftX =
            FloatArray(pieceCount)

        private val driftY =
            FloatArray(pieceCount)

        private val wobbleX =
            FloatArray(pieceCount)

        private val wobbleY =
            FloatArray(pieceCount)

        private val wobbleSpeed =
            FloatArray(pieceCount)

        private val wobblePhase =
            FloatArray(pieceCount)

        private val sourceRects =
            arrayOfNulls<Rect>(pieceCount)

        private val shapeTypes =
            IntArray(pieceCount)

        /*
         * Animation timing.
         *
         * 0.00 - 0.38  : fragments appear and assemble
         * 0.38 - 0.48  : final reconstruction
         * 0.48 - 0.60  : zoom OUT
         * 0.60 - 0.72  : zoom IN
         * 0.72 - 1.00  : fragments break apart and disappear
         */
        private val cycleLength = 16000L

        private var animationStart = 0L

        private var selectedShape = 0
        private var selectedSpeed = 50

        override fun onVisibilityChanged(
            visible: Boolean
        ) {

            running = visible

            if (visible) {

                loadSettings()
                loadPhotos()

                animationStart =
                    System.currentTimeMillis()

                createNewCycle()

                startDrawing()

            } else {

                stopDrawing()
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

            screenWidth = width
            screenHeight = height

            loadSettings()
            loadPhotos()

            animationStart =
                System.currentTimeMillis()

            createNewCycle()

            if (running) {
                startDrawing()
            }
        }

        override fun onSurfaceDestroyed(
            holder: SurfaceHolder
        ) {

            stopDrawing()

            super.onSurfaceDestroyed(holder)
        }

        private fun loadSettings() {

            val prefs =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                )

            selectedShape =
                prefs.getInt(
                    "shape_mode",
                    0
                )

            selectedSpeed =
                prefs.getInt(
                    "speed",
                    50
                )
        }

        private fun loadPhotos() {

            val prefs =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                )

            val backgroundUri =
                prefs.getString(
                    "background_uri",
                    null
                )

            val flowUri =
                prefs.getString(
                    "flow_uri",
                    null
                )

            backgroundBitmap =
                decodePhoto(backgroundUri)

            flowBitmap =
                decodePhoto(flowUri)
        }

        private fun decodePhoto(
            value: String?
        ): Bitmap? {

            if (value == null) {
                return null
            }

            return try {

                val uri =
                    Uri.parse(value)

                val stream =
                    contentResolver
                        .openInputStream(uri)

                val bitmap =
                    BitmapFactory
                        .decodeStream(stream)

                stream?.close()

                bitmap

            } catch (_: Exception) {

                null
            }
        }

        private fun createNewCycle() {

            if (
                screenWidth <= 0 ||
                screenHeight <= 0
            ) {
                return
            }

            createSourceRects()
            createFragmentPositions()
            createFragmentShapes()
        }

        private fun createSourceRects() {

            val bitmap =
                flowBitmap ?: return

            val cellWidth =
                bitmap.width.toFloat() /
                    columns

            val cellHeight =
                bitmap.height.toFloat() /
                    rows

            var index = 0

            for (row in 0 until rows) {

                for (column in 0 until columns) {

                    if (index >= pieceCount) {
                        return
                    }

                    val left =
                        (column * cellWidth)
                            .toInt()

                    val top =
                        (row * cellHeight)
                            .toInt()

                    val right =
                        if (column == columns - 1) {
                            bitmap.width
                        } else {
                            ((column + 1) *
                                cellWidth).toInt()
                        }

                    val bottom =
                        if (row == rows - 1) {
                            bitmap.height
                        } else {
                            ((row + 1) *
                                cellHeight).toInt()
                        }

                    sourceRects[index] =
                        Rect(
                            left,
                            top,
                            right,
                            bottom
                        )

                    index++
                }
            }
        }

        private fun createFragmentPositions() {

            val cellWidth =
                screenWidth.toFloat() /
                    columns

            val cellHeight =
                screenHeight.toFloat() /
                    rows

            var index = 0

            for (row in 0 until rows) {

                for (column in 0 until columns) {

                    if (index >= pieceCount) {
                        return
                    }

                    targetX[index] =
                        column * cellWidth +
                            cellWidth / 2f

                    targetY[index] =
                        row * cellHeight +
                            cellHeight / 2f

                    /*
                     * Start randomly around the whole
                     * screen instead of following a
                     * predictable pattern.
                     */
                    startX[index] =
                        random.nextFloat() *
                            screenWidth

                    startY[index] =
                        random.nextFloat() *
                            screenHeight

                    startScale[index] =
                        0.08f +
                            random.nextFloat() *
                            0.20f

                    targetScale[index] =
                        1f

                    startRotation[index] =
                        random.nextFloat() *
                            720f -
                            360f

                    targetRotation[index] =
                        (
                            random.nextFloat() -
                                0.5f
                            ) * 12f

                    driftX[index] =
                        (
                            random.nextFloat() -
                                0.5f
                            ) *
                            screenWidth *
                            0.30f

                    driftY[index] =
                        (
                            random.nextFloat() -
                                0.5f
                            ) *
                            screenHeight *
                            0.30f

                    wobbleX[index] =
                        5f +
                            random.nextFloat() *
                            30f

                    wobbleY[index] =
                        5f +
                            random.nextFloat() *
                            30f

                    wobbleSpeed[index] =
                        0.8f +
                            random.nextFloat() *
                            2.0f

                    wobblePhase[index] =
                        random.nextFloat() *
                            6.28318f

                    index++
                }
            }
        }

        private fun createFragmentShapes() {

            for (index in 0 until pieceCount) {

                shapeTypes[index] =
                    when (selectedShape) {

                        0 -> 0       // Circle
                        1 -> 1       // Rectangle
                        2 -> 2       // Triangle
                        3 -> 3       // Pentagon
                        4 -> 4       // Hexagon

                        else -> {
                            /*
                             * Random mode.
                             */
                            random.nextInt(0, 5)
                        }
                    }
            }
        }

        private fun startDrawing() {

            if (
                drawingThread?.isAlive == true
            ) {
                return
            }

            drawingThread =
                Thread {

                    while (running) {

                        drawFrame()

                        try {

                            Thread.sleep(
                                frameDelay()
                            )

                        } catch (
                            _: InterruptedException
                        ) {

                            break
                        }
                    }
                }

            drawingThread?.start()
        }

        private fun stopDrawing() {

            running = false

            drawingThread?.interrupt()

            drawingThread = null
        }

        private fun frameDelay(): Long {

            /*
             * Speed affects how quickly frames are
             * produced without making the animation
             * jumpy.
             */
            return when {

                selectedSpeed >= 80 -> 12L

                selectedSpeed >= 60 -> 14L

                selectedSpeed >= 40 -> 16L

                selectedSpeed >= 20 -> 18L

                else -> 22L
            }
        }

        private fun drawFrame() {

            val canvas =
                try {

                    surfaceHolder
                        .lockCanvas()

                } catch (_: Exception) {

                    null
                }

            if (canvas == null) {
                return
            }

            try {

                canvas.drawColor(
                    Color.BLACK
                )

                drawBackground(canvas)

                val elapsed =
                    System.currentTimeMillis() -
                        animationStart

                val adjustedCycle =
                    cycleLength *
                        speedMultiplier()

                val time =
                    (
                        elapsed %
                            adjustedCycle
                        ).toFloat() /
                        adjustedCycle

                if (
                    elapsed >=
                        adjustedCycle
                ) {

                    animationStart =
                        System.currentTimeMillis()

                    createNewCycle()
                }

                drawFlowAnimation(
                    canvas,
                    time
                )

            } finally {

                try {

                    surfaceHolder
                        .unlockCanvasAndPost(
                            canvas
                        )

                } catch (_: Exception) {
                }
            }
        }

        private fun speedMultiplier(): Float {

            return when {

                selectedSpeed >= 80 -> 0.72f

                selectedSpeed >= 60 -> 0.85f

                selectedSpeed >= 40 -> 1.0f

                selectedSpeed >= 20 -> 1.18f

                else -> 1.35f
            }
        }
                private fun drawBackground(
            canvas: Canvas
        ) {

            val bitmap =
                backgroundBitmap ?: return

            drawCoverBitmap(
                canvas,
                bitmap,
                1f
            )
        }

        private fun drawFlowAnimation(
            canvas: Canvas,
            time: Float
        ) {

            val bitmap =
                flowBitmap ?: return

            /*
             * PHASE 1
             *
             * Photo 2 fragments appear small and
             * randomly scattered, then travel toward
             * their exact positions while growing.
             */
            if (time < 0.40f) {

                val progress =
                    smoothStep(
                        time / 0.40f
                    )

                drawFragments(
                    canvas,
                    bitmap,
                    progress,
                    false
                )

                return
            }

            /*
             * PHASE 2
             *
             * The last fragments finish their movement.
             * This gives the photograph a smooth,
             * complete reconstruction.
             */
            if (time < 0.48f) {

                val progress =
                    smoothStep(
                        (time - 0.40f) /
                            0.08f
                    )

                drawFragments(
                    canvas,
                    bitmap,
                    1f,
                    false
                )

                drawFullPhoto(
                    canvas,
                    bitmap,
                    0.96f +
                        0.04f * progress
                )

                return
            }

            /*
             * PHASE 3
             *
             * Complete Photo 2 smoothly zooms OUT.
             */
            if (time < 0.60f) {

                val progress =
                    smoothStep(
                        (time - 0.48f) /
                            0.12f
                    )

                val zoom =
                    1f -
                        0.15f * progress

                drawFullPhoto(
                    canvas,
                    bitmap,
                    zoom
                )

                return
            }

            /*
             * PHASE 4
             *
             * Complete Photo 2 smoothly zooms IN.
             */
            if (time < 0.72f) {

                val progress =
                    smoothStep(
                        (time - 0.60f) /
                            0.12f
                    )

                val zoom =
                    0.85f +
                        0.15f * progress

                drawFullPhoto(
                    canvas,
                    bitmap,
                    zoom
                )

                return
            }

            /*
             * PHASE 5
             *
             * The actual photograph breaks back into
             * its original fragments.
             *
             * The fragments move away, rotate and shrink
             * until they disappear.
             */
            val progress =
                smoothStep(
                    (time - 0.72f) /
                        0.28f
                )

            drawFragments(
                canvas,
                bitmap,
                1f - progress,
                true
            )
        }

        private fun drawFragments(
            canvas: Canvas,
            bitmap: Bitmap,
            progress: Float,
            scattering: Boolean
        ) {

            val cellWidth =
                screenWidth.toFloat() /
                    columns

            val cellHeight =
                screenHeight.toFloat() /
                    rows

            for (index in 0 until pieceCount) {

                val source =
                    sourceRects[index]
                        ?: continue

                val baseX =
                    targetX[index]

                val baseY =
                    targetY[index]

                val movement =
                    if (!scattering) {

                        /*
                         * Assembly:
                         *
                         * start position
                         *       ↓
                         * random curved movement
                         *       ↓
                         * exact target position
                         */
                        1f - progress

                    } else {

                        /*
                         * Scattering:
                         *
                         * exact target position
                         *       ↓
                         * random outward movement
                         */
                        progress
                    }

                val curvedX =
                    sin(
                        (
                            progress *
                                3.14159f *
                                2f
                        ) +
                            wobblePhase[index]
                    ) *
                        wobbleX[index] *
                        movement

                val curvedY =
                    cos(
                        (
                            progress *
                                3.14159f *
                                2f
                        ) +
                            wobblePhase[index]
                    ) *
                        wobbleY[index] *
                        movement

                val x =
                    if (!scattering) {

                        lerp(
                            startX[index],
                            baseX,
                            progress
                        ) +
                            driftX[index] *
                            sin(
                                progress *
                                    3.14159f
                            ) *
                            (1f - progress) +
                            curvedX

                    } else {

                        baseX +
                            driftX[index] *
                                progress *
                                2.8f +
                            curvedX

                    }

                val y =
                    if (!scattering) {

                        lerp(
                            startY[index],
                            baseY,
                            progress
                        ) +
                            driftY[index] *
                            sin(
                                progress *
                                    3.14159f
                            ) *
                            (1f - progress) +
                            curvedY

                    } else {

                        baseY +
                            driftY[index] *
                                progress *
                                2.8f +
                            curvedY

                    }

                /*
                 * Pieces grow during assembly.
                 *
                 * Pieces shrink during scattering.
                 */
                val scale =
                    if (!scattering) {

                        startScale[index] +
                            (
                                targetScale[index] -
                                    startScale[index]
                                ) *
                            smoothStep(progress)

                    } else {

                        targetScale[index] *
                            (
                                1f -
                                    0.92f *
                                    smoothStep(progress)
                                )

                    }

                val rotation =
                    if (!scattering) {

                        lerp(
                            startRotation[index],
                            targetRotation[index],
                            smoothStep(progress)
                        )

                    } else {

                        targetRotation[index] +
                            startRotation[index] +
                            (
                                rotationDirection(index) *
                                    180f *
                                    progress
                                )

                    }

                /*
                 * Every source cell has its own
                 * corresponding piece of Photo 2.
                 */
                val destination =
                    RectF(
                        -cellWidth / 2f,
                        -cellHeight / 2f,
                        cellWidth / 2f,
                        cellHeight / 2f
                    )

                canvas.save()

                canvas.translate(
                    x,
                    y
                )

                canvas.rotate(
                    rotation
                )

                canvas.scale(
                    scale,
                    scale
                )

                drawMaskedPhotoPiece(
                    canvas,
                    bitmap,
                    source,
                    destination,
                    shapeTypes[index]
                )

                canvas.restore()
            }
        }

        private fun drawMaskedPhotoPiece(
            canvas: Canvas,
            bitmap: Bitmap,
            source: Rect,
            destination: RectF,
            shape: Int
        ) {

            val path =
                createShapePath(
                    destination,
                    shape
                )

            canvas.save()

            canvas.clipPath(path)

            canvas.drawBitmap(
                bitmap,
                source,
                destination,
                paint
            )

            canvas.restore()
        }

        private fun createShapePath(
            rect: RectF,
            shape: Int
        ): Path {

            val path = Path()

            when (shape) {

                /*
                 * Circle
                 */
                0 -> {

                    canvasCircle(
                        path,
                        rect
                    )
                }

                /*
                 * Rectangle
                 */
                1 -> {

                    path.addRect(
                        rect,
                        Path.Direction.CW
                    )
                }

                /*
                 * Triangle
                 */
                2 -> {

                    addPolygon(
                        path,
                        rect,
                        3,
                        -90f
                    )
                }

                /*
                 * Pentagon
                 */
                3 -> {

                    addPolygon(
                        path,
                        rect,
                        5,
                        -90f
                    )
                }

                /*
                 * Hexagon
                 */
                4 -> {

                    addPolygon(
                        path,
                        rect,
                        6,
                        30f
                    )
                }

                /*
                 * Safety fallback
                 */
                else -> {

                    path.addRect(
                        rect,
                        Path.Direction.CW
                    )
                }
            }

            return path
        }

        private fun canvasCircle(
            path: Path,
            rect: RectF
        ) {

            path.addOval(
                rect,
                Path.Direction.CW
            )
        }

        private fun addPolygon(
            path: Path,
            rect: RectF,
            sides: Int,
            rotationDegrees: Float
        ) {

            val centerX =
                rect.centerX()

            val centerY =
                rect.centerY()

            val radius =
                min(
                    rect.width(),
                    rect.height()
                ) / 2f

            for (i in 0 until sides) {

                val angle =
                    Math.toRadians(
                        (
                            rotationDegrees +
                                i *
                                360f /
                                sides
                            ).toDouble()
                    )

                val x =
                    centerX +
                        cos(angle).toFloat() *
                        radius

                val y =
                    centerY +
                        sin(angle).toFloat() *
                        radius

                if (i == 0) {

                    path.moveTo(
                        x,
                        y
                    )

                } else {

                    path.lineTo(
                        x,
                        y
                    )
                }
            }

            path.close()
        }

        private fun drawFullPhoto(
            canvas: Canvas,
            bitmap: Bitmap,
            zoom: Float
        ) {

            val baseScale =
                maxOf(
                    screenWidth.toFloat() /
                        bitmap.width,

                    screenHeight.toFloat() /
                        bitmap.height
                )

            val finalScale =
                baseScale * zoom

            val drawWidth =
                bitmap.width *
                    finalScale

            val drawHeight =
                bitmap.height *
                    finalScale

            val left =
                (
                    screenWidth -
                        drawWidth
                    ) / 2f

            val top =
                (
                    screenHeight -
                        drawHeight
                    ) / 2f

            val destination =
                RectF(
                    left,
                    top,
                    left + drawWidth,
                    top + drawHeight
                )

            canvas.drawBitmap(
                bitmap,
                null,
                destination,
                paint
            )
        }

        private fun drawCoverBitmap(
            canvas: Canvas,
            bitmap: Bitmap,
            scaleMultiplier: Float
        ) {

            val scale =
                maxOf(
                    screenWidth.toFloat() /
                        bitmap.width,

                    screenHeight.toFloat() /
                        bitmap.height
                ) *
                    scaleMultiplier

            val drawWidth =
                bitmap.width *
                    scale

            val drawHeight =
                bitmap.height *
                    scale

            val left =
                (
                    screenWidth -
                        drawWidth
                    ) / 2f

            val top =
                (
                    screenHeight -
                        drawHeight
                    ) / 2f

            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    left,
                    top,
                    left + drawWidth,
                    top + drawHeight
                ),
                paint
            )
        }

        private fun rotationDirection(
            index: Int
        ): Float {

            return if (
                index % 2 == 0
            ) {
                1f
            } else {
                -1f
            }
        }

        private fun lerp(
            start: Float,
            end: Float,
            amount: Float
        ): Float {

            return start +
                (
                    end - start
                    ) *
                amount
        }

        private fun smoothStep(
            value: Float
        ): Float {

            val x =
                value.coerceIn(
                    0f,
                    1f
                )

            return x * x *
                (
                    3f -
                        2f * x
                    )
        }
    }
}
