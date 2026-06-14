package com.leviathan.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.utils.AnimationController
import com.badlogic.gdx.math.Vector3
import com.leviathan.game.network.*
import com.leviathan.game.screens.*
import com.leviathan.game.utils.Constants

class LeviathanGame : ApplicationAdapter() {
    lateinit var camera: PerspectiveCamera
    lateinit var modelBatch: ModelBatch
    lateinit var environment: Environment
    lateinit var modelBuilder: ModelBuilder

    var currentScreen: GameScreen? = null
    val networkManager = NetworkManager()

    var playerRole = PlayerRole.NONE
    var gameState = GameState.LOBBY

    override fun create() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 30f, 30f)
        camera.lookAt(0f, 0f, 0f)
        camera.near = 1f
        camera.far = 500f
        camera.update()

        modelBatch = ModelBatch()

        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(com.badlogic.gdx.graphics.g3d.environment.DirectionalLight().let {
            it.set(0.8f, 0.8f, 0.8f, -0.5f, -0.5f, -0.5f)
            it
        })

        modelBuilder = ModelBuilder()

        networkManager.onConnected = {
            Gdx.app.postRunnable {
                if (networkManager.isHosting()) {
                    setScreen(LobbyScreen(this))
                }
            }
        }
        networkManager.onPacketReceived = { packet ->
            Gdx.app.postRunnable {
                currentScreen?.onNetworkPacket(packet)
            }
        }
        networkManager.onDisconnected = { reason ->
            Gdx.app.postRunnable {
                setScreen(MenuScreen(this))
            }
        }

        setScreen(MenuScreen(this))
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        currentScreen?.render(Gdx.graphics.deltaTime)
    }

    override fun resize(width: Int, height: Int) {
        currentScreen?.resize(width, height)
    }

    override fun dispose() {
        currentScreen?.dispose()
        modelBatch.dispose()
        networkManager.disconnect()
    }

    fun setScreen(screen: GameScreen) {
        currentScreen?.dispose()
        currentScreen = screen
        screen.show()
    }
}
