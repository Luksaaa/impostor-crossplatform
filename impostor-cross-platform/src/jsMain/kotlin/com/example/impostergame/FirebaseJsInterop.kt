package com.example.impostergame

import kotlin.js.Json

external val firebase: dynamic

external class JsDataSnapshot {
    fun `val`(): dynamic
    fun exists(): Boolean
    val key: String?
}

external interface RoomJsInterop {
    var admin: String?
    var status: String?
    var imposterId: String?
    var mrWhiteId: String?
    var imposterWord: String?
    var mainWord: String?
    var isDiscussionActive: Boolean?
    var discussionStartTime: Double?
    var discussionEndTime: Double?
    var resultMessage: String?
    var chatMessages: Json?
    var players: Json?
    var messages: Json?
}

external interface PlayerInfoJsInterop {
    var name: String?
    var isReady: Boolean?
    var joinedAt: Double?
}

external interface ChatMessageJsInterop {
    var sender: String?
    var message: String?
    var timestamp: Double?
}
