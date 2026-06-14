package com.leviathan.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.leviathan.game.utils.Constants

class WorldObjects(private val modelBuilder: ModelBuilder) {
    private val objects = mutableListOf<ModelInstance>()
    private val waterModel: Model
    val waterInstance: ModelInstance

    init {
        waterModel = modelBuilder.createRect(
            -Constants.MAP_SIZE_X / 2f, Constants.WATER_LEVEL, -Constants.MAP_SIZE_Z / 2f,
            Constants.MAP_SIZE_X / 2f, Constants.WATER_LEVEL, -Constants.MAP_SIZE_Z / 2f,
            Constants.MAP_SIZE_X / 2f, Constants.WATER_LEVEL, Constants.MAP_SIZE_Z / 2f,
            -Constants.MAP_SIZE_X / 2f, Constants.WATER_LEVEL, Constants.MAP_SIZE_Z / 2f,
            0f, 1f, 0f,
            Material(ColorAttribute.createDiffuse(Color.valueOf("1a5276"))),
            VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal
        )
        waterInstance = ModelInstance(waterModel)
        waterInstance.transform.setToTranslation(0f, Constants.WATER_LEVEL, 0f)

        createIslands()
        createReefs()
    }

    private fun createIslands() {
        val islandMat = Material(ColorAttribute.createDiffuse(Color.valueOf("8B4513")))
        val grassMat = Material(ColorAttribute.createDiffuse(Color.valueOf("228B22")))

        val islandPositions = listOf(
            Vector3(-150f, Constants.WATER_LEVEL, 120f),
            Vector3(180f, Constants.WATER_LEVEL, -100f),
            Vector3(-100f, Constants.WATER_LEVEL, -180f),
            Vector3(200f, Constants.WATER_LEVEL, 160f),
        )

        for (pos in islandPositions) {
            val base = modelBuilder.createBox(30f, 5f, 30f, islandMat,
                VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal)
            val top = modelBuilder.createBox(25f, 3f, 25f, grassMat,
                VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal)

            val baseInst = ModelInstance(base)
            baseInst.transform.setToTranslation(pos.x, Constants.WATER_LEVEL - 2f, pos.z)
            objects.add(baseInst)

            val topInst = ModelInstance(top)
            topInst.transform.setToTranslation(pos.x, Constants.WATER_LEVEL + 2f, pos.z)
            objects.add(topInst)
        }
    }

    private fun createReefs() {
        val reefMat = Material(ColorAttribute.createDiffuse(Color.valueOf("4a4a4a")))

        val reefPositions = listOf(
            Vector3(-50f, Constants.WATER_LEVEL - 1f, 50f),
            Vector3(80f, Constants.WATER_LEVEL - 1f, -60f),
            Vector3(-30f, Constants.WATER_LEVEL - 2f, -80f),
            Vector3(60f, Constants.WATER_LEVEL - 1f, 90f),
            Vector3(-120f, Constants.WATER_LEVEL - 2f, -40f),
            Vector3(140f, Constants.WATER_LEVEL - 1f, 30f),
        )

        for (pos in reefPositions) {
            val reef = modelBuilder.createBox(8f, 3f, 8f, reefMat,
                VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal)
            val inst = ModelInstance(reef)
            inst.transform.setToTranslation(pos.x, pos.y, pos.z)
            objects.add(inst)
        }
    }

    fun render(provider: ModelBatch, env: Environment) {
        provider.render(waterInstance, env)
        objects.forEach { provider.render(it, env) }
    }

    fun dispose() {
        waterModel.dispose()
    }
}
