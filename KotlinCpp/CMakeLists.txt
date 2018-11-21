cmake_minimum_required(VERSION 3.11)
project(cpp)

set(CMAKE_CXX_STANDARD 11)


include_directories(knarch/src/main/cpp)

include_directories(/Users/kgalligan/temp6/kotlin-native-1.0/runtime/src/main/cpp)

add_executable(cpp
        knarch/src/main/cpp/android_database_SQLiteCommon.cpp
        knarch/src/main/cpp/android_database_SQLiteCommon.h
        knarch/src/main/cpp/android_database_SQLiteConnection.cpp
        knarch/src/main/cpp/KonanHelper.cpp
        knarch/src/main/cpp/KonanHelper.h
        knarch/src/main/cpp/UtilsErrors.h)
