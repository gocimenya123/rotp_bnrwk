@echo off
rem Creates gradlew / gradlew.bat / gradle-wrapper.jar (Gradle 7.6.4) for this project.
rem Needs any installed Gradle on PATH. Run once, then use gradlew.bat.
set TMPW=%TEMP%\rotp_bnrwk_wrap
rmdir /s /q "%TMPW%" 2>nul
mkdir "%TMPW%"
pushd "%TMPW%"
call gradle wrapper --gradle-version 7.6.4
popd
if not exist "%TMPW%\gradle\wrapper\gradle-wrapper.jar" (
  echo FAILED: gradle wrapper was not generated
  exit /b 1
)
copy /y "%TMPW%\gradlew" "%~dp0gradlew" >nul
copy /y "%TMPW%\gradlew.bat" "%~dp0gradlew.bat" >nul
copy /y "%TMPW%\gradle\wrapper\gradle-wrapper.jar" "%~dp0gradle\wrapper\gradle-wrapper.jar" >nul
copy /y "%TMPW%\gradle\wrapper\gradle-wrapper.properties" "%~dp0gradle\wrapper\gradle-wrapper.properties" >nul
echo Done. Now set Gradle JVM to JDK 8 in IntelliJ and reload the project.
