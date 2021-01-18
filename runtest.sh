#Runs specific test. Pass in test name.
./gradlew linkDebugTestMacosX64
./sqliter-driver/build/bin/macosX64/debugTest/test.kexe --ktest_regex_filter=.*$1.*
