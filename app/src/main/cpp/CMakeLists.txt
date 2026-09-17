cmake_minimum_required(VERSION 3.22.1)
project(autocare_ai LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
# Inference must remain optimized in debug APKs used for device measurements.
add_compile_options("$<$<CONFIG:Debug>:-O3>")
include(FetchContent)
# llama.cpp b6900: pin both source revision and archive digest.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
FetchContent_Declare(llama
    URL https://codeload.github.com/ggml-org/llama.cpp/tar.gz/c22473b580807929fd9e3a3344a48e8cfbe6c88f
    URL_HASH SHA256=9e8e85ad057669b7eea657a7deabd73d8431867a79033545ab17d082ebe53ce6)
FetchContent_MakeAvailable(llama)
add_library(autocare_ai SHARED autocare_ai.cpp)
target_link_libraries(autocare_ai PRIVATE llama android log)
# Support devices using 16 KB memory pages.
target_link_options(autocare_ai PRIVATE "-Wl,-z,max-page-size=16384")
