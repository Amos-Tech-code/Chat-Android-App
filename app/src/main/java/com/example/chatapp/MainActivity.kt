 package com.example.chatapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.example.chatapp.ui.theme.ChatAppTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.permissionx.guolindev.PermissionX
import com.zegocloud.uikit.internal.ZegoUIKitLanguage
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.core.invite.ZegoCallInvitationData
import com.zegocloud.uikit.prebuilt.call.event.CallEndListener
import com.zegocloud.uikit.prebuilt.call.event.ErrorEventsListener
import com.zegocloud.uikit.prebuilt.call.event.SignalPluginConnectListener
import com.zegocloud.uikit.prebuilt.call.event.ZegoCallEndReason
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoTranslationText
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoUIKitPrebuiltCallConfigProvider
import dagger.hilt.android.AndroidEntryPoint
import im.zego.zim.enums.ZIMConnectionEvent
import im.zego.zim.enums.ZIMConnectionState
import org.json.JSONObject
import timber.log.Timber

 @AndroidEntryPoint
class MainActivity : FragmentActivity() {

     private val bridge = ZPNsBridge() // Create an instance of ZPNsBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatAppTheme(darkTheme = false) {
                SetBarContentToWhite(color = Color.White)
                MainApp()
            }
        }

        permissionHandling(this)

        // Initialize logging
        bridge.initLogModule("MyTag", 1)
    }


     fun initZegoService(appID: Long, appSign: String, userID: String, userName: String) {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             // Initialize Zego service for devices running Android O and above
             val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
             callInvitationConfig.translationText = ZegoTranslationText(ZegoUIKitLanguage.ENGLISH)
             callInvitationConfig.provider =
                 ZegoUIKitPrebuiltCallConfigProvider { invitationData: ZegoCallInvitationData? ->
                     ZegoUIKitPrebuiltCallInvitationConfig.generateDefaultConfig(
                         invitationData
                     )
                 }

             ZegoUIKitPrebuiltCallService.events.errorEventsListener =
                 ErrorEventsListener { errorCode: Int, message: String ->
                     Timber.d("onError() called with: errorCode = [$errorCode], message = [$message]")
                 }

             ZegoUIKitPrebuiltCallService.events.invitationEvents.pluginConnectListener =
                 SignalPluginConnectListener { state: ZIMConnectionState, event: ZIMConnectionEvent, extendedData: JSONObject ->
                     Timber.d("onSignalPluginConnectionStateChanged() called with: state = [$state], event = [$event], extendedData = [$extendedData$]")
                 }

             ZegoUIKitPrebuiltCallService.init(
                 application, appID, appSign, userID, userName, callInvitationConfig
             )

             ZegoUIKitPrebuiltCallService.enableFCMPush()

             ZegoUIKitPrebuiltCallService.events.callEvents.callEndListener =
                 CallEndListener { callEndReason: ZegoCallEndReason?, jsonObject: String? ->
                     Log.d(
                         "CallEndListener",
                         "Call Ended with reason: $callEndReason and json: $jsonObject"
                     )
                 }
         } else {
             Log.e("MainActivity", "ZegoUIKitPrebuiltCallService not supported below Android O")
         }
     }


     private fun permissionHandling(activityContext: FragmentActivity) {
         PermissionX.init(activityContext).permissions(Manifest.permission.SYSTEM_ALERT_WINDOW)
             .onExplainRequestReason { scope, deniedList ->
                 val message =
                     "We need your consent for the following permissions in order to use the offline call function properly"
                 scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny")
             }.request { allGranted, grantedList, deniedList -> }
     }



     @Composable
     private fun SetBarContentToWhite(color: Color) {
         val systemUiController = rememberSystemUiController()

         LaunchedEffect(Unit) {
             systemUiController.setSystemBarsColor(

                 color = color,
                 darkIcons = true
             )
         }
     }


}
