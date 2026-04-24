# Additional clean files
cmake_minimum_required(VERSION 3.16)

if("${CONFIG}" STREQUAL "" OR "${CONFIG}" STREQUAL "")
  file(REMOVE_RECURSE
  "CMakeFiles/onenode_autogen.dir/AutogenUsed.txt"
  "CMakeFiles/onenode_autogen.dir/ParseCache.txt"
  "onenode_autogen"
  )
endif()
