import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
    id("com.google.android.gms.oss-licenses-plugin")
    id("com.novoda.build-properties") version "0.4.1"
    id("com.google.gms.google-services")
}

buildProperties {
    create("env") {
        using(System.getenv() as Map<String, Any>?)
    }
    create("secrets") {
        if (rootProject.file("secret_keys.properties").exists()) {
            using(rootProject.file("secret_keys.properties"))
        } else {
            println("Secret keys properties file does not exist. Setting `secrets`" +
                    "build property as empty.")
            using(mapOf())
        }
    }
    // Version metadata
    create("versions") {
        using(rootProject.file("versions.properties"))
    }
}

val envProperties = buildProperties["env"].asMap()
val versionsProperties = buildProperties["versions"].asMap()
val secretsProperties = buildProperties["secrets"].asMap()

extra.apply {
    // See the Semver guide for more info
    // Adapted from https://medium.com/@maxirosson/versioning-android-apps-d6ec171cfd82
    // Allows the user to specify the versionMajor via the command-line (See below for an example)
    set(
        "versionMajor", project.properties["versionMajor"] as Int? ?:
            versionsProperties["version_major"]?.int
    )
    // Allows the user to specify the versionMinor via the command-line (See below for an example)
    set(
        "versionMinor", project.properties["versionMinor"] as Int? ?:
            versionsProperties["version_minor"]?.int
    )
    // Allows the user to specify the versionPatch via the command-line (See below for an example)
    set(
        "versionPatch", project.properties["versionPatch"] as Int? ?:
            versionsProperties["version_patch"]?.int
    )
    // Allows the user to specify the versionClassifier via the command-line
    // Example: ./gradlew <task> -PversionClassifier=nightly
    set("versionClassifier", project.properties["versionClassifier"])
    // Allows the user to specify the currentVariant via the command-line  (See below for an example)
    set("currentVariant", project.properties["currentVariant"])
}

val buildTimeString: String
    get() {
        val format = SimpleDateFormat("yyyy-MM-dd-HHmmss")
        return format.format(Date())
    }

fun generateVersionCodeBuildVariant(): Int {
    // Default is 1 to represent release variant
    // 0: Debug variant
    // 1: Release variant
    // 10: Nightly variant
    var result = 1
    when (extra["currentVariant"]) {
        "debug" -> {
            result = 0
        }
        "nightly" -> {
            result = 10
        }
    }
    println("Generated version code (build variant): $result")
    return result
}

fun generateVersionCodeClassifier(): Int {
    // Default is 0 to represent no version classifier
    var result = 0
    when (extra["versionClassifier"]) {
        "alpha" -> {
            result = 1
        }
        "beta" -> {
            result = 2
        }
        "rc" -> {
            result = 3
        }
        "nightly" -> {
            result = 10
        }
    }
    println("Generated version code (version classifier): $result")
    return result
}

fun generateVersionCode(): Int {
    /*
     * Returns <build-variant type as number>-<version classifier>-<major>-<minor>-<patch>
     * E.g. 01-10-01-01-00, with the following config:
     * - release build variant
     * - nightly classifier
     * - version major: 1
     * - version minor: 1
     * - version patch: 0
     */
    return (generateVersionCodeBuildVariant() * 100000000 + generateVersionCodeClassifier() * 1000000
            + extra["versionMajor"].cast<Int>() * 10000 + extra["versionMinor"].cast<Int>() * 100
            + extra["versionPatch"].cast<Int>())
}

fun generateVersionName(): String {
    return "${extra["versionMajor"]}.${extra["versionMinor"]}.${extra["versionPatch"]}"
}

android {
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "com.edricchan.studybuddy"
        minSdkVersion(21)
        targetSdkVersion(29)
        // versionCode 9
        versionCode = generateVersionCode()
        println("Generated version code: ${generateVersionCode()}")
        versionName = generateVersionName()
        println("Generated version name: ${generateVersionName()}")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resConfigs("en") // TODO: Don't restrict to just en
        multiDexEnabled = true
        // Enable support library for vector drawables
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        maybeCreate("release").apply {
            storeFile(rootProject.file("studybuddy.jks"))
        }
    }

    buildTypes {
        maybeCreate("debug").apply {
            debuggable(true) // Allow app to be debuggable
            applicationIdSuffix = ".debug"

            // Reduce amount of time needed to compile app in debug mode
            // Can be accessed with BuildConfig.BUILD_TIME
            buildConfigField("Long", "BUILD_TIME", "0L")
            // Can be accessed with R.int.build_time
            resValue("integer", "build_time", "0")
            extra["currentVariant"] = "debug"
        }
        maybeCreate("release").apply {
            isMinifyEnabled = true // Enable minification
            isShrinkResources = true // Shrink resources to reduce APK size
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs["release"]
            // Can be accessed with BuildConfig.BUILD_TIME
            buildConfigField("long", "BUILD_TIME", System.currentTimeMillis().toString())
            // Can be accessed with R.int.build_time
            resValue("integer", "build_time", System.currentTimeMillis().toInt().toString())
            extra["currentVariant"] = "release"
        }
        maybeCreate("nightly").apply {
            // Nightly releases
            initWith(getByName("release"))
            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-NIGHTLY-$buildTimeString"
            extra["currentVariant"] = "nightly"
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }


    buildToolsVersion = "29.0.2"

    // GitHub Actions always sets GITHUB_ACTIONS to true when running the workflow.
    // See https://help.github.com/en/actions/automating-your-workflow-with-github-actions/using-environment-variables#default-environment-variables
    // for more info.
    val isRunningOnActions = envProperties["GITHUB_ACTIONS"]?.boolean ?: false

    lintOptions {
        textReport = isRunningOnActions
        textOutput("stdout")
        isAbortOnError = false
        baseline(file("lint-baseline.xml"))
    }

    if (isRunningOnActions && envProperties.isNotEmpty()) {
        // Configure keystore
        signingConfigs["release"].storePassword = envProperties["APP_KEYSTORE_PASSWORD"]?.string
        signingConfigs["release"].keyAlias = envProperties["APP_KEYSTORE_ALIAS"]?.string
        signingConfigs["release"].keyPassword = envProperties["APP_KEYSTORE_ALIAS_PASSWORD"]?.string
    } else if (secretsProperties.isNotEmpty()) {
        // Building locally
        signingConfigs["release"].storePassword = secretsProperties["keystore_password"]?.string
        signingConfigs["release"].keyAlias = secretsProperties["keystore_alias"]?.string
        signingConfigs["release"].keyPassword =
            secretsProperties["keystore_alias_password"]?.string
    }
}

dependencies {
    // Support Lib
    // implementation("androidx.legacy:legacy-support-v4:1.0.0")
    // Support Annotations
    implementation("androidx.annotation:annotation:1.1.0")
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
    // AndroidX Test
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
    androidTestImplementation("androidx.test:core:1.3.0-beta01")
    androidTestImplementation("androidx.test:runner:1.3.0-beta01")
    androidTestImplementation("androidx.test:rules:1.3.0-beta01")
    testImplementation("org.mockito:mockito-core:3.3.3")
    // AndroidX Core
    implementation("androidx.core:core:1.3.0-rc01")
    implementation("androidx.core:core-ktx:1.3.0-rc01")
    // AppCompat
    implementation("androidx.appcompat:appcompat:1.2.0-beta01")
    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta4")
    // Design Support
    implementation("com.google.android.material:material:1.2.0-alpha06")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.2.0-alpha03")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0-rc01")
    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0-rc01")
    // Chrome Custom Tabs
    implementation("androidx.browser:browser:1.3.0-alpha01")
    testImplementation("junit:junit:4.13")
    // Firebase stuff
    implementation("com.google.firebase:firebase-analytics-ktx:17.4.0")
    implementation("com.google.firebase:firebase-firestore-ktx:21.4.3")
    implementation("com.google.firebase:firebase-auth-ktx:19.3.1")
    implementation("com.google.firebase:firebase-messaging:20.1.6")
    implementation("com.google.android.gms:play-services-auth:18.0.0")
    implementation("com.google.firebase:firebase-perf:19.0.7")
    implementation("com.google.firebase:firebase-dynamic-links-ktx:19.1.0")
    implementation("com.google.guava:guava:29.0-android")
    // Provide a way to update the app
    implementation("com.github.javiersantos:AppUpdater:2.7")
    // The app"s intro screen
    // implementation("com.heinrichreimersoftware:material-intro:1.6.2")
    // Crashlytics
    // implementation("com.crashlytics.sdk.android:crashlytics:2.10.1")
    implementation("com.google.firebase:firebase-crashlytics:17.0.0")
    // For JSON parsing
    implementation("com.google.code.gson:gson:2.8.6")
    // Emoji AppCompat
    implementation("androidx.emoji:emoji-appcompat:1.0.0")
    // AndroidX Preference
    implementation("androidx.preference:preference:1.1.1")
    // Kotlin support for AndroidX Preference library
    implementation("androidx.preference:preference-ktx:1.1.1")
    // AndroidX WorkManager
    implementation("androidx.work:work-runtime-ktx:2.4.0-alpha03")

    // Custom Preferences
    /*
    - New releases to the AndroidX preference library have broken the following 2 libraries
      (this has since been fixed in newer versions of the library)
    - See https://github.com/takisoft/preferencex-android/issues/4 for the relevant issue
      and https://issuetracker.google.com/issues/128579401 for the issue tracker
    */
    implementation("com.takisoft.preferencex:preferencex:1.1.0")
    // implementation("com.takisoft.preferencex:preferencex-datetimepicker:1.1.0-alpha05")

    // Image picker
    implementation("com.github.esafirm.android-image-picker:imagepicker:2.1.0")

    // About screen
    implementation("com.github.daniel-stoneuk:material-about-library:2.4.2")
    // Explicitly specify the AndroidX CardView library for now
    implementation("androidx.cardview:cardview:1.0.0")

    // Markwon - Markdown parser for Android
    // See https://noties.io/Markwon for more info
    val markwon_version = "4.3.1"
    implementation("io.noties.markwon:core:$markwon_version")
    implementation("io.noties.markwon:editor:$markwon_version")
    implementation("io.noties.markwon:ext-strikethrough:$markwon_version")
    implementation("io.noties.markwon:ext-tables:$markwon_version")
    implementation("io.noties.markwon:ext-tasklist:$markwon_version")
    implementation("io.noties.markwon:html:$markwon_version")
    implementation("io.noties.markwon:image-glide:$markwon_version")
    implementation("io.noties.markwon:linkify:$markwon_version")
    implementation("io.noties.markwon:syntax-highlight:$markwon_version")
    // See https://github.com/noties/Markwon/issues/148#issuecomment-508003794
    configurations.all {
        exclude("org.jetbrains", "annotations-java5")
    }

    // Used to handle loading of remote images
    implementation("com.github.bumptech.glide:glide:4.11.0")
    kapt("com.github.bumptech.glide:compiler:4.11.0")

    // ExpandableLayout
    implementation("net.cachapa.expandablelayout:expandablelayout:2.9.2")

    // OSS licenses library to show open source licenses
    implementation("com.google.android.gms:play-services-oss-licenses:17.0.0")
    // Backported Java Stream library for Java 6/7
    implementation("net.sourceforge.streamsupport:streamsupport:1.7.2")

    // Support for Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${rootProject.extra["kotlin_version"]}")

    // Support for parsing Deep Links
    implementation("com.airbnb:deeplinkdispatch:5.1.0")
    kapt("com.airbnb:deeplinkdispatch-processor:5.1.0")
}

kapt {
    arguments {
        // See https://github.com/airbnb/DeepLinkDispatch#incremental-annotation-processing for
        // more info
        arg("deepLink.incremental", "true")
        arg(
            "deepLink.customAnnotations",
            "com.edricchan.studybuddy.annotations.AppDeepLink,"
                    + "com.edricchan.studybuddy.annotations.WebDeepLink"
        )
        // Add support for documenting deep links
        // See https://github.com/airbnb/DeepLinkDispatch#generated-deep-links-documentation for
        // more info
        arg("deepLinkDoc.output", "${rootDir}/docs/deeplinks.md")
    }
}