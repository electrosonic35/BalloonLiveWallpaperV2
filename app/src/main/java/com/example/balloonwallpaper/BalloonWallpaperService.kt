package com.example.balloonwallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Version 2 live wallpaper.
 *
 * The wallpaper does not enable touch events. Android therefore leaves normal
 * home-screen gestures and icon taps to the launcher while this service draws
 * behind them.
 */
class BalloonWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = BalloonEngine()

    inner class BalloonEngine : Engine() {

        private val random = Random(System.currentTimeMillis())
        private val frameIntervalMs = 16L

        private var running = false
        private var lastFrameNanos = 0L
        private var lastWindChangeNanos = 0L

        private var width = 1f
        private var height = 1f

        // World state: position in pixels, velocity in pixels/second.
        private var x = 0f
        private var y = 0f
        private var vx = 0f
        private var vy = 0f

        // Slowly changing virtual wind, in pixels/second^2.
        private var windAcceleration = 0f
        private var targetWindAcceleration = 0f

        private val balloonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
            color = Color.rgb(110, 110, 110)
        }
        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val knotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val balloonPath = Path()
        private val leftStringPath = Path()
        private val rightStringPath = Path()

        private val frameRunnable = object : Runnable {
            override fun run() {
                if (!running) return

                updatePhysics()
                drawFrame()
                postNextFrame()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // Deliberately do NOT call setTouchEventsEnabled(true).
            // Launcher touch interaction therefore remains unaffected.
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                startAnimation()
            } else {
                stopAnimation()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            surfaceWidth: Int,
            surfaceHeight: Int
        ) {
            super.onSurfaceChanged(holder, format, surfaceWidth, surfaceHeight)
            width = surfaceWidth.toFloat().coerceAtLeast(1f)
            height = surfaceHeight.toFloat().coerceAtLeast(1f)

            if (x == 0f && y == 0f) {
                x = width * 0.5f
                y = height * 0.55f
                vx = width * 0.035f
                vy = -height * 0.012f
            } else {
                x = x.coerceIn(90f, (width - 90f).coerceAtLeast(90f))
                y = y.coerceIn(130f, (height - 260f).coerceAtLeast(130f))
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopAnimation()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            stopAnimation()
            super.onDestroy()
        }

        private fun startAnimation() {
            running = true
            lastFrameNanos = System.nanoTime()
            lastWindChangeNanos = lastFrameNanos
            removeCallbacks()
            frameRunnable.run()
        }

        private fun stopAnimation() {
            running = false
            removeCallbacks()
        }

        private fun removeCallbacks() {
            HandlerHolder.main.removeCallbacks(frameRunnable)
        }

        private fun postNextFrame() {
            HandlerHolder.main.postDelayed(frameRunnable, frameIntervalMs)
        }

        private fun updatePhysics() {
            val now = System.nanoTime()
            var dt = (now - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = now

            // Prevent a pause/resume or slow frame from producing a huge jump.
            dt = dt.coerceIn(0.001f, 0.05f)

            val secondsSinceWindChange = (now - lastWindChangeNanos) / 1_000_000_000f
            if (secondsSinceWindChange > random.nextFloat() * 2.5f + 1.2f) {
                targetWindAcceleration = random.nextFloat() * 36f - 18f
                lastWindChangeNanos = now
            }

            // Wind changes gradually rather than instantly.
            val windResponse = (1f - kotlin.math.exp(-1.2f * dt)).toFloat()
            windAcceleration += (targetWindAcceleration - windAcceleration) * windResponse

            // Approximate forces:
            // - buoyancy keeps the balloon slightly lighter than air
            // - gravity pulls down
            // - quadratic-ish drag damps high velocities
            // - wind supplies horizontal acceleration
            val buoyancy = -11f
            val gravity = 7f
            val airDragX = 0.16f * vx * abs(vx)
            val airDragY = 0.10f * vy * abs(vy)

            // Very gentle turbulent side-to-side force.
            val t = now / 1_000_000_000.0
            val turbulence = (sin(t * 0.55) * 3.0 + cos(t * 0.21) * 1.5).toFloat()

            val ax = windAcceleration + turbulence - airDragX
            val ay = gravity + buoyancy - airDragY

            vx += ax * dt
            vy += ay * dt

            x += vx * dt
            y += vy * dt

            resolveBoundaries()
        }

        private fun resolveBoundaries() {
            val halfWidth = 58f
            val top = 105f
            val bottom = (height - 250f).coerceAtLeast(top + 50f)
            val left = halfWidth
            val right = (width - halfWidth).coerceAtLeast(left)

            if (x < left) {
                x = left
                vx = abs(vx) * 0.72f
            } else if (x > right) {
                x = right
                vx = -abs(vx) * 0.72f
            }

            if (y < top) {
                y = top
                vy = abs(vy) * 0.58f
            } else if (y > bottom) {
                y = bottom
                vy = -abs(vy) * 0.58f
            }
        }

        private fun drawFrame() {
            val canvas = try {
                surfaceHolder.lockCanvas()
            } catch (_: Exception) {
                null
            } ?: return

            try {
                // Intentionally black. The launcher icons remain above the wallpaper.
                canvas.drawColor(Color.BLACK)

                val speed = kotlin.math.sqrt(vx * vx + vy * vy)
                val tilt = (vx * 0.035f).coerceIn(-7f, 7f)

                drawBalloon(canvas, tilt, speed)
            } finally {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {
                    // Surface may have disappeared between lock and post.
                }
            }
        }

        private fun drawBalloon(canvas: Canvas, tiltDegrees: Float, speed: Float) {
            val bodyWidth = 112f
            val bodyHeight = 155f
            val stringLength = 125f

            // A small velocity-dependent lean makes the balloon feel less rigid.
            canvas.save()
            canvas.rotate(tiltDegrees, x, y)

            val body = RectF(
                x - bodyWidth / 2f,
                y - bodyHeight / 2f,
                x + bodyWidth / 2f,
                y + bodyHeight / 2f
            )

            // Soft shadow/edge gives the cartoon body depth without external assets.
            shadowPaint.shader = RadialGradient(
                x + 16f,
                y + 32f,
                bodyWidth * 0.72f,
                Color.argb(80, 0, 0, 0),
                Color.argb(0, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(body, shadowPaint)

            balloonPaint.shader = LinearGradient(
                body.left,
                body.top,
                body.right,
                body.bottom,
                Color.rgb(112, 222, 116),
                Color.rgb(27, 139, 39),
                Shader.TileMode.CLAMP
            )

            balloonPath.reset()
            balloonPath.moveTo(x, y + bodyHeight * 0.50f)
            balloonPath.cubicTo(
                x - 12f, y + bodyHeight * 0.38f,
                x - bodyWidth * 0.50f, y + bodyHeight * 0.22f,
                x - bodyWidth * 0.50f, y - bodyHeight * 0.08f
            )
            balloonPath.cubicTo(
                x - bodyWidth * 0.50f, y - bodyHeight * 0.46f,
                x - bodyWidth * 0.24f, y - bodyHeight * 0.50f,
                x, y - bodyHeight * 0.50f
            )
            balloonPath.cubicTo(
                x + bodyWidth * 0.24f, y - bodyHeight * 0.50f,
                x + bodyWidth * 0.50f, y - bodyHeight * 0.46f,
                x + bodyWidth * 0.50f, y - bodyHeight * 0.08f
            )
            balloonPath.cubicTo(
                x + bodyWidth * 0.50f, y + bodyHeight * 0.22f,
                x + 12f, y + bodyHeight * 0.38f,
                x, y + bodyHeight * 0.50f
            )
            balloonPath.close()
            canvas.drawPath(balloonPath, balloonPaint)

            // Cartoon highlight.
            highlightPaint.shader = RadialGradient(
                x - 27f,
                y - 43f,
                39f,
                Color.argb(190, 225, 255, 225),
                Color.argb(0, 225, 255, 225),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(
                RectF(x - 46f, y - 65f, x - 5f, y - 8f),
                highlightPaint
            )

            // Knot.
            knotPaint.color = Color.rgb(30, 116, 36)
            val knot = Path().apply {
                moveTo(x - 9f, y + bodyHeight * 0.46f)
                lineTo(x + 9f, y + bodyHeight * 0.46f)
                lineTo(x, y + bodyHeight * 0.61f)
                close()
            }
            canvas.drawPath(knot, knotPaint)

            // Strings sway with horizontal motion.
            val stringTop = y + bodyHeight * 0.57f
            val sway = (vx * 0.8f).coerceIn(-30f, 30f)
            val stringBottom = stringTop + stringLength

            leftStringPath.reset()
            leftStringPath.moveTo(x - 4f, stringTop)
            leftStringPath.cubicTo(
                x - 30f + sway,
                stringTop + stringLength * 0.32f,
                x + 23f + sway,
                stringTop + stringLength * 0.70f,
                x - 2f + sway * 0.5f,
                stringBottom
            )

            rightStringPath.reset()
            rightStringPath.moveTo(x + 4f, stringTop)
            rightStringPath.cubicTo(
                x + 30f + sway,
                stringTop + stringLength * 0.32f,
                x - 23f + sway,
                stringTop + stringLength * 0.70f,
                x + 2f + sway * 0.5f,
                stringBottom
            )

            // Slightly thicker when moving quickly, giving a subtle sense of motion.
            stringPaint.strokeWidth = (2.0f + speed * 0.01f).coerceIn(2f, 3.2f)
            canvas.drawPath(leftStringPath, stringPaint)
            canvas.drawPath(rightStringPath, stringPaint)

            canvas.restore()
        }
    }

    /** Small holder so the engine can share one UI-thread handler without creating one per frame. */
    private object HandlerHolder {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
    }
}
