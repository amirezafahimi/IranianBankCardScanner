# ML Kit text recognition loads its models reflectively.
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text** { *; }
