#!/bin/bash
export ANDROID_SDK_ROOT=$ANDROID_HOME
export ANDROID_NDK_ROOT=$ANDROID_HOME/ndk/23.1.7779620

bash ~/code/other/ffmpeg-kit/android.sh \
	--lts \
	--disable-arm-v7a-neon \
	--disable-x86 \
	--disable-x86-64 \
	--enable-android-media-codec \
	--enable-android-zlib \
	--enable-gmp \
	--enable-gnutls \
	--enable-openh264 

