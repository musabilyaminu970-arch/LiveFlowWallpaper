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
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class FlowWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return FlowEngine()
    }

    private inner class FlowEngine : Engine() {

        private var running = false
        private var thread: Thread? = null

        private var backgroundBitmap: Bitmap? = null
        private var flowBitmap: Bitmap? = null

        private val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG
        )

        private val random =
            Random(System.currentTimeMillis())

        private val columns = 6
        private val rows = 6
        private val pieceCount = 36

        private val startX =
            FloatArray(pieceCount)

        private val startY =
            FloatArray(pieceCount)

        private val targetX =
            FloatArray(pieceCount)

        private val targetY =
            FloatArray(pieceCount)

        private val driftX =
            FloatArray(pieceCount)

        private val driftY =
            FloatArray(pieceCount)

        private val rotation =
            FloatArray(pieceCount)

        private val rotationSpeed =
            FloatArray(pieceCount)

        private val randomPhase =
            FloatArray(pieceCount)

        private val randomRadius =
            FloatArray(pieceCount)

        private var width = 0
        private var height = 0

        private var animationStart = 0L

        private val animationLength = 14000L

        override fun onVisibilityChanged(
            visible: Boolean
        ) {
            running = visible

            if (visible) {
                loadPhotos()
                createPieces()

                animationStart =
                    System.currentTimeMillis()

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

            this.width = width
            this.height = height

            loadPhotos()
            createPieces()

            animationStart =
                System.currentTimeMillis()

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

        private fun createPieces() {

            if (width <= 0 || height <= 0) {
                return
            }

            val cellWidth =
                width.toFloat() / columns

            val cellHeight =
                height.toFloat() / rows

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

                    startX[index] =
                        random.nextFloat() *
                            width

                    startY[index] =
                        random.nextFloat() *
                            height

                    driftX[index] =
                        (
                            random.nextFloat() -
                                0.5f
                            ) *
                            width *
                            0.45f

                    driftY[index] =
                        (
                            random.nextFloat() -
                                0.5f
                            ) *
                            height *
                            0.45f

                    rotation[index] =
                        random.nextFloat() *
                            720f -
                            360f

                    rotationSpeed[index] =
                        (
                            random.nextFloat() -
                                0.5f
                            ) *
                            720f

                    randomPhase[index] =
                        random.nextFloat() *
                            6.28318f

                    randomRadius[index] =
                        0.5f +
                            random.nextFloat() *
                            1.2f

                    index++
                }
            }
        }

        private fun startDrawing() {

            if (thread?.isAlive == true) {
                return
            }

            thread = Thread {

                while (running) {

                    drawFrame()

                    try {
                        Thread.sleep(16L)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }

            thread?.start()
        }

        private fun stopDrawing() {

            running = false

            thread?.interrupt()
            thread = null
        }

        private fun drawFrame() {

            val canvas =
                try {
                    surfaceHolder.lockCanvas()
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

                val time =
                    (
                        elapsed %
                            animationLength
                        ).toFloat() /
                        animationLength

                if (elapsed >= animationLength) {

                    animationStart =
                        System.currentTimeMillis()

                    createPieces()
                }

                drawAnimation(
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

        private fun drawBackground(
            canvas: Canvas
        ) {

            val bitmap =
                backgroundBitmap
                    ?: return

            drawCoverBitmap(
                canvas,
                bitmap,
                1f
            )
        }

        private fun drawAnimation(
            canvas: Canvas,
            time: Float
        ) {

            val bitmap =
                flowBitmap
                    ?: return

            if (time < 0.48f) {

                val progress =
                    smoothStep(
                        time / 0.48f
                    )

                drawPieces(
                    canvas,
                    bitmap,
                    progress,
                    false
                )

                return
            }

            if (time < 0.58f) {

                drawFullPhoto(
                    canvas,
                    bitmap,
                    1f
                )

                return
            }

            if (time < 0.68f) {

                val progress =
                    smoothStep(
                        (time - 0.58f) /
                            0.10f
                    )

                val zoom =
                    1f -
                        0.12f * progress

                drawFullPhoto(
                    canvas,
                    bitmap,
                    zoom
                )

                return
            }

            if (time < 0.78f) {

                val progress =
                    smoothStep(
                        (time - 0.68f) /
                            0.10f
                    )

                val zoom =
                    0.88f +
                        0.12f * progress

                drawFullPhoto(
                    canvas,
                    bitmap,
                    zoom
                )

                return
            }

            val progress =
                smoothStep(
                    (time - 0.78f) /
                        0.22f
                )

            drawPieces(
                canvas,
                bitmap,
                1f - progress,
                true
            )
        }
                private fun drawPieces(
            canvas: Canvas,
            bitmap: Bitmap,
            progress: Float,
            scattering: Boolean
        ) {

            val prefs =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                )

            val shapeMode =
                prefs.getInt(
                    "shape_mode",
                    0
                )

            val cellWidth =
                width.toFloat() /
                    columns

            val cellHeight =
                height.toFloat() /
                    rows

            var index = 0

            for (row in 0 until rows) {

                for (column in 0 until columns) {

                    if (index >= pieceCount) {
                        break
                    }

                    val targetCX =
                        targetX[index]

                    val targetCY =
                        targetY[index]

                    val startCX =
                        startX[index]

                    val startCY =
                        startY[index]

                    val wave =
                        sin(
                            randomPhase[index] +
                                progress * 7.5f
                        )

                    val wave2 =
                        cos(
                            randomPhase[index] +
                                progress * 5.2f
                        )

                    val randomMoveX =
                        driftX[index] *
                            progress +
                            wave *
                            cellWidth *
                            randomRadius[index]

                    val randomMoveY =
                        driftY[index] *
                            progress +
                            wave2 *
                            cellHeight *
                            randomRadius[index]

                    val centerX: Float
                    val centerY: Float

                    if (!scattering) {

                        centerX =
                            lerp(
                                startCX,
                                targetCX,
                                progress
                            ) +
                                randomMoveX *
                                (1f - progress)

                        centerY =
                            lerp(
                                startCY,
                                targetCY,
                                progress
                            ) +
                                randomMoveY *
                                (1f - progress)

                    } else {

                        centerX =
                            targetCX +
                                randomMoveX *
                                progress

                        centerY =
                            targetCY +
                                randomMoveY *
                                progress
                    }

                    val sizeProgress =
                        if (!scattering) {

                            0.18f +
                                0.82f *
                                smoothStep(progress)

                        } else {

                            1f -
                                0.82f *
                                smoothStep(progress)
                        }

                    val halfWidth =
                        cellWidth *
                            0.50f *
                            sizeProgress

                    val halfHeight =
                        cellHeight *
                            0.50f *
                            sizeProgress

                    val currentRotation =
                        if (!scattering) {

                            rotation[index] *
                                (1f - progress) +
                                rotationSpeed[index] *
                                sin(
                                    progress * 3f
                                ) *
                                (1f - progress)

                        } else {

                            rotation[index] +
                                rotationSpeed[index] *
                                progress
                        }

                    val sourceLeft =
                        bitmap.width *
                            column /
                            columns

                    val sourceTop =
                        bitmap.height *
                            row /
                            rows

                    val sourceRight =
                        bitmap.width *
                            (column + 1) /
                            columns

                    val sourceBottom =
                        bitmap.height *
                            (row + 1) /
                            rows

                    val source =
                        Rect(
                            sourceLeft,
                            sourceTop,
                            sourceRight,
                            sourceBottom
                        )

                    val destination =
                        RectF(
                            centerX -
                                halfWidth,

                            centerY -
                                halfHeight,

                            centerX +
                                halfWidth,

                            centerY +
                                halfHeight
                        )

                    canvas.save()

                    canvas.rotate(
                        currentRotation,
                        centerX,
                        centerY
                    )

                    val path =
                        createShapePath(
                            shapeMode,
                            centerX,
                            centerY,
                            halfWidth,
                            halfHeight,
                            index
                        )

                    canvas.clipPath(path)

                    canvas.drawBitmap(
                        bitmap,
                        source,
                        destination,
                        paint
                    )

                    canvas.restore()

                    index++
                }
            }
        }

        private fun createShapePath(
            shapeMode: Int,
            cx: Float,
            cy: Float,
            halfWidth: Float,
            halfHeight: Float,
            index: Int
        ): Path {

            val path = Path()

            var actualShape =
                shapeMode

            if (shapeMode == 5) {

                actualShape =
                    index % 5
            }

            when (actualShape) {

                0 -> {

                    path.addOval(
                        RectF(
                            cx - halfWidth,
                            cy - halfHeight,
                            cx + halfWidth,
                            cy + halfHeight
                        ),
                        Path.Direction.CW
                    )
                }

                1 -> {

                    path.addRect(
                        RectF(
                            cx - halfWidth,
                            cy - halfHeight,
                            cx + halfWidth,
                            cy + halfHeight
                        ),
                        Path.Direction.CW
                    )
                }

                2 -> {

                    addPolygon(
                        path,
                        cx,
                        cy,
                        min(
                            halfWidth,
                            halfHeight
                        ),
                        3
                    )
                }

                3 -> {

                    addPolygon(
                        path,
                        cx,
                        cy,
                        min(
                            halfWidth,
                            halfHeight
                        ),
                        5
                    )
                }

                else -> {

                    addPolygon(
                        path,
                        cx,
                        cy,
                        min(
                            halfWidth,
                            halfHeight
                        ),
                        6
                    )
                }
            }

            return path
        }

        private fun addPolygon(
            path: Path,
            cx: Float,
            cy: Float,
            radius: Float,
            sides: Int
        ) {

            for (i in 0 until sides) {

                val angle =
                    -Math.PI / 2.0 +
                        i *
                        2.0 *
                        Math.PI /
                        sides

                val x =
                    cx +
                        cos(
                            angle
                        ).toFloat() *
                        radius

                val y =
                    cy +
                        sin(
                            angle
                        ).toFloat() *
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

            val centerX =
                width / 2f

            val centerY =
                height / 2f

            val scale =
                maxOf(
                    width.toFloat() /
                        bitmap.width,

                    height.toFloat() /
                        bitmap.height
                ) * zoom

            val drawWidth =
                bitmap.width *
                    scale

            val drawHeight =
                bitmap.height *
                    scale

            val destination =
                RectF(
                    centerX -
                        drawWidth / 2f,

                    centerY -
                        drawHeight / 2f,

                    centerX +
                        drawWidth / 2f,

                    centerY +
                        drawHeight / 2f
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
            zoom: Float
        ) {

            val scale =
                maxOf(
                    width.toFloat() /
                        bitmap.width,

                    height.toFloat() /
                        bitmap.height
                ) * zoom

            val drawWidth =
                bitmap.width *
                    scale

            val drawHeight =
                bitmap.height *
                    scale

            val left =
                (width - drawWidth) /
                    2f

            val top =
                (height - drawHeight) /
                    2f

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

        private fun lerp(
            a: Float,
            b: Float,
            t: Float
        ): Float {

            return a +
                (b - a) * t
        }

        private fun smoothStep(
            value: Float
        ): Float {

            val t =
                value.coerceIn(
                    0f,
                    1f
                )

            return t * t *
                (3f - 2f * t)
        }
    }
}
