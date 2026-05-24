@echo off
setlocal EnableExtensions
title Upload GitHub automatique

REM Aller dans le dossier ou se trouve ce fichier .bat
cd /d "%~dp0"

echo ==================================================
echo        UPLOAD AUTOMATIQUE VERS GITHUB
echo ==================================================
echo.
echo Dossier detecte :
echo %cd%
echo.

REM Verification de Git
where git >nul 2>nul
if errorlevel 1 (
    echo ERREUR : Git n'est pas installe ou pas reconnu.
    echo Installe Git ici : https://git-scm.com/downloads
    echo Puis relance ce fichier.
    pause
    exit /b 1
)

echo Git est installe :
git --version
echo.

REM Demander l'URL GitHub
echo Exemple d'URL :
echo https://github.com/TON-PSEUDO/TON-REPO.git
echo.
set /p REPO_URL=Colle l'URL du repo GitHub ici : 

if "%REPO_URL%"=="" (
    echo.
    echo ERREUR : tu n'as pas colle d'URL GitHub.
    pause
    exit /b 1
)

if /I "%REPO_URL%"=="origin" (
    echo.
    echo ERREUR : tu as mis "origin". Il faut coller l'URL GitHub complete.
    echo Exemple : https://github.com/TON-PSEDO/TON-REPO.git
    pause
    exit /b 1
)

echo.
echo ==================================================
echo 1/6 - Initialisation Git
echo ==================================================

if not exist ".git" (
    git init
) else (
    echo Git est deja initialise dans ce dossier.
)

echo.
echo ==================================================
echo 2/6 - Ajout des fichiers
echo ==================================================
git add -A

echo.
echo ==================================================
echo 3/6 - Creation du commit
echo ==================================================

git diff --cached --quiet
if errorlevel 1 (
    git commit -m "Mise en ligne du projet"
) else (
    echo Aucun nouveau changement a commit.
)

echo.
echo ==================================================
echo 4/6 - Branche main
echo ==================================================
git branch -M main

echo.
echo ==================================================
echo 5/6 - Configuration du lien GitHub
echo ==================================================

git remote remove origin >nul 2>nul
git remote add origin "%REPO_URL%"

echo Lien GitHub configure :
git remote -v

echo.
echo ==================================================
echo 6/6 - Envoi vers GitHub
echo ==================================================

git push -u origin main

if errorlevel 1 (
    echo.
    echo ==================================================
    echo ERREUR PENDANT LE PUSH
    echo ==================================================
    echo Verifie que :
    echo - le repo GitHub existe deja sur GitHub
    echo - l'URL collee est bien l'URL HTTPS du repo
    echo - tu es bien connecte a ton compte GitHub
    echo - tu as les droits sur ce repo
    echo.
    pause
    exit /b 1
)

echo.
echo ==================================================
echo TERMINE
echo ==================================================
echo Ton projet a ete envoye sur GitHub.
echo.
pause
