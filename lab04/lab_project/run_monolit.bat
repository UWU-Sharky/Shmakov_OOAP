@echo off
set JAR_NAME=sqlite-jdbc.jar

:: Поиск любого файла, начинающегося на sqlite-jdbc
for %%f in (sqlite-jdbc*.jar) do set JAR_NAME=%%f

echo Using driver: %JAR_NAME%

javac -cp ".;%JAR_NAME%" MonolithicApp.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b
)

java -cp ".;%JAR_NAME%" MonolithicApp
pause