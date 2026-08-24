package ru.unicorecms.unicoreconnect.adapters.permissions.forgeessentials

import ru.unicorecms.unicoreconnect.common.platform.PermissionAdapter
import java.lang.reflect.Method
import java.util.UUID

class ForgeEssentialsAdapter : PermissionAdapter {
    override val id = ID

    private val api: Api? by lazy { Api.find() }

    override fun available() = api != null

    override fun addGroup(uuid: UUID, group: String, expiresAt: Long?) {
        val api = api ?: return

        if (!api.groupExists(group)) api.createGroup(group)

        api.addPlayerToGroup(uuid, group)
    }

    override fun removeGroup(uuid: UUID, group: String) {
        api?.removePlayerFromGroup(uuid, group)
    }

    override fun addPermission(uuid: UUID, permission: String, expiresAt: Long?) {
        api?.setPlayerPermission(uuid, permission, true)
    }

    override fun removePermission(uuid: UUID, permission: String) {
        api?.setPlayerPermission(uuid, permission, false)
    }

    override fun groupsOf(uuid: UUID): Set<String> = api?.playerGroups(uuid) ?: emptySet()

    override fun permissionsOf(uuid: UUID): Set<String> = emptySet()

    override fun has(uuid: UUID, permission: String): Boolean? = api?.checkUserPermission(uuid, permission)

    private class Api(
        private val perms: Any,
        private val identOf: Method,
        private val methods: Map<String, Method>,
    ) {
        fun groupExists(group: String) = call("groupExists", group) as? Boolean ?: false

        fun createGroup(group: String) {
            call("createGroup", group)
        }

        fun addPlayerToGroup(uuid: UUID, group: String) {
            call("addPlayerToGroup", ident(uuid) ?: return, group)
        }

        fun removePlayerFromGroup(uuid: UUID, group: String) {
            call("removePlayerFromGroup", ident(uuid) ?: return, group)
        }

        fun setPlayerPermission(uuid: UUID, permission: String, value: Boolean) {
            call("setPlayerPermission", ident(uuid) ?: return, permission, value)
        }

        fun checkUserPermission(uuid: UUID, permission: String): Boolean? {
            return call("checkUserPermission", ident(uuid) ?: return null, permission) as? Boolean
        }

        fun playerGroups(uuid: UUID): Set<String> {
            val result = call("getPlayerGroups", ident(uuid) ?: return emptySet()) as? Collection<*> ?: return emptySet()

            return result.mapNotNull { entry -> groupName(entry) }.toSet()
        }

        private fun groupName(entry: Any?): String? {
            if (entry == null) return null
            if (entry is String) return entry

            return try {
                entry.javaClass.getMethod("getGroup").invoke(entry) as? String
            } catch (error: Throwable) {
                entry.toString()
            }
        }

        private fun ident(uuid: UUID): Any? {
            return try {
                identOf.invoke(null, uuid)
            } catch (error: Throwable) {
                null
            }
        }

        private fun call(name: String, vararg arguments: Any): Any? {
            val method = methods[name] ?: return null

            return try {
                method.invoke(perms, *arguments)
            } catch (error: Throwable) {
                null
            }
        }

        companion object {
            private val SIGNATURES = mapOf(
                "groupExists" to 1,
                "createGroup" to 1,
                "addPlayerToGroup" to 2,
                "removePlayerFromGroup" to 2,
                "setPlayerPermission" to 3,
                "checkUserPermission" to 2,
                "getPlayerGroups" to 1,
            )

            fun find(): Api? {
                return try {
                    val registry = Class.forName("com.forgeessentials.api.APIRegistry")
                    val perms = registry.getField("perms").get(null) ?: return null
                    val identOf = Class.forName("com.forgeessentials.api.UserIdent")
                        .getMethod("get", UUID::class.java)
                    val methods = SIGNATURES.mapNotNull { (name, count) ->
                        val method = perms.javaClass.methods.firstOrNull {
                            it.name == name && it.parameterCount == count && matches(it, name)
                        }

                        method?.let { name to it }
                    }.toMap()

                    if (methods.size != SIGNATURES.size) null else Api(perms, identOf, methods)
                } catch (error: Throwable) {
                    null
                }
            }

            private fun matches(method: Method, name: String): Boolean {
                val types = method.parameterTypes

                return when (name) {
                    "groupExists", "createGroup" -> types[0] == String::class.java
                    "checkUserPermission" -> types[1] == String::class.java
                    "setPlayerPermission" -> types[1] == String::class.java && types[2] == Boolean::class.javaPrimitiveType
                    else -> true
                }
            }
        }
    }

    companion object {
        const val ID = "forgeessentials"
    }
}
