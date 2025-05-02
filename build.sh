#!/bin/bash

export ANDROID_HOME=/Users/yy/Downloads/android-sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools

# 确保脚本在项目根目录执行
cd "$(dirname "$0")"

case "$1" in
    "emulator-list")
        # 列出所有可用的模拟器
        emulator -list-avds
        ;;
    "emulator-start")
        # 启动模拟器（如果指定了名称则启动指定的，否则启动第一个）
        if [ -n "$2" ]; then
            emulator -avd "$2"
        else
            FIRST_AVD=$(emulator -list-avds | head -n 1)
            if [ -n "$FIRST_AVD" ]; then
                emulator -avd "$FIRST_AVD"
            else
                echo "没有找到可用的模拟器，请先创建一个"
            fi
        fi
        ;;
    "debug")
        open -a "Android Studio" .
        ;;
    "devices")
        # 列出所有连接的设备
        adb devices
        ;;
    "install")
        # 安装到连接的设备
        ./gradlew installDebug
        ;;
    "run")
        # 构建并运行到设备
        ./gradlew installDebug
        adb shell am start -n "com.gosgf.app/.MainActivity"
        ;;
    "logcat")
        # 查看应用日志
        adb logcat | grep "com.gosgf.app"
        ;;
    *)
        echo "使用方法: ./build.sh [命令]"
        echo "可用命令:"
        echo "  debug   - 在 Android Studio 中打开项目"
        echo "  devices - 列出已连接的设备"
        echo "  install - 安装调试版本到设备"
        echo "  run     - 构建并运行应用"
        echo "  logcat  - 查看应用日志"
        echo "  无参数  - 构建调试和发布版本"
        
        # 默认构建行为
        ./gradlew clean
        ./gradlew assembleDebug
        ./gradlew assembleRelease
        
        echo "构建完成！"
        echo "调试版本：app/build/outputs/apk/debug/app-debug.apk"
        echo "发布版本：app/build/outputs/apk/release/app-release.apk"
        ;;
esac