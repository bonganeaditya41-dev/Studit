# Firestore / Auth model classes must keep their no-arg constructors.
-keepclassmembers class com.studyit.data.model.** {
    <init>();
    *;
}
-keep class com.studyit.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.firebase.**
