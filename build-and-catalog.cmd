@echo off
setlocal
call build-with-jdk.cmd
if errorlevel 1 exit /b %errorlevel%
java -jar target\ff1-data-editor-0.1.0.jar ff1.jar --catalog target\ff1-catalog.md
