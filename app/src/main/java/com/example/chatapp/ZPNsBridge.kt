package com.example.chatapp

class ZPNsBridge {
    init {
        System.loadLibrary("zpn_bridge")
    }

    // Declare the native method
    external fun initLogModule(tag: String, level: Int)
}