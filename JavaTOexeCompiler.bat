@echo off
title MyBeans Cashier Launcher
color 0B

:MENU
cls
echo ===========================================
echo    MYBEANS CASHIER - Beta Test
echo ===========================================
echo  1. Build and Run (Compile new changes)
echo  2. Just Run (Launch Compiled app)
echo  3. Exit
echo ===========================================
echo.
set /p choice="Select an option (1-3): "

if "%choice%"=="1" goto BUILD
if "%choice%"=="2" goto RUN
if "%choice%"=="3" goto EXIT

echo Invalid choice. Please press 1, 2, or 3.
pause
goto MENU

:BUILD
cls
echo ===========================================
echo                 BUILDING...
echo ===========================================
echo [1/3] Compiling Java source code...
javac CashierGUI.java

if %errorlevel% neq 0 (
    color 0C
    echo.
    echo ERROR: Compilation failed. Please check your Java code for syntax errors.
    pause
    color 0B
    goto MENU
)

echo [2/3] Creating executable JAR file...
jar cfe MyBeansCashier.jar CashierGUI *.class

if %errorlevel% neq 0 (
    color 0C
    echo.
    echo ERROR: Failed to create JAR file.
    pause
    color 0B
    goto MENU
)

echo [3/3] Cleaning up temporary class files...
del *.class

echo.
color 0A
echo ===========================================
echo    BUILD SUCCESSFUL: MyBeansCashier.jar
echo ===========================================
echo.

set /p run=Would you like to run the app now? (Y/N): 
if /i "%run%"=="Y" goto RUN
color 0B
goto MENU

:RUN
echo.
echo Launching MyBeans Cashier Beta...
if exist MyBeansCashier.jar (
    start javaw -jar MyBeansCashier.jar
    :: Optionally close the CMD window after launching by uncommenting the next line
    :: exit
) else (
    color 0C
    echo ERROR: MyBeansCashier.jar not found! Please choose Option 1 to Build it first.
    pause
    color 0B
)
goto MENU

:EXIT
exit