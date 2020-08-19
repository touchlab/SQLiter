cmake_minimum_required(VERSION 3.11)
project(cpp)

set(CMAKE_CXX_STANDARD 11)


include_directories(/Users/kgalligan/devel_kmp/kotlin-native-again/runtime/src/main/cpp)

include_directories(knarch/src/main/cpp)

add_executable(cpp
        knarch/src/main/cpp/SQLiteCommon.cpp
        knarch/src/main/cpp/SQLiteCommon.h
        knarch/src/main/cpp/KonanHelper.cpp
        knarch/src/main/cpp/KonanHelper.h
        knarch/src/main/cpp/UtilsErrors.h)
