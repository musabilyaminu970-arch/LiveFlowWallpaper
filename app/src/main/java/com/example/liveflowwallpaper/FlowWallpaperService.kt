package com.example.liveflowwallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
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
        private var thread: Thread? = null
        private var bitmap: Bitmap? = null

        private val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
            )

        private val random =
            Random(System.currentTimeMillis())

        private var pieceX = FloatArray(0)
        private var pieceY = FloatArray(0)
        private var pieceRadius = FloatArray(0)
        private var pieceSides = IntArray(0)
        private var pieceRotation = FloatArray(0)
        private var piecePhase = FloatArray(0)
        private var pieceSpeed = FloatArray(0)
        private var pieceDistance = FloatArray(0)

        override fun onVisibilityChanged(
            visible: Boolean
        ) {
            running = visible

            if (visible) {
                loadPhoto()
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

            loadPhoto()

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

        private fun loadPhoto() {

            val saved =
                getSharedPreferences(
                    "LiveFlow",
                    MODE_PRIVATE
                ).getString(
                    "photo_uri",
                    null
                )

            if (saved == null) {
                bitmap = null
                return
            }

            try {

                val uri =
                    Uri.parse(saved)

                val stream =
                    contentResolver
                        .openInputStream(uri)

                val original =
                    BitmapFactory
                        .decodeStream(stream)

                stream?.close()

                bitmap = original

            } catch (_: Exception) {

                bitmap = null
            }
        }

        private fun startDrawing() {

            if (thread?.isAlive == true) {
                return
            }

            running = true

            thread = Thread {

                var time = 0f

                while (running) {

                    val frameStart =
                        System.nanoTime()

                    drawFrame(time)

                    time += 0.018f

                    val elapsed =
                        (
                            System.nanoTime() -
                                frameStart
                        ) / 1_000_000L

                    val sleep =
                        (
                            16L - elapsed
                        ).coerceAtLeast(1L)

                    try {

                        Thread.sleep(sleep)

                    } catch (
                        _: InterruptedException
                    ) {
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

        private fun createPieces(
            width: Float,
            height: Float
        ) {

            val count = 24

            pieceX =
                FloatArray(count)

            pieceY =
                FloatArray(count)

            pieceRadius =
                FloatArray(count)

            pieceSides =
                IntArray(count)

            pieceRotation =
                FloatArray(count)

            piecePhase =
                FloatArray(count)

            pieceSpeed =
                FloatArray(count)

            pieceDistance =
                FloatArray(count)

            val columns = 4
            val rows = 6

            val cellWidth =
                width / columns

            val cellHeight =
                height / rows

            val minDimension =
                minOf(
                    width,
                    height
                )

            var index = 0

            for (row in 0 until rows) {

                for (column in 0 until columns) {

                    pieceX[index] =
                        column *
                            cellWidth +
                            cellWidth / 2f

                    pieceY[index] =
                        row *
                            cellHeight +
                            cellHeight / 2f

                    pieceRadius[index] =
                        minOf(
                            cellWidth,
                            cellHeight
                        ) * 0.48f

                    pieceSides[index] =
                        random.nextInt(
                            4,
                            8
                        )

                    pieceRotation[index] =
                        random.nextFloat() *
                            360f

                    piecePhase[index] =
                        random.nextFloat() *
                            6.28f

                    pieceSpeed[index] =
                        0.5f +
                            random.nextFloat() *
                            0.8f

                    pieceDistance[index] =
                        minDimension *
                            (
                                0.15f +
                                    random.nextFloat() *
                                    0.55f
                            )

                    index++
                }
            }
        }

        private fun drawFrame(
            time: Float
        ) {

            val holder =
                surfaceHolder

            val canvas =
                try {

                    holder.lockCanvas()

                } catch (_: Exception) {

                    null
                } ?: return

            try {

                val width =
                    canvas.width.toFloat()

                val height =
                    canvas.height.toFloat()

                val image =
                    bitmap

                if (image == null) {

                    canvas.drawColor(
                        Color.rgb(
                            7,
                            11,
                            24
                        )
                    )

                    drawWaitingMessage(
                        canvas,
                        width,
                        height
                    )

                    return
                }

                if (
                    pieceX.size != 24
                ) {

                    createPieces(
                        width,
                        height
                    )
                }

                val cycle =
                    (
                        time / 7f
                    ) % 1f

                val reveal =
                    smoothStep(cycle)

                drawPolygonPhoto(
                    canvas,
                    image,
                    width,
                    height,
                    reveal,
                    time
                )

            } finally {

                try {

                    holder.unlockCanvasAndPost(
                        canvas
                    )

                } catch (_: Exception) {
                }
            }
        }

        private fun drawPolygonPhoto(
            canvas: Canvas,
            image: Bitmap,
            width: Float,
            height: Float,
            reveal: Float,
            time: Float
        ) {

            canvas.drawColor(
                Color.rgb(
                    3,
                    5,
                    12
                )
            )

            for (i in 0 until 24) {

                val wave =
                    sin(
                        time *
                            pieceSpeed[i] +
                            piecePhase[i]
                    ).toFloat()

                val scatter =
                    (
                        1f - reveal
                    ) *
                        pieceDistance[i]

                val direction =
                    pieceRotation[i] *
                        0.017453292f

                val dx =
                    cos(direction) *
                        scatter

                val dy =
                    sin(direction) *
                        scatter

                val centerX =
                    pieceX[i] + dx

                val centerY =
                    pieceY[i] + dy

                val zoom =
                    0.55f +
                        reveal * 0.45f +
                        wave * 0.08f

                val rotation =
                    pieceRotation[i] +
                        wave * 35f +
                        (
                            1f - reveal
                        ) * 180f

                val path =
                    createPolygonPath(
                        centerX,
                        centerY,
                        pieceRadius[i] *
                            zoom,
                        pieceSides[i],
                        rotation
                    )

                canvas.save()

                canvas.clipPath(path)

                val matrix =
                    createImageMatrix(
                        image,
                        width,
                        height
                    )

                matrix.postTranslate(
                    centerX -
                        pieceX[i],
                    centerY -
                        pieceY[i]
                )

                matrix.postScale(
                    zoom,
                    zoom,
                    centerX,
                    centerY
                )

                paint.alpha = 255

                paint.style =
                    Paint.Style.FILL

                canvas.drawBitmap(
                    image,
                    matrix,
                    paint
                )

                canvas.restore()

                paint.style =
                    Paint.Style.STROKE

                paint.strokeWidth =
                    2.5f

                paint.color =
                    Color.argb(
                        100,
                        255,
                        255,
                        255
                    )

                canvas.drawPath(
                    path,
                    paint
                )

                paint.style =
                    Paint.Style.FILL
            }

            if (reveal > 0.82f) {

                val alpha =
                    (
                        (
                            reveal -
                                0.82f
                        ) /
                            0.18f *
                            255f
                    )
                        .toInt()
                        .coerceIn(
                            0,
                            255
                        )

                paint.alpha =
                    alpha

                val matrix =
                    createImageMatrix(
                        image,
                        width,
                        height
                    )

                canvas.drawBitmap(
                    image,
                    matrix,
                    paint
                )

                paint.alpha = 255
            }
        }

        private fun createPolygonPath(
            cx: Float,
            cy: Float,
            radius: Float,
            sides: Int,
            rotation: Float
        ): Path {

            val path =
                Path()

            val rotationRadians =
                Math.toRadians(
                    rotation.toDouble()
                )

            for (
                i in 0 until sides
            ) {

                val angle =
                    rotationRadians +
                        i.toDouble() *
                        Math.PI *
                        2.0 /
                        sides.toDouble()

                val x =
                    cx +
                        cos(angle).toFloat() *
                        radius

                val y =
                    cy +
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

            return path
        }

        private fun createImageMatrix(
            bitmap: Bitmap,
            width: Float,
            height: Float
        ): Matrix {

            val matrix =
                Matrix()

            val bitmapWidth =
                bitmap.width.toFloat()

            val bitmapHeight =
                bitmap.height.toFloat()

            val scale =
                maxOf(
                    width /
                        bitmapWidth,
                    height /
                        bitmapHeight
                )

            val scaledWidth =
                bitmapWidth * scale

            val scaledHeight =
                bitmapHeight * scale

            val left =
                (
                    width -
                        scaledWidth
                ) / 2f

            val top =
                (
                    height -
                        scaledHeight
                ) / 2f

            matrix.setScale(
                scale,
                scale
            )

            matrix.postTranslate(
                left,
                top
            )

            return matrix
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
                    3f - 2f * x
                )
        }

        private fun drawWaitingMessage(
            canvas: Canvas,
            width: Float,
            height: Float
        ) {

            paint.color =
                Color.WHITE

            paint.textSize =
                42f

            paint.textAlign =
                Paint.Align.CENTER

            paint.style =
                Paint.Style.FILL

            canvas.drawText(
                "Choose a photo",
                width / 2f,
                height / 2f,
                paint
            )
        }
    }
}
