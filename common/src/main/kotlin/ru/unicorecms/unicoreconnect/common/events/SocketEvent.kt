package ru.unicorecms.unicoreconnect.common.events

import ru.unicorecms.unicoreconnect.common.types.UserDonate
import ru.unicorecms.unicoreconnect.common.types.UserPermission

class SocketEvent {
    class GIVE_GROUP(val payload: UserDonate)
    class TAKE_GROUP(val payload: UserDonate)
    class GIVE_PERMISSION(val payload: UserPermission)
    class TAKE_PERMISSION(val payload: UserPermission)
    class RUN_COMMANDS
}