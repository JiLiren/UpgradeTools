#include "VerifyFilesLib.h"
#include "VerifyFilesLibPrivate.h"
#include "GetFileMD5.h"
#include <iostream>
#include <string>



const char* jstringToChar(JNIEnv* env, jstring jstr)
{
  const char* cppMsg=env->GetStringUTFChars(jstr, JNI_FALSE);
  env->ReleaseStringUTFChars(jstr, cppMsg);
  return cppMsg;
}

//char* jstringToChar(JNIEnv* env, jstring jstr)
//{
//  char* rtn = NULL;
//  jclass clsstring = env->FindClass("java/lang/String");
//  jstring strencode = env->NewStringUTF("GB2312");
//  jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(java/lang/String;)[B");
//  jbyteArray barr = (jbyteArray) (*env)->CallObjectMethod(env, jstr, mid, strencode);
//  jsize alen = env->GetArrayLength(barr);
//  jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
//  if (alen > 0) {
//  	rtn = (char*) malloc(alen + 1);
//  	memcpy(rtn, ba, alen);
//  	rtn[alen] = 0;
//  }
//  env->ReleaseByteArrayElements(barr, ba, 0);
//  return rtn;
//}

jstring charTojstring(JNIEnv* env, const char* pat) {
  //定义java String类 strClass
  jclass strClass = env->FindClass( "java/lang/String;");
  //获取String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
  jmethodID ctorID = env->GetMethodID( strClass, "<init>", "([BLjava/lang/String;)V");
  //建立byte数组
  jbyteArray bytes = env->NewByteArray(strlen(pat));
  //将char* 转换为byte数组
  env->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte*) pat);
  // 设置String, 保存语言类型,用于byte数组转换至String时的参数
  jstring encoding = env->NewStringUTF( "GB2312");
    //将byte数组转换为java String,并输出
  return (jstring) env->NewObject(strClass, ctorID, bytes, encoding);
}

JNIEXPORT jstring JNICALL Java_com_ritu_upgrade_tools_EncryptionTools_GetFileMD5(JNIEnv* env, jobject thiz, jstring jstrFileName) {
  const char* szFileName = jstringToChar(env, jstrFileName);
  std::string strFileName = szFileName;
  std::string strMD5 = GetFileHash(env,strFileName);

//   LOGE(env->NewStringUTF(strMD5.c_str()));
  const char* str = strMD5.c_str();
  return env->NewStringUTF(str);
}
