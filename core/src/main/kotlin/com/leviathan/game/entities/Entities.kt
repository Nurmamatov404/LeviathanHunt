package com.leviathan.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Quaternion
import com.leviathan.game.utils.Constants

class ShipEntity(val modelBuilder: ModelBuilder) {
    var position = Vector3(0f, 0.5f, 0f)
    var rotation = 0f
    var hp = Constants.SHIP_MAX_HP
    var maxHp = Constants.SHIP_MAX_HP
    var speed = Constants.SHIP_SPEED
    var engineHealth = 100f

    var currentAmmo = Constants.MAX_AMMO
    var currentHarpoons = Constants.MAX_HARPOONS
    var repairsLeft = Constants.MAX_REPAIRS
    var turboCooldown = 0f
    var isTurboActive = false
    var isDisabled = false
    var isOperational = true

    lateinit var model: Model
    lateinit var instance: ModelInstance
    private val shipParts = mutableListOf<ModelInstance>()

    fun build() {
        val hull = modelBuilder.createBox(6f, 2f, 3f,
            Material(ColorAttribute.createDiffuse(Color.BROWN)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        val cabin = modelBuilder.createBox(2f, 1.5f, 2f,
            Material(ColorAttribute.createDiffuse(Color.GRAY)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        val deck = modelBuilder.createBox(5f, 0.3f, 2.5f,
            Material(ColorAttribute.createDiffuse(Color.valueOf("8B4513"))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        val cannon = modelBuilder.createCylinder(1.2f, 0.8f, 0.8f, 12,
            Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())

        instance = ModelInstance(hull)
        instance.transform.setToTranslation(position)

        val cabinInst = ModelInstance(cabin)
        cabinInst.transform.setToTranslation(0f, 1.5f, 0f)
        shipParts.add(cabinInst)

        val deckInst = ModelInstance(deck)
        deckInst.transform.setToTranslation(0f, 1.2f, 0f)
        shipParts.add(deckInst)

        val cannonInst = ModelInstance(cannon)
        cannonInst.transform.setToTranslation(2.5f, 0.5f, 0f)
        shipParts.add(cannonInst)

        val cannonInst2 = ModelInstance(cannon)
        cannonInst2.transform.setToTranslation(2.5f, 0.5f, 0f)
        shipParts.add(cannonInst2)
    }

    fun getRenderables(provider: ModelBatch, env: Environment) {
        provider.render(instance, env)
        shipParts.forEach { part ->
            part.transform.set(instance.transform)
            provider.render(part, env)
        }
    }

    fun applyDamage(dmg: Float) {
        hp = (hp - dmg).coerceAtLeast(0f)
    }

    fun isAlive() = hp > 0f

    fun getHpPercent() = hp / maxHp
}

class MonsterEntity(val modelBuilder: ModelBuilder) {
    var position = Vector3(50f, -3f, 50f)
    var rotation = 0f
    var hp = Constants.MONSTER_MAX_HP
    var maxHp = Constants.MONSTER_MAX_HP
    var energy = Constants.MONSTER_MAX_ENERGY
    var maxEnergy = Constants.MONSTER_MAX_ENERGY
    var speed = Constants.MONSTER_SPEED
    var isDisabled = false

    lateinit var model: Model
    lateinit var instance: ModelInstance
    private val bodyParts = mutableListOf<ModelInstance>()

    fun build() {
        val body = modelBuilder.createSphere(5f, 3f, 4f, 16, 16,
            Material(ColorAttribute.createDiffuse(Color.GREEN)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        val head = modelBuilder.createSphere(2.5f, 2f, 2f, 12, 12,
            Material(ColorAttribute.createDiffuse(Color.valueOf("006400"))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        val eye = modelBuilder.createSphere(0.5f, 0.5f, 0.5f, 8, 8,
            Material(ColorAttribute.createDiffuse(Color.RED)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        val tentacle = modelBuilder.createCylinder(1f, 3f, 1f, 8,
            Material(ColorAttribute.createDiffuse(Color.valueOf("006400"))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())

        instance = ModelInstance(body)
        instance.transform.setToTranslation(position)

        val headInst = ModelInstance(head)
        headInst.transform.setToTranslation(0f, 2f, 2f)
        bodyParts.add(headInst)

        for (i in 0 until 6) {
            val angle = i * 60f
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val t = ModelInstance(tentacle)
            t.transform.setToTranslation(
                kotlin.math.cos(rad) * 2.5f,
                -1f,
                kotlin.math.sin(rad) * 2.5f
            )
            bodyParts.add(t)
        }

        val eyeL = ModelInstance(eye)
        eyeL.transform.setToTranslation(-0.8f, 2.3f, 3f)
        bodyParts.add(eyeL)
        val eyeR = ModelInstance(eye)
        eyeR.transform.setToTranslation(0.8f, 2.3f, 3f)
        bodyParts.add(eyeR)
    }

    fun getRenderables(provider: ModelBatch, env: Environment) {
        provider.render(instance, env)
        bodyParts.forEach { part ->
            part.transform.set(instance.transform)
            provider.render(part, env)
        }
    }

    fun applyDamage(dmg: Float) {
        hp = (hp - dmg).coerceAtLeast(0f)
    }

    fun spendEnergy(amount: Float): Boolean {
        if (energy >= amount) {
            energy -= amount
            return true
        }
        return false
    }

    fun regenEnergy(delta: Float) {
        energy = (energy + Constants.ENERGY_REGEN_RATE * delta).coerceAtMost(maxEnergy)
    }

    fun isAlive() = hp > 0f
    fun getHpPercent() = hp / maxHp
    fun getEnergyPercent() = energy / maxEnergy
}
