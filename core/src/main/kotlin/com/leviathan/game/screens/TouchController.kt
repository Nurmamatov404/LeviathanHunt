package com.leviathan.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.leviathan.game.network.MonsterAbility
import com.leviathan.game.network.PlayerRole

class TouchController(private val role: PlayerRole) : InputAdapter() {
    data class ButtonDef(
        val id: String,
        val x: Float, val y: Float,
        val radius: Float,
        val label: String,
        var isPressed: Boolean = false,
        var justPressed: Boolean = false
    )

    private val joystickCenter = Vector2(150f, 150f)
    private val joystickRadius = 100f
    private val joystickKnobRadius = 30f
    private val joystickBgRadius = 80f
    private var joystickActive = false
    private var joystickTouchId = -1
    private val joystickOutput = Vector2()
    private val touchPos = Vector3()

    private var joystickKnobPos = Vector2(joystickCenter)

    private val screenW: Float get() = Gdx.graphics.width.toFloat()
    private val screenH: Float get() = Gdx.graphics.height.toFloat()

    val buttons = mutableListOf<ButtonDef>()
    private var verticalDelta = 0f
    private var lastVerticalTouch = -1f
    private var verticalTouchId = -1

    var onAction: ((String) -> Unit)? = null
    var moveX: Float = 0f
    var moveY: Float = 0f
    var moveZ: Float = 0f

    init {
        layoutButtons()
    }

    private fun layoutButtons() {
        buttons.clear()
        val bx = screenW - 140f
        val by = 120f
        val gap = 95f
        val r = 35f

        if (role == PlayerRole.SHIP) {
            buttons.add(ButtonDef("cannon", bx, by + gap * 3, r, "FIRE"))
            buttons.add(ButtonDef("harpoon", bx, by + gap * 2, r, "HARP"))
            buttons.add(ButtonDef("turbo", bx + gap, by + gap * 1, r, "TURBO"))
            buttons.add(ButtonDef("repair", bx, by + gap * 0, r, "REP"))
        } else {
            buttons.add(ButtonDef("tentacle", bx, by + gap * 5, r, "TNT"))
            buttons.add(ButtonDef("dash", bx, by + gap * 4, r, "DASH"))
            buttons.add(ButtonDef("emp", bx, by + gap * 3, r, "EMP"))
            buttons.add(ButtonDef("whirlpool", bx + gap, by + gap * 2, r, "WRL"))
            buttons.add(ButtonDef("deepdive", bx + gap, by + gap * 1, r, "DIVE"))
            buttons.add(ButtonDef("jammer", bx + gap, by + gap * 0, r, "JAM"))
        }

        buttons.forEachIndexed { i, b ->
            buttons[i] = b.copy(x = screenW - 140f + (if (i >= 4) gap else 0f))
        }
    }

    fun update() {
        buttons.forEach { it.justPressed = false }
        moveX = joystickOutput.x
        moveY = joystickOutput.y

        val monoH = Gdx.graphics.height.toFloat()
        if (verticalTouchId >= 0) {
            val vy = touchPos.y
            val delta = (vy - lastVerticalTouch) / 10f
            moveZ = delta.coerceIn(-1f, 1f)
            lastVerticalTouch = vy
        } else {
            moveZ *= 0.9f
        }
    }

    override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        val xx = x.toFloat()
        val yy = Gdx.graphics.height - y.toFloat()
        touchPos.set(xx, yy, 0f)

        // Check buttons first
        buttons.forEachIndexed { i, btn ->
            val dx = xx - btn.x
            val dy = yy - btn.y
            if (dx * dx + dy * dy <= btn.radius * btn.radius) {
                buttons[i] = btn.copy(isPressed = true, justPressed = true)
                onAction?.invoke(btn.id)
                return true
            }
        }

        // Joystick (left side)
        if (xx < Gdx.graphics.width / 2f) {
            if (Vector2(xx, yy).dst(joystickCenter) <= joystickRadius * 1.5f) {
                joystickActive = true
                joystickTouchId = pointer
                updateJoystick(xx, yy)
                return true
            }
        }

        // Vertical movement zone (right center, for monster)
        if (role == PlayerRole.MONSTER && xx > Gdx.graphics.width / 2f) {
            verticalTouchId = pointer
            lastVerticalTouch = yy
            moveZ = 0f
            return true
        }

        return false
    }

    override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean {
        val xx = x.toFloat()
        val yy = Gdx.graphics.height - y.toFloat()
        touchPos.set(xx, yy, 0f)

        if (pointer == joystickTouchId && joystickActive) {
            updateJoystick(xx, yy)
            return true
        }

        if (pointer == verticalTouchId) {
            val delta = (yy - lastVerticalTouch) / 10f
            moveZ = delta.coerceIn(-1f, 1f)
            lastVerticalTouch = yy
            return true
        }

        // Drag over buttons
        buttons.forEachIndexed { i, btn ->
            val dx = xx - btn.x
            val dy = yy - btn.y
            val inside = dx * dx + dy * dy <= btn.radius * btn.radius
            if (inside != btn.isPressed) {
                buttons[i] = btn.copy(isPressed = inside)
            }
        }

        return false
    }

    override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        if (pointer == joystickTouchId) {
            joystickActive = false
            joystickTouchId = -1
            joystickKnobPos.set(joystickCenter)
            joystickOutput.set(0f, 0f)
            moveX = 0f
            moveY = 0f
            return true
        }

        if (pointer == verticalTouchId) {
            verticalTouchId = -1
            moveZ = 0f
            return true
        }

        buttons.forEachIndexed { i, btn ->
            if (btn.isPressed) {
                buttons[i] = btn.copy(isPressed = false)
            }
        }

        return false
    }

    private fun updateJoystick(xx: Float, yy: Float) {
        val diff = Vector2(xx, yy).sub(joystickCenter)
        val dist = diff.len().coerceAtMost(joystickBgRadius)
        val norm = diff.nor()
        joystickKnobPos.set(joystickCenter).add(norm.scl(dist))
        joystickOutput.set(diff.x / joystickBgRadius, diff.y / joystickBgRadius)
        joystickOutput.limit(1f)
    }

    fun render(shape: ShapeRenderer, batch: SpriteBatch, font: BitmapFont) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Joystick
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.setColor(1f, 1f, 1f, 0.2f)
        shape.circle(joystickCenter.x, joystickCenter.y, joystickRadius)
        shape.setColor(1f, 1f, 1f, 0.4f)
        shape.circle(joystickCenter.x, joystickCenter.y, joystickBgRadius)
        shape.setColor(1f, 1f, 1f, 0.6f)
        shape.circle(joystickKnobPos.x, joystickKnobPos.y, joystickKnobRadius)
        shape.end()

        // Vertical indicator for monster
        if (role == PlayerRole.MONSTER) {
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.setColor(1f, 1f, 1f, 0.15f)
            val vx = Gdx.graphics.width / 2f + 20f
            shape.rect(vx, 50f, 30f, Gdx.graphics.height - 100f)
            val vy = touchPos.y
            if (verticalTouchId >= 0) {
                shape.setColor(0.3f, 0.7f, 1f, 0.5f)
                shape.rect(vx, vy - 20f, 30f, 40f)
            }
            shape.end()
        }

        // Buttons
        shape.begin(ShapeRenderer.ShapeType.Filled)
        buttons.forEach { btn ->
            if (btn.isPressed) {
                shape.setColor(0.3f, 0.8f, 1f, 0.9f)
            } else {
                shape.setColor(1f, 1f, 1f, 0.25f)
            }
            shape.circle(btn.x, btn.y, btn.radius)
            shape.setColor(1f, 1f, 1f, 0.08f)
            shape.circle(btn.x, btn.y, btn.radius + 3f)
        }
        shape.end()

        // Button labels
        batch.begin()
        font.data.setScale(0.6f)
        buttons.forEach { btn ->
            val labelW = font.draw(batch, btn.label, btn.x - 15f, btn.y + 5f).width
        }
        batch.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    fun resize(width: Int, height: Int) {
        layoutButtons()
    }
}
