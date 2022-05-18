package com.yjpapp.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class RespFacebookUserInfo (
    var name: String = "",
    var email: String = "",
    var id: String = ""
)