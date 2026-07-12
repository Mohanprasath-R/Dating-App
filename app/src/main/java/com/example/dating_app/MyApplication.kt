package com.example.dating_app

import android.app.Application
import com.example.dating_app.util.CloudinaryHelper
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CloudinaryHelper.init(this)
    }

    fun initZegoService(userID: String, userName: String) {
        val appID: Long = 1187941930L // Replace with your AppID
        val appSign = "f4b532e5882f2962b48b9d658de80e7137b445814c4d7fa8cab51c580de14666" // Replace with your AppSign
        
        val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
        
        ZegoUIKitPrebuiltCallService.init(this, appID, appSign, userID, userName, callInvitationConfig)
    }
}
