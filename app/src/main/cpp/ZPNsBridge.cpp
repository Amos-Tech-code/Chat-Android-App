#include <jni.h>
#include <android/log.h>

extern "C" {
JNIEXPORT void JNICALL Java_com_example_chatapp_ZPNsBridge_initLogModule(JNIEnv *env, jobject obj, jstring tag, jlong level) {
const char *tagStr = env->GetStringUTFChars(tag, nullptr);
__android_log_print(ANDROID_LOG_INFO, tagStr, "Log module initialized with level: %ld", level);
env->ReleaseStringUTFChars(tag, tagStr);
}
}
