package ru.unicorecms.unicoreconnect.common.types

class CommandsAck(
    val done: List<Int>,
    val failed: List<Int>,
)
