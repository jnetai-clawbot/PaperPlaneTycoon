package com.jnetai.paperplanetycoon

import android.graphics.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import android.widget.ImageView
import android.widget.ScrollView
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*
import java.util.*
import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "PaperPlaneTycoon"
        const val CURRENT_VERSION = "1.0.0"
        const val GITHUB_REPO = "jnetai-clawbot/PaperPlaneTycoon"
    }

    private lateinit var gameView: GameView
    private lateinit var aboutButton: Button
    private lateinit var scoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF0A0A1A.toInt()
        window.navigationBarColor = 0xFF0A0A1A.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0A1A.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        scoreText = TextView(this).apply {
            text = "Pull back to launch!"
            setTextColor(0xFFFF9933.toInt())
            textSize = 18f
            setPadding(32, 32, 32, 8)
            typeface = Typeface.MONOSPACE
        }

        gameView = GameView(this, ::updateScore)

        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 8, 16, 48)
        }

        val restartBtn = Button(this).apply {
            text = "Restart"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { gameView.restart() }
        }

        aboutButton = Button(this).apply {
            text = "About"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFF00FF88.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { showAbout() }
        }

        buttonBar.addView(restartBtn)
        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(32, 0) }
        buttonBar.addView(spacer)
        buttonBar.addView(aboutButton)

        root.addView(scoreText)
        root.addView(gameView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(buttonBar)
        setContentView(root)
    }

    private fun updateScore(score: Int, phase: String) {
        runOnUiThread {
            scoreText.text = "Score: $score  |  $phase"
        }
    }

    private fun showAbout() {
        val builder = AlertDialog.Builder(this, R.style.AboutDialogTheme)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 32)
            setBackgroundColor(0xFF151528.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "Paper Plane Tycoon"
            setTextColor(0xFF00FF88.toInt())
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        })

        layout.addView(TextView(this).apply {
            text = "Made by jnetai.com"
            setTextColor(0xFF888899.toInt())
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })

        layout.addView(TextView(this).apply {
            text = "Version $CURRENT_VERSION"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 16f
            setPadding(0, 0, 0, 24)
        })

        val checkBtn = Button(this).apply {
            text = "Check for Update"
            setBackgroundColor(0xFF006644.toInt())
            setTextColor(0xFF00FF88.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            val btn = this
            setOnClickListener {
                btn.isEnabled = false
                btn.text = "Checking..."
                checkForUpdate { result ->
                    runOnUiThread {
                        btn.text = result
                        btn.isEnabled = true
                    }
                }
            }
        }
        layout.addView(checkBtn)

        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 24)
        })

        val shareBtn = Button(this).apply {
            text = "Share App"
            setBackgroundColor(0xFF234A6A.toInt())
            setTextColor(0xFF00CCFF.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Paper Plane Tycoon")
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
                }
                startActivity(Intent.createChooser(intent, "Share via"))
            }
        }
        layout.addView(shareBtn)

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }

        builder.setView(scrollView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkForUpdate(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name").removePrefix("v")

                if (latestTag != CURRENT_VERSION) {
                    callback("New version $latestTag available!")
                } else {
                    callback("You're up to date!")
                }
            } catch (e: Exception) {
                callback("Could not check updates: ${e.message}")
            }
        }
    }
}

class GameView(context: Context, private val scoreCallback: (Int, String) -> Unit) : View(context) {
    companion object {
        const val GRAVITY = 0.15f
        const val LIFT = 0.06f
        const val DRAG = 0.98f
        const val TAG = "GameView"
        const val GROUND_Y = 0.85f
        const val PLANE_RADIUS = 16f
    }

    private enum class Phase { FOLDING, FLYING, CRASHED }

    private var phase = Phase.FOLDING
    private var planeX = 0f
    private var planeY = 0f
    private var velocityX = 0f
    private var velocityY = 0f
    private var planeAngle = 0f
    private var score = 0
    private var distanceTraveled = 0f
    private var lastX = 0f
    private var trickCombo = 0
    private var trickText = ""
    private var trickTextTimer = 0
    private var gameOver = false

    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swipeActive = false
    private var launchAngle = 0f
    private var launchPower = 0f

    private val obstacles = mutableListOf<Obstacle>()
    private val trailPoints = mutableListOf<Pair<Float, Float>>()
    private val random = Random()
    private var scrollOffset = 0f
    private var lastScoredX = 0f

    private val bgPaint = Paint().apply { color = 0xFF0A0A1A.toInt(); style = Paint.Style.FILL }
    private val floorPaint = Paint().apply { color = 0xFF2A2A3A.toInt(); style = Paint.Style.FILL }
    private val carpetPaint = Paint().apply { color = 0xFF1A1520.toInt(); style = Paint.Style.FILL }
    private val deskPaint = Paint().apply { color = 0xFF3A3020.toInt(); style = Paint.Style.FILL }
    private val windowPaint = Paint().apply { color = 0xFF334466.toInt(); style = Paint.Style.FILL }
    private val windowGlowPaint = Paint().apply { color = 0x22336699.toInt(); style = Paint.Style.FILL }
    private val fanPaint = Paint().apply { color = 0xFF555555.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val fanBladePaint = Paint().apply { color = 0x66555555.toInt(); style = Paint.Style.FILL }
    private val planePaint = Paint().apply { color = 0xFFFF9933.toInt(); style = Paint.Style.FILL }
    private val planeWingPaint = Paint().apply { color = 0xFFFFCC66.toInt(); style = Paint.Style.FILL }
    private val trailPaint = Paint().apply { color = 0x44FF9933.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val aimLinePaint = Paint().apply { color = 0x88FF9933.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val aimDotPaint = Paint().apply { color = 0xFFFF9933.toInt(); style = Paint.Style.FILL }
    private val obstacleHitboxPaint = Paint().apply { color = 0xFFFF3344.toInt(); style = Paint.Style.FILL }

    init {
        planeX = 0.1f
        planeY = GROUND_Y
        generateObstacles()
        post(gameLoop)
    }

    private fun generateObstacles() {
        obstacles.clear()
        for (i in 0 until 8) {
            val ox = 0.3f + random.nextFloat() * 5.0f
            val t = random.nextInt(4)
            when (t) {
                0 -> obstacles.add(Obstacle(ox, 0.08f, 40f, 60f, "ceiling_fan"))
                1 -> obstacles.add(Obstacle(ox, 0.70f, 30f, 80f, "desk"))
                2 -> obstacles.add(Obstacle(ox, 0.55f, 60f, 12f, "desk_edge"))
                3 -> obstacles.add(Obstacle(ox, 0.12f, 50f, 70f, "window"))
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) restart()
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (phase == Phase.FOLDING) {
                    swipeStartX = event.x
                    swipeStartY = event.y
                    swipeActive = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (swipeActive && phase == Phase.FOLDING) {
                    val dx = event.x - swipeStartX
                    val dy = event.y - swipeStartY
                    launchPower = sqrt(dx * dx + dy * dy).coerceAtMost(300f) / 300f
                    launchAngle = atan2(dy.toDouble(), (-dx).toDouble()).toFloat()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (swipeActive && phase == Phase.FOLDING && launchPower > 0.1f) {
                    velocityX = cos(launchAngle) * launchPower * 18f
                    velocityY = sin(launchAngle) * launchPower * 18f
                    phase = Phase.FLYING
                    lastX = planeX
                    lastScoredX = planeX
                    score = 0
                    trailPoints.clear()
                }
                swipeActive = false
            }
        }
        return true
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (gameOver) return
            update()
            invalidate()
            postDelayed(this, 33)
        }
    }

    private fun update() {
        if (phase == Phase.FLYING) {
            velocityY += GRAVITY
            velocityY -= LIFT * (0.5f + planeAngle.coerceIn(-0.5f, 0.5f))
            velocityX *= DRAG
            velocityY *= DRAG

            val speed = sqrt(velocityX * velocityX + velocityY * velocityY)
            if (speed > 0.5f) {
                planeAngle = (planeAngle * 0.9f + velocityY / speed * 0.1f).coerceIn(-1f, 1f)
            }

            planeX += velocityX * 0.016f
            planeY += velocityY * 0.016f

            distanceTraveled += (planeX - lastX).absoluteValue
            lastX = planeX

            val dx = planeX - lastScoredX
            if (dx > 0.2f) {
                score += (dx * 100).toInt()
                lastScoredX = planeX
            }

            val prevCombo = trickCombo
            if (velocityY < -4f && velocityX > 1f) trickCombo = maxOf(trickCombo, 1)
            if (speed > 6f && abs(planeAngle) > 0.6f) trickCombo = maxOf(trickCombo, 2)
            if (trickCombo > prevCombo) {
                val tricks = listOf("Loop!", "Barrel Roll!", "Nose Dive!", "Glide!")
                trickText = tricks[trickCombo - 1]
                trickTextTimer = 60
                score += trickCombo * 250
            }

            if (trickCombo >= 1 && speed < 3f && abs(planeAngle) < 0.2f) trickCombo = 0
            if (trickTextTimer > 0) trickTextTimer--

            if (planeY > GROUND_Y) {
                phase = Phase.CRASHED
                gameOver = true
                scoreCallback(score, "Crashed!")
                return
            }
            if (planeY < -1.5f) {
                phase = Phase.CRASHED
                gameOver = true
                scoreCallback(score, "Too high!")
                return
            }

            val vw = width.toFloat()
            val vh = height.toFloat()
            for (obs in obstacles) {
                val ox = (obs.x - scrollOffset) * vw
                val oy = obs.y * vh
                val ow = obs.w
                val oh = obs.h
                val px = planeX * vw - scrollOffset * vw
                val py = planeY * vh

                if (obs.type == "ceiling_fan") {
                    val fc = ox + ow / 2
                    val fcy = oy + oh / 2
                    val tipAngle = System.currentTimeMillis() % 3000 / 3000f * PI.toFloat() * 2f

                    for (b in 0 until 3) {
                        val ba = tipAngle + b * 2f * PI.toFloat() / 3f
                        val bx = fc + cos(ba) * ow * 0.35f
                        val by = fcy + sin(ba) * ow * 0.35f
                        if (abs(px - bx) < ow * 0.15f && abs(py - by) < ow * 0.15f) {
                            phase = Phase.CRASHED
                            gameOver = true
                            scoreCallback(score, "Hit the fan!")
                            return
                        }
                    }
                } else {
                    if (px > ox - ow / 2 && px < ox + ow / 2 && py > oy - oh / 2 && py < oy + oh / 2) {
                        if (obs.type == "window") {
                            score += 500
                            trickText = "Window Pass!"
                            trickTextTimer = 60
                        } else {
                            phase = Phase.CRASHED
                            gameOver = true
                            scoreCallback(score, "Hit obstacle!")
                            return
                        }
                    }
                }
            }

            trailPoints.add(planeX to planeY)
            if (trailPoints.size > 60) trailPoints.removeAt(0)

            scrollOffset += velocityX * 0.008f
            val phaseStr = if (trickTextTimer > 0) trickText else "Flying"
            scoreCallback(score, phaseStr)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val vw = width.toFloat()
        val vh = height.toFloat()

        canvas.drawRect(0f, 0f, vw, vh, bgPaint)

        val carpetRect = RectF(0f, vh * 0.82f, vw, vh.toFloat())
        canvas.drawRect(carpetRect, carpetPaint)

        val floorY = GROUND_Y * vh
        canvas.drawRect(0f, floorY - 4f, vw, floorY + 4f, floorPaint)

        for (obs in obstacles) {
            val ox = (obs.x - scrollOffset) * vw
            val oy = obs.y * vh

            when (obs.type) {
                "ceiling_fan" -> {
                    val cx = ox + obs.w / 2
                    val cy = oy
                    canvas.drawCircle(cx, cy, obs.w / 2, fanPaint)
                    val tipAngle = System.currentTimeMillis() % 3000 / 3000f * PI.toFloat() * 2f
                    for (b in 0 until 3) {
                        val ba = tipAngle + b * 2f * PI.toFloat() / 3f
                        val bx = cx + cos(ba) * obs.w * 0.35f
                        val by = cy + sin(ba) * obs.w * 0.35f
                        canvas.drawCircle(bx, by, obs.w * 0.1f, fanBladePaint)
                    }
                }
                "desk" -> {
                    canvas.drawRect(ox - obs.w / 2, oy - obs.h / 2, ox + obs.w / 2, oy + obs.h / 2, deskPaint)
                }
                "desk_edge" -> {
                    canvas.drawRect(ox - obs.w / 2, oy, ox + obs.w / 2, oy + 6f, deskPaint)
                }
                "window" -> {
                    val wr = RectF(ox - obs.w / 2, oy - obs.h / 2, ox + obs.w / 2, oy + obs.h / 2)
                    canvas.drawRect(wr, windowPaint)
                    canvas.drawRect(wr, windowGlowPaint)
                }
            }
        }

        val scrollPx = scrollOffset * vw

        for (tp in trailPoints) {
            val tx = tp.first * vw - scrollPx
            val ty = tp.second * vh
            canvas.drawCircle(tx, ty, 3f, trailPaint)
        }

        val px = planeX * vw - scrollPx
        val py = planeY * vh

        if (phase == Phase.FLYING || phase == Phase.CRASHED) {
            canvas.save()
            canvas.rotate(Math.toDegrees(planeAngle.toDouble()).toFloat(), px, py)

            canvas.drawCircle(px - PLANE_RADIUS * 0.8f, py, PLANE_RADIUS * 0.35f, planePaint)
            canvas.drawRoundRect(
                px - PLANE_RADIUS * 0.3f, py - PLANE_RADIUS * 0.15f,
                px + PLANE_RADIUS * 0.8f, py + PLANE_RADIUS * 0.15f,
                4f, 4f, planePaint
            )

            val wingPath = Path().apply {
                moveTo(px - PLANE_RADIUS * 0.2f, py + PLANE_RADIUS * 0.15f)
                lineTo(px - PLANE_RADIUS * 0.3f, py + PLANE_RADIUS * 0.5f)
                lineTo(px + PLANE_RADIUS * 0.0f, py + PLANE_RADIUS * 0.3f)
                close()
            }
            canvas.drawPath(wingPath, planeWingPaint)

            val wingPath2 = Path().apply {
                moveTo(px - PLANE_RADIUS * 0.2f, py - PLANE_RADIUS * 0.15f)
                lineTo(px - PLANE_RADIUS * 0.3f, py - PLANE_RADIUS * 0.5f)
                lineTo(px + PLANE_RADIUS * 0.0f, py - PLANE_RADIUS * 0.3f)
                close()
            }
            canvas.drawPath(wingPath2, planeWingPaint)

            canvas.restore()
        } else {
            canvas.drawCircle(px, py, PLANE_RADIUS * 0.3f, planePaint)
            canvas.drawRoundRect(
                px - PLANE_RADIUS * 0.3f, py - PLANE_RADIUS * 0.12f,
                px + PLANE_RADIUS * 0.7f, py + PLANE_RADIUS * 0.12f,
                3f, 3f, planePaint
            )
        }

        if (swipeActive && phase == Phase.FOLDING) {
            val lx = px + cos(launchAngle) * launchPower * 200f
            val ly = py + sin(launchAngle) * launchPower * 200f
            canvas.drawLine(px, py, lx, ly, aimLinePaint)
            canvas.drawCircle(lx, ly, 8f, aimDotPaint)
        }

        if (gameOver) {
            val overlay = Paint().apply { color = 0xBB000000.toInt(); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, vw, vh, overlay)
            val textPaint = Paint().apply {
                color = 0xFFFF3344.toInt()
                textSize = 48f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Game Over", vw / 2f, vh / 2f - 24, textPaint)
            textPaint.color = 0xFFFF9933.toInt()
            textPaint.textSize = 32f
            canvas.drawText("Score: $score", vw / 2f, vh / 2f + 32, textPaint)
            textPaint.textSize = 22f
            textPaint.color = 0xFFCCCCCC.toInt()
            canvas.drawText("Tap to Restart", vw / 2f, vh / 2f + 72, textPaint)
        }
    }

    fun restart() {
        phase = Phase.FOLDING
        planeX = 0.1f
        planeY = GROUND_Y
        velocityX = 0f
        velocityY = 0f
        planeAngle = 0f
        score = 0
        distanceTraveled = 0f
        lastX = 0.1f
        lastScoredX = 0.1f
        scrollOffset = 0f
        trickCombo = 0
        trickText = ""
        trickTextTimer = 0
        swipeActive = false
        launchAngle = 0f
        launchPower = 0f
        gameOver = false
        trailPoints.clear()
        scoreCallback(0, "Ready")
        invalidate()
    }
}

data class Obstacle(val x: Float, val y: Float, val w: Float, val h: Float, val type: String)
