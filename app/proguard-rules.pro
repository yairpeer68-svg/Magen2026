# Magen v4.6.0 release hardening.
# Android Gradle Plugin automatically keeps manifest-referenced components. Keep only
# the data required by framework/native boundaries; avoid blanket -keep rules so R8
# can still shrink and obfuscate the application.

# Keep enum members used by platform serialization/logging stable.
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }

# Keep custom View constructors if XML inflation references a custom class.
-keepclasseswithmembers,allowobfuscation class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Preserve source/line metadata for useful crash reports without exposing local variables.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
