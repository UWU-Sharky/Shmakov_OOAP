@echo off
echo Cleaning old versions...
rd /s /q bin
mkdir bin

echo Compiling...
javac -d bin -cp "lib/*" src/core/*.java src/plugins/*.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b
)

echo Starting application...
java -cp "bin;lib/*" core.LabProjectApp