@echo off
rem Builds the mod. Needs JDK 25 and, the first time, a `gradle` install (only to generate the wrapper).
cd /d "%~dp0"
if not exist gradle\wrapper\gradle-wrapper.jar (
  where gradle >nul 2>nul || (echo Install Gradle once to generate the wrapper, or open the folder in IntelliJ. & exit /b 1)
  call gradle wrapper --gradle-version 9.6.0 || exit /b 1
)
call gradlew.bat build --stacktrace || exit /b 1
if not exist dist mkdir dist
copy /y build\libs\*.jar dist\ >nul
echo Done - see dist\
