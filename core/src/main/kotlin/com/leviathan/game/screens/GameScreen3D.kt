package com.leviathan.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.leviathan.game.LeviathanGame
import com.leviathan.game.entities.*
import com.leviathan.game.network.*
import com.leviathan.game.utils.Constants
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameScreen3D(private val game: LeviathanGame) : GameScreen {
    private lateinit var modelBatch: ModelBatch
    private lateinit var modelBuilder: ModelBuilder
    private lateinit var world: WorldObjects
    private lateinit var hudBatch: SpriteBatch
    private lateinit var hudFont: BitmapFont
    private lateinit var hudShape: ShapeRenderer

    private lateinit var ship: ShipEntity
    private lateinit var monster: MonsterEntity

    private var moveX = 0f
    private var moveY = 0f
    private var moveZ = 0f
    private var timeRemaining = Constants.MATCH_DURATION
    private var lastStateSend = 0f
    private var lastEnergyRegen = 0f
    private var waveTime = 0f

    private val cannonballs = mutableListOf<CannonballProjectile>()
    private val harpoons = mutableListOf<HarpoonProjectileEntity>()
    private var lastCannonFire = 0f
    private var lastHarpoonFire = 0f
    private var isReloading = false
    private var reloadTimer = 0f

    override fun show() {
        modelBatch = game.modelBatch
        modelBuilder = ModelBuilder()
        world = WorldObjects(modelBuilder)

        ship = ShipEntity(modelBuilder).also { it.build() }
        monster = MonsterEntity(modelBuilder).also { it.build() }

        hudBatch = SpriteBatch()
        hudFont = BitmapFont()
        hudFont.data.setScale(1.2f)
        hudShape = ShapeRenderer()

        if (game.networkManager.isHosting()) {
            ship.position.set(0f, 0.5f, 0f)
            monster.position.set(80f, -3f, 80f)
        }
    }

    override fun render(delta: Float) {
        handleInput()
        update(delta)
        render3D()
        renderHUD()
    }

    private fun handleInput() {
        moveX = 0f; moveY = 0f; moveZ = 0f

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveY = 1f
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveY = -1f
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX = -1f
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX = 1f

        if (game.playerRole == PlayerRole.MONSTER) {
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) moveZ = 1f
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) moveZ = -1f
        }

        if (game.playerRole == PlayerRole.SHIP) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) tryFireCannon()
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) tryFireHarpoon()
            if (Gdx.input.isKeyJustPressed(Input.Keys.T)) activateTurbo()
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) repairShip()
        }

        if (game.playerRole == PlayerRole.MONSTER) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) activateAbility(MonsterAbility.TENTACLE)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) activateAbility(MonsterAbility.DASH)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) activateAbility(MonsterAbility.EMP)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) activateAbility(MonsterAbility.WHIRLPOOL)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) activateAbility(MonsterAbility.DEEP_DIVE)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) activateAbility(MonsterAbility.SONAR_JAMMER)
        }

        sendInput()
    }

    private fun sendInput() {
        if (game.playerRole == PlayerRole.SHIP) {
            game.networkManager.sendPacket(ShipInputPacket().also {
                it.moveX = moveX; it.moveY = moveY
            })
        } else if (game.playerRole == PlayerRole.MONSTER) {
            game.networkManager.sendPacket(MonsterInputPacket().also {
                it.moveX = moveX; it.moveY = moveY; it.moveZ = moveZ
            })
        }
    }

    private fun update(delta: Float) {
        waveTime += delta

        if (game.playerRole == PlayerRole.SHIP) {
            updateShip(delta)
            sendEntityState("ship", ship.position, ship.rotation, ship.hp)
        } else if (game.playerRole == PlayerRole.MONSTER) {
            updateMonster(delta)
            sendEntityState("monster", monster.position, monster.rotation, monster.hp)
        }

        updateCannonballs(delta)
        updateHarpoons(delta)

        if (game.networkManager.isHosting()) {
            timeRemaining -= delta
            if (timeRemaining <= 0f) {
                game.networkManager.sendPacket(GameOverPacket().also {
                    it.winner = PlayerRole.SHIP; it.reason = "Time up"
                })
            }
            game.networkManager.sendPacket(GameStatePacket().also {
                it.state = GameState.PLAYING; it.timeRemaining = timeRemaining
                it.shipHp = ship.hp; it.monsterHp = monster.hp; it.monsterEnergy = monster.energy
            })
        }

        if (game.playerRole == PlayerRole.MONSTER) {
            monster.regenEnergy(delta)
        }

        constrainEntities()
    }

    private fun updateShip(delta: Float) {
        if (!ship.isOperational) return
        val speed = (ship.speed * (if (ship.isTurboActive) Constants.TURBO_MULTIPLIER else 1f))
        val rotSpeed = Constants.SHIP_ROTATION_SPEED

        ship.rotation += moveX * rotSpeed * delta
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        ship.position.x += cos(rad) * moveY * speed * delta
        ship.position.z += sin(rad) * moveY * speed * delta

        if (ship.isTurboActive) {
            ship.turboCooldown -= delta
            if (ship.turboCooldown <= 0f) ship.isTurboActive = false
        }
    }

    private fun updateMonster(delta: Float) {
        val speed = monster.speed
        val rotSpeed = Constants.MONSTER_ROTATION_SPEED

        if (moveX != 0f || moveY != 0f) {
            val targetRot = Math.toDegrees(kotlin.math.atan2(moveX.toDouble(), moveY.toDouble())).toFloat()
            monster.rotation += (targetRot - monster.rotation) * delta * 3f
        }
        val rad = Math.toRadians(monster.rotation.toDouble()).toFloat()
        monster.position.x += cos(rad) * moveY * speed * delta
        monster.position.z += sin(rad) * moveY * speed * delta
        monster.position.y += moveZ * 10f * delta
    }

    private fun tryFireCannon() {
        if (ship.currentAmmo <= 0) {
            if (!isReloading) { isReloading = true; reloadTimer = Constants.CANNON_RELOAD_TIME }
            return
        }
        val now = System.currentTimeMillis() / 1000f
        if (now - lastCannonFire < Constants.CANNON_FIRE_RATE) return

        lastCannonFire = now
        ship.currentAmmo--
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        val dir = Vector3(cos(rad), 0f, sin(rad))
        val start = ship.position.cpy().add(dir.x * 3f, 1f, dir.z * 3f)

        cannonballs.add(CannonballProjectile(start, dir))
        game.networkManager.sendPacket(CannonFirePacket().also {
            it.posX = start.x; it.posY = start.y; it.posZ = start.z
            it.dirX = dir.x; it.dirY = dir.y; it.dirZ = dir.z
        })
    }

    private fun tryFireHarpoon() {
        if (ship.currentHarpoons <= 0) return
        val now = System.currentTimeMillis() / 1000f
        if (now - lastHarpoonFire < 2f) return

        lastHarpoonFire = now
        ship.currentHarpoons--
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        val dir = Vector3(cos(rad), 0f, sin(rad))
        val start = ship.position.cpy().add(dir.x * 3f, 1f, dir.z * 3f)

        harpoons.add(HarpoonProjectileEntity(start, dir))
        game.networkManager.sendPacket(HarpoonFirePacket().also {
            it.posX = start.x; it.posY = start.y; it.posZ = start.z
            it.dirX = dir.x; it.dirY = dir.y; it.dirZ = dir.z
        })
    }

    private fun activateTurbo() {
        if (ship.turboCooldown > 0f || ship.isTurboActive) return
        ship.isTurboActive = true
        ship.turboCooldown = Constants.TURBO_DURATION
    }

    private fun repairShip() {
        if (ship.repairsLeft <= 0) return
        ship.repairsLeft--
        ship.hp = (ship.hp + Constants.REPAIR_AMOUNT * 2f).coerceAtMost(ship.maxHp)
        ship.engineHealth = (ship.engineHealth + Constants.REPAIR_AMOUNT).coerceAtMost(100f)
    }

    private fun activateAbility(ability: MonsterAbility) {
        when (ability) {
            MonsterAbility.TENTACLE -> {
                if (!monster.spendEnergy(Constants.TENTACLE_ENERGY)) return
                val dist = ship.position.cpy().sub(monster.position).len()
                if (dist <= Constants.TENTACLE_RANGE) {
                    ship.applyDamage(Constants.TENTACLE_DAMAGE)
                    ship.isDisabled = true
                }
            }
            MonsterAbility.DASH -> {
                if (!monster.spendEnergy(Constants.DASH_ENERGY)) return
                val dir = ship.position.cpy().sub(monster.position).nor()
                monster.position.add(dir.x * Constants.DASH_RANGE, 0f, dir.z * Constants.DASH_RANGE)
                val dist = monster.position.dst(ship.position)
                if (dist < 8f) ship.applyDamage(Constants.DASH_DAMAGE)
            }
            MonsterAbility.EMP -> {
                if (!monster.spendEnergy(Constants.EMP_ENERGY)) return
                val dist = monster.position.dst(ship.position)
                if (dist <= Constants.EMP_RADIUS) {
                    ship.applyDamage(Constants.EMP_DAMAGE)
                    ship.isDisabled = true
                    ship.isOperational = false
                }
            }
            MonsterAbility.WHIRLPOOL -> {
                if (!monster.spendEnergy(Constants.WHIRLPOOL_ENERGY)) return
                val dist = monster.position.dst(ship.position)
                if (dist <= Constants.WHIRLPOOL_RADIUS) {
                    val pullDir = monster.position.cpy().sub(ship.position).nor()
                    ship.position.add(pullDir.x * 10f, 0f, pullDir.z * 10f)
                    ship.applyDamage(Constants.WHIRLPOOL_DPS)
                }
            }
            MonsterAbility.DEEP_DIVE -> {
                if (!monster.spendEnergy(Constants.DEEP_DIVE_ENERGY)) return
                monster.position.y = -Constants.MAP_DEPTH
                // emerge near ship
                monster.position.set(ship.position.x, 3f, ship.position.z)
                val dist = monster.position.dst(ship.position)
                if (dist < 12f) ship.applyDamage(Constants.DIVE_EMERGE_DAMAGE)
            }
            MonsterAbility.SONAR_JAMMER -> {
                if (!monster.spendEnergy(Constants.SONAR_JAMMER_ENERGY)) return
                // Sonar jammer - radar disabled on ship
            }
        }
        game.networkManager.sendPacket(AbilityActivatePacket().also {
            it.ability = ability
            it.targetX = ship.position.x; it.targetY = ship.position.y; it.targetZ = ship.position.z
        })
    }

    private fun updateCannonballs(delta: Float) {
        val iter = cannonballs.iterator()
        while (iter.hasNext()) {
            val cb = iter.next()
            cb.position.add(cb.direction.cpy().scl(Constants.CANNON_SPEED * delta))
            cb.life -= delta
            if (cb.life <= 0f || cb.position.dst(ship.position) > Constants.CANNON_RANGE) {
                iter.remove()
                continue
            }
            if (cb.position.dst(monster.position) < 4f) {
                monster.applyDamage(Constants.CANNON_DAMAGE)
                checkMonsterDeath()
                iter.remove()
            }
        }
    }

    private fun updateHarpoons(delta: Float) {
        val iter = harpoons.iterator()
        while (iter.hasNext()) {
            val h = iter.next()
            h.position.add(h.direction.cpy().scl(50f * delta))
            h.life -= delta
            if (h.life <= 0f || h.position.dst(ship.position) > Constants.HARPOON_RANGE) {
                iter.remove()
                continue
            }
            if (h.position.dst(monster.position) < 4f) {
                monster.applyDamage(Constants.HARPOON_DAMAGE)
                monster.isDisabled = true
                iter.remove()
            }
        }
    }

    private fun checkMonsterDeath() {
        if (!monster.isAlive() && game.networkManager.isHosting()) {
            game.networkManager.sendPacket(GameOverPacket().also {
                it.winner = PlayerRole.SHIP; it.reason = "Monster defeated"
            })
        }
    }

    private fun sendEntityState(id: String, pos: Vector3, rot: Float, hp: Float) {
        val now = System.currentTimeMillis() / 1000f
        if (now - lastStateSend < 0.1f) return
        lastStateSend = now
        game.networkManager.sendPacket(EntityStatePacket().also {
            it.entityId = id; it.posX = pos.x; it.posY = pos.y; it.posZ = pos.z
            it.rotY = rot; it.hp = hp
        })
    }

    private fun constrainEntities() {
        val halfX = Constants.MAP_SIZE_X / 2f
        val halfZ = Constants.MAP_SIZE_Z / 2f
        ship.position.x = ship.position.x.coerceIn(-halfX, halfX)
        ship.position.z = ship.position.z.coerceIn(-halfZ, halfZ)
        monster.position.x = monster.position.x.coerceIn(-halfX, halfX)
        monster.position.z = monster.position.z.coerceIn(-halfZ, halfZ)
        monster.position.y = monster.position.y.coerceIn(-Constants.MAP_DEPTH.toFloat(), 5f)
    }

    private fun render3D() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        game.camera.position.set(
            if (game.playerRole == PlayerRole.SHIP)
                ship.position.x + 0f else monster.position.x + 0f,
            25f,
            if (game.playerRole == PlayerRole.SHIP)
                ship.position.z + 20f else monster.position.z + 20f
        )
        game.camera.lookAt(
            if (game.playerRole == PlayerRole.SHIP) ship.position else monster.position
        )
        game.camera.update()

        ship.instance.transform.setToTranslation(ship.position.x, ship.position.y, ship.position.z)
        ship.instance.transform.rotate(Vector3.Y, ship.rotation)
        monster.instance.transform.setToTranslation(monster.position.x, monster.position.y, monster.position.z)
        monster.instance.transform.rotate(Vector3.Y, monster.rotation)

        modelBatch.begin(game.camera)
        world.render(modelBatch, game.environment)
        ship.getRenderables(modelBatch, game.environment)
        monster.getRenderables(modelBatch, game.environment)
        renderProjectiles()
        modelBatch.end()
    }

    private fun renderProjectiles() {
        cannonballs.forEach { cb ->
            // Simple sphere for cannonball - reuse model or just skip for now
        }
    }

    private fun renderHUD() {
        hudShape.projectionMatrix = game.camera.combined
        hudBatch.begin()
        hudFont.data.setScale(1.5f)

        val mins = (timeRemaining / 60f).toInt()
        val secs = (timeRemaining % 60f).toInt()
        hudFont.draw(hudBatch, String.format("%02d:%02d", mins, secs),
            Gdx.graphics.width / 2f - 30f, Gdx.graphics.height - 20f)

        if (game.playerRole == PlayerRole.SHIP) {
            hudFont.draw(hudBatch, "SHIP HP: ${ship.hp.toInt()}/${ship.maxHp.toInt()} | Ammo: ${ship.currentAmmo} | Harpoons: ${ship.currentHarpoons} | Repairs: ${ship.repairsLeft}",
                20f, Gdx.graphics.height - 60f)
            hudFont.draw(hudBatch, "[F]Fire [Q]Harpoon [T]Turbo [R]Repair  WASD:Move",
                20f, 40f)
        } else {
            hudFont.draw(hudBatch, "MONSTER HP: ${monster.hp.toInt()}/${monster.maxHp.toInt()} | Energy: ${monster.energy.toInt()}/${monster.maxEnergy.toInt()}",
                20f, Gdx.graphics.height - 60f)
            hudFont.draw(hudBatch, "[1]Tentacle [2]Dash [3]EMP [4]Whirlpool [5]Dive [6]Jammer",
                20f, 40f)
        }
        hudFont.draw(hudBatch, "Monster HP: ${monster.hp.toInt()}",
            Gdx.graphics.width - 200f, Gdx.graphics.height - 60f)

        hudBatch.end()
    }

    override fun onNetworkPacket(packet: Packet) {
        when (packet) {
            is EntityStatePacket -> {
                if (packet.entityId == "ship" && game.playerRole != PlayerRole.SHIP) {
                    ship.position.set(packet.posX, packet.posY, packet.posZ)
                    ship.rotation = packet.rotY; ship.hp = packet.hp
                }
                if (packet.entityId == "monster" && game.playerRole != PlayerRole.MONSTER) {
                    monster.position.set(packet.posX, packet.posY, packet.posZ)
                    monster.rotation = packet.rotY; monster.hp = packet.hp
                }
            }
            is GameStatePacket -> {
                timeRemaining = packet.timeRemaining
                ship.hp = packet.shipHp; monster.hp = packet.monsterHp; monster.energy = packet.monsterEnergy
            }
            is GameOverPacket -> {
                game.setScreen(GameOverScreen(game, packet.winner, packet.reason))
            }
            is CannonFirePacket -> {
                if (game.playerRole != PlayerRole.SHIP) {
                    cannonballs.add(CannonballProjectile(
                        Vector3(packet.posX, packet.posY, packet.posZ),
                        Vector3(packet.dirX, packet.dirY, packet.dirZ)
                    ))
                }
            }
            is AbilityActivatePacket -> {
                if (game.playerRole == PlayerRole.SHIP) {
                    when (packet.ability) {
                        MonsterAbility.TENTACLE -> {
                            ship.applyDamage(Constants.TENTACLE_DAMAGE)
                            ship.isDisabled = true
                        }
                        MonsterAbility.DASH -> ship.applyDamage(Constants.DASH_DAMAGE)
                        MonsterAbility.EMP -> { ship.applyDamage(Constants.EMP_DAMAGE); ship.isOperational = false }
                        MonsterAbility.WHIRLPOOL -> {
                            val pullDir = monster.position.cpy().sub(ship.position).nor()
                            ship.position.add(pullDir.x * 10f, 0f, pullDir.z * 10f)
                            ship.applyDamage(Constants.WHIRLPOOL_DPS)
                        }
                        MonsterAbility.DEEP_DIVE -> {
                            monster.position.set(packet.targetX, 3f, packet.targetZ)
                            if (monster.position.dst(ship.position) < 12f)
                                ship.applyDamage(Constants.DIVE_EMERGE_DAMAGE)
                        }
                        else -> {}
                    }
                }
            }
            else -> {}
        }
    }

    override fun getGame() = game
    override fun resize(w: Int, h: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        hudBatch.dispose(); hudFont.dispose(); hudShape.dispose(); world.dispose()
    }
}

data class CannonballProjectile(val position: Vector3, val direction: Vector3, var life: Float = 2f)
data class HarpoonProjectileEntity(val position: Vector3, val direction: Vector3, var life: Float = 2f)
