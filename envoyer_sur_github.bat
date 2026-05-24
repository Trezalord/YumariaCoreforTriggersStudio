@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Upload GitHub automatique

cd /d "%~dp0"
set "CONFIG_FILE=.github_repo_url.txt"

echo ==================================================
echo        UPLOAD AUTOMATIQUE VERS GITHUB
echo ==================================================
echo.
echo Dossier detecte :
echo %cd%
echo.

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

set "REPO_URL="
if exist "%CONFIG_FILE%" (
    set /p REPO_URL=<"%CONFIG_FILE%"
)

if not "!REPO_URL!"=="" (
    echo URL GitHub deja sauvegardee :
    echo !REPO_URL!
    echo.
    set /p CHANGE_URL=Voulez-vous changer l'URL ? Tape O pour oui, ou Entree pour garder celle-ci : 
    if /I "!CHANGE_URL!"=="O" (
        set "REPO_URL="
    )
)

if "!REPO_URL!"=="" (
    echo Exemple d'URL valide :
    echo https://github.com/LaZiiZaa/yumaria.git
    echo.
    set /p REPO_URL=Colle l'URL du repo GitHub ici : 

    REM Nettoyage des guillemets si Windows les ajoute
    set "REPO_URL=!REPO_URL:"=!"

    if "!REPO_URL!"=="" (
        echo.
        echo ERREUR : tu n'as pas colle d'URL GitHub.
        pause
        exit /b 1
    )

    REM Verification simple : l'URL doit contenir github.com
    echo !REPO_URL! | findstr /I "github.com" >nul
    if errorlevel 1 (
        echo.
        echo ERREUR : ce lien ne ressemble pas a un lien GitHub.
        echo Tu as colle :
        echo !REPO_URL!
        echo.
        echo Exemple attendu :
        echo https://github.com/LaZiiZaa/yumaria.git
        pause
        exit /b 1
    )

    echo !REPO_URL!>"%CONFIG_FILE%"
    echo.
    echo URL sauvegardee dans %CONFIG_FILE%
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
    git commit -m "Mise a jour du projet"
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
git remote add origin "!REPO_URL!"

echo Lien configure :
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
    echo - l'URL est bien : https://github.com/LaZiiZaa/yumaria.git
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
