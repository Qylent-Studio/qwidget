plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

// Windows test workers can misread non-ASCII output paths. Optional local override.
providers.environmentVariable("QWIDGET_BUILD_ROOT").orNull?.let { outputRoot ->
    subprojects { layout.buildDirectory.set(file("$outputRoot/$name")) }
}
