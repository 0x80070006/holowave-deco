# HOLOWAVE has no reflection-based serialization: org.json and the platform APIs only.
# Strip verbose/debug logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
