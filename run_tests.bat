@echo off
setlocal enabledelayedexpansion

REM Define as simulações em um array
set simulations=HealthSimulations CreateUserSimulations GetUserSimulations UpdateUserSimulations UpdateUserPasswordSimulations LogoutUserSimulations CreateNoteSimulations GetNoteSimulations UpdateNoteSimulations UpdateNoteStatusSimulations GetAllNotesSimulations DeleteNoteSimulations

for %%s in (%simulations%) do (
    echo Running simulation: %%s
    mvn gatling:test -Dgatling.simulationClass=tests.%%s -P smoke
    echo Waiting 60 seconds before next simulation...
    timeout /t 60 /nobreak
)

echo All simulations completed!
pause
