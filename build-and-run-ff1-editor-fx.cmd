@echo off
setlocal
call "%~dp0build-with-jdk.cmd"
if errorlevel 1 exit /b %errorlevel%
set "JFX_BASE=%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\25.0.1\javafx-base-25.0.1-win.jar"
set "JFX_GRAPHICS=%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\25.0.1\javafx-graphics-25.0.1-win.jar"
set "JFX_CONTROLS=%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\25.0.1\javafx-controls-25.0.1-win.jar"
set "JFX_MODULE_PATH=%JFX_BASE%;%JFX_GRAPHICS%;%JFX_CONTROLS%"
java --module-path "%JFX_MODULE_PATH%" --add-modules javafx.controls -jar target\ff1-data-editor-0.1.0.jar %*
