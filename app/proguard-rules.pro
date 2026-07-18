# R8 full mode is on by default in AGP 8. The libraries in use (Compose, Room, Hilt,
# DataStore) all ship their own consumer rules, so nothing app-specific is needed yet.
#
# The math engine is plain Kotlin with no reflection and no serialization, so it shrinks
# without help. Add rules here only if a real crash proves one necessary.

-dontwarn org.jetbrains.annotations.**
