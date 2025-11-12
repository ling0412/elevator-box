# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================
# 最小化规则：只保留真正必要的规则
# ============================================

# 1. 移除日志调用（减小体积，不影响功能）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# 2. Kotlinx Serialization（必需 - 否则序列化会失败）
# 项目中使用了 @Serializable 注解，如果不保留这些规则：
# - 序列化/反序列化会失败
# - 应用崩溃（ClassNotFoundException 或 NoSuchMethodException）
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================
# 不需要的规则（会增加体积）：
# ============================================
# -keep class androidx.compose.** { *; }
#   → Compose 库自带 ProGuard 规则，不需要额外配置
#   → 如果添加，会增加 2-5MB 体积
#
# -keepclassmembers class com.ling.box.** { <fields>; }
#   → 过于宽泛，会保留所有字段
#   → 如果数据类需要序列化，应该单独指定，例如：
#   -keep class com.ling.box.settings.data.ExportData { *; }
