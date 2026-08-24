package ru.unicorecms.unicoreconnect.common.services.donate

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.types.DonateGroup
import ru.unicorecms.unicoreconnect.common.types.UserDonate
import java.util.UUID

class DonateGroupService {
    private val config = UnicoreCommon.config
    private val baseUrl = "${config.apiUrl}/donates/groups"

    companion object {
        var groups: Array<DonateGroup> = arrayOf()
    }

    fun load(): Array<DonateGroup> {
        groups = UnicoreCommon.requester.get("$baseUrl/server/${config.server}").getOrThrow()

        return groups
    }

    fun userGroups(uuid: UUID): Array<UserDonate> {
        return UnicoreCommon.requester.get("$baseUrl/user/${config.server}/$uuid").getOrThrow()
    }
}
