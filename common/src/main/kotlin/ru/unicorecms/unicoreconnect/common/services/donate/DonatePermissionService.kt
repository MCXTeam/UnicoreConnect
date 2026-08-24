package ru.unicorecms.unicoreconnect.common.services.donate

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.types.DonatePermission
import ru.unicorecms.unicoreconnect.common.types.UserPermission
import java.util.UUID

class DonatePermissionService {
    private val config = UnicoreCommon.config
    private val baseUrl = "${config.apiUrl}/donates/permissions"

    companion object {
        var permissions: Array<DonatePermission> = arrayOf()
    }

    fun load(): Array<DonatePermission> {
        permissions = UnicoreCommon.requester.get("$baseUrl/server/uc/${config.server}").getOrThrow()

        return permissions
    }

    fun userPermissions(uuid: UUID): Array<UserPermission> {
        return UnicoreCommon.requester.get("$baseUrl/user/${config.server}/$uuid").getOrThrow()
    }
}
