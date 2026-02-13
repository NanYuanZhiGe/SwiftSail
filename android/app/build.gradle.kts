plugins {
    id("com.android.application")
}

android {
    namespace = "com.nyzg.swiftsail"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nyzg.swiftsail"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable=true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime:2.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.google.code.gson:gson:2.10.1")
    //底部滑出面板的组件
    implementation("com.sothree.slidinguppanel:library:3.4.0")

    //柱状图、折线图等报表组件
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    //私有云地图瓦片显示组件
    implementation("org.maplibre.gl:android-sdk:11.11.0")

    //指纹识别
    implementation("androidx.biometric:biometric:1.1.0")

    //环形进度条
    implementation("com.seosh817:circularseekbar:1.0.2")

    //custom tabs用于站外导航
    implementation("androidx.browser:browser:1.2.0")

    //tab layout
    implementation("com.google.android.material:material:1.13.0")

    //新版recycler view，解决在nested scroll view下wrap content的问题
    //然而并没有解决，用了其他的方式解决UnsafeButFixProb
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("androidx.lifecycle:lifecycle-service:2.4.1")

    implementation("com.github.skydoves:balloon:1.7.3")

    //用于解决键盘弹出和收回的监听方案
    implementation("com.github.boybeak:skb-global:0.2.0")

    //websocket支持
    implementation("org.java-websocket:Java-WebSocket:1.6.0")
}