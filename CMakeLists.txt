cmake_minimum_required(VERSION 3.16)
project(dropin)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_AUTOMOC ON)
find_package(Qt6 REQUIRED COMPONENTS Core Widgets Network)
add_executable(dropin
    main.cpp
    mainwindow.cpp
)
target_link_libraries(dropin Qt6::Core Qt6::Widgets Qt6::Network)
