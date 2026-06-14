package com.leviathan.game.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

enum class PacketType {
    @SerializedName("handshake") HANDSHAKE,
    @SerializedName("role_select") ROLE_SELECT,
    @SerializedName("ready") READY,
    @SerializedName("start_game") START_GAME,
    @SerializedName("game_state") GAME_STATE,
    @SerializedName("ship_input") SHIP_INPUT,
    @SerializedName("monster_input") MONSTER_INPUT,
    @SerializedName("cannon_fire") CANNON_FIRE,
    @SerializedName("harpoon_fire") HARPOON_FIRE,
    @SerializedName("ability_activate") ABILITY_ACTIVATE,
    @SerializedName("damage") DAMAGE,
    @SerializedName("entity_state") ENTITY_STATE,
    @SerializedName("game_over") GAME_OVER,
    @SerializedName("chat") CHAT
}

enum class PlayerRole { NONE, SHIP, MONSTER }
enum class GameState { LOBBY, PLAYING, SHIP_WINS, MONSTER_WINS }
enum class DamageType { CANNON, HARPOON, TENTACLE, DASH, EMP, WHIRLPOOL, ENVIRONMENT }
enum class MonsterAbility { TENTACLE, DASH, EMP, WHIRLPOOL, DEEP_DIVE, SONAR_JAMMER }

open class Packet(val type: PacketType)

class HandshakePacket : Packet(PacketType.HANDSHAKE) {
    var clientName: String = "Player"
    var version: String = "1.0.0"
}

class RoleSelectPacket : Packet(PacketType.ROLE_SELECT) {
    var role: PlayerRole = PlayerRole.NONE
}

class ReadyPacket : Packet(PacketType.READY) {
    var ready: Boolean = false
}

class StartGamePacket : Packet(PacketType.START_GAME)
class GameOverPacket : Packet(PacketType.GAME_OVER) {
    var winner: PlayerRole = PlayerRole.NONE
    var reason: String = ""
}

class GameStatePacket : Packet(PacketType.GAME_STATE) {
    var state: GameState = GameState.LOBBY
    var timeRemaining: Float = 600f
    var shipHp: Float = 1000f
    var monsterHp: Float = 1500f
    var monsterEnergy: Float = 200f
}

class ShipInputPacket : Packet(PacketType.SHIP_INPUT) {
    var moveX: Float = 0f
    var moveY: Float = 0f
    var fireCannon: Boolean = false
    var fireHarpoon: Boolean = false
    var turbo: Boolean = false
    var repair: Boolean = false
    var radar: Boolean = false
}

class MonsterInputPacket : Packet(PacketType.MONSTER_INPUT) {
    var moveX: Float = 0f
    var moveY: Float = 0f
    var moveZ: Float = 0f
    var ability: MonsterAbility? = null
}

class CannonFirePacket : Packet(PacketType.CANNON_FIRE) {
    var posX: Float = 0f
    var posY: Float = 0f
    var posZ: Float = 0f
    var dirX: Float = 0f
    var dirY: Float = 0f
    var dirZ: Float = 0f
}

class HarpoonFirePacket : Packet(PacketType.HARPOON_FIRE) {
    var posX: Float = 0f
    var posY: Float = 0f
    var posZ: Float = 0f
    var dirX: Float = 0f
    var dirY: Float = 0f
    var dirZ: Float = 0f
}

class AbilityActivatePacket : Packet(PacketType.ABILITY_ACTIVATE) {
    var ability: MonsterAbility = MonsterAbility.TENTACLE
    var targetX: Float = 0f
    var targetY: Float = 0f
    var targetZ: Float = 0f
}

class DamagePacket : Packet(PacketType.DAMAGE) {
    var target: String = ""
    var damage: Float = 0f
    var damageType: DamageType = DamageType.ENVIRONMENT
}

class EntityStatePacket : Packet(PacketType.ENTITY_STATE) {
    var entityId: String = ""
    var posX: Float = 0f
    var posY: Float = 0f
    var posZ: Float = 0f
    var rotY: Float = 0f
    var hp: Float = 100f
    var energy: Float = 200f
}

class ChatPacket : Packet(PacketType.CHAT) {
    var message: String = ""
    var sender: String = ""
}

object PacketSerializer {
    @PublishedApi internal val gson = Gson()

    fun serialize(packet: Packet): String = gson.toJson(packet) + "\n"

    inline fun <reified T : Packet> deserialize(json: String): T? {
        return try {
            val type = gson.fromJson(json, JsonTypePacket::class.java).type
            when (type) {
                PacketType.HANDSHAKE -> gson.fromJson(json, HandshakePacket::class.java)
                PacketType.ROLE_SELECT -> gson.fromJson(json, RoleSelectPacket::class.java)
                PacketType.READY -> gson.fromJson(json, ReadyPacket::class.java)
                PacketType.START_GAME -> gson.fromJson(json, StartGamePacket::class.java)
                PacketType.GAME_OVER -> gson.fromJson(json, GameOverPacket::class.java)
                PacketType.GAME_STATE -> gson.fromJson(json, GameStatePacket::class.java)
                PacketType.SHIP_INPUT -> gson.fromJson(json, ShipInputPacket::class.java)
                PacketType.MONSTER_INPUT -> gson.fromJson(json, MonsterInputPacket::class.java)
                PacketType.CANNON_FIRE -> gson.fromJson(json, CannonFirePacket::class.java)
                PacketType.HARPOON_FIRE -> gson.fromJson(json, HarpoonFirePacket::class.java)
                PacketType.ABILITY_ACTIVATE -> gson.fromJson(json, AbilityActivatePacket::class.java)
                PacketType.DAMAGE -> gson.fromJson(json, DamagePacket::class.java)
                PacketType.ENTITY_STATE -> gson.fromJson(json, EntityStatePacket::class.java)
                PacketType.CHAT -> gson.fromJson(json, ChatPacket::class.java)
                else -> null
            } as? T
        } catch (e: Exception) {
            null
        }
    }

    internal class JsonTypePacket(@PublishedApi internal val type: PacketType)
}
