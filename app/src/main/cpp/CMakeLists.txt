cmake_minimum_required(VERSION 3.4.1)

add_library(zpn_bridge SHARED ZPNsBridge.cpp)

find_library(log-lib log)

target_link_libraries(zpn_bridge ${log-lib})
