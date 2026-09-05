# R8 已启用（release: isMinifyEnabled + isShrinkResources）。
# Room/Media3/OkHttp/Coil/DataStore 均通过 AAR 自带 consumer 规则，无需额外 keep。
# 如后续接入反射/序列化框架，在此补充精确 keep 规则。
# 发布时请归档 app/build/outputs/mapping/release/mapping.txt 用于崩溃堆栈反混淆。
