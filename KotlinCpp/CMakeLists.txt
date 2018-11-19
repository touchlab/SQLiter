cmake_minimum_required(VERSION 3.11)
project(cpp)

set(CMAKE_CXX_STANDARD 11)


include_directories(/Users/kgalligan/temp6/kotlin-native-1.0/runtime/src/main/cpp)
include_directories(knarch/src/main/cpp)


add_executable(cpp
        knarch/src/main/cpp/android_database_CursorWindow.cpp
        knarch/src/main/cpp/android_database_SQLiteCommon.cpp
        knarch/src/main/cpp/android_database_SQLiteCommon.h
        knarch/src/main/cpp/android_database_SQLiteConnection.cpp
        knarch/src/main/cpp/android_database_SQLiteGlobal.cpp
        knarch/src/main/cpp/AndroidfwCursorWindow.cpp
        knarch/src/main/cpp/AndroidfwCursorWindow.h
        knarch/src/main/cpp/SQLiteSupport.cpp
        knarch/src/main/cpp/KonanHelper.cpp
        knarch/src/main/cpp/KonanHelper.h
        knarch/src/main/cpp/UtilsErrors.h)
