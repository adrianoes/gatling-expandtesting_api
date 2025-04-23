@echo off

:: Executando os comandos Gatling para as simulações
echo Executing Gatling Simulations...

:: Rodando a primeira simulação
mvn gatling:test -Dgatling.simulationClass=tests.HealthSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

:: Rodando as simulações de usuários
mvn gatling:test -Dgatling.simulationClass=tests.CreateUserSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

mvn gatling:test -Dgatling.simulationClass=tests.GetUserSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

mvn gatling:test -Dgatling.simulationClass=tests.UpdateUserSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

mvn gatling:test -Dgatling.simulationClass=tests.UpdateUserPasswordSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

mvn gatling:test -Dgatling.simulationClass=tests.LogoutUserSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

:: Rodando as simulações de notas
mvn gatling:test -Dgatling.simulationClass=tests.CreateNoteSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

mvn gatling:test -Dgatling.simulationClass=tests.GetNoteSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

mvn gatling:test -Dgatling.simulationClass=tests.UpdateNoteSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

mvn gatling:test -Dgatling.simulationClass=tests.UpdateNoteStatusSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

mvn gatling:test -Dgatling.simulationClass=tests.GetAllNotesSimulations -P smoke
echo Waiting for 1 minute before next test...
timeout /t 60

mvn gatling:test -Dgatling.simulationClass=tests.DeleteNoteSimulations -P smoke

pause
