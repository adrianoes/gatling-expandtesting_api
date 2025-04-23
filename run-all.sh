#!/bin/bash

# Executando os comandos Gatling para as simulações

mvn gatling:test -Dgatling.simulationClass=tests.HealthSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.CreateUserSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.GetUserSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.UpdateUserSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.UpdateUserPasswordSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.LogoutUserSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.CreateNoteSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.GetNoteSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.UpdateNoteSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.UpdateNoteStatusSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.GetAllNotesSimulations -P smoke
mvn gatling:test -Dgatling.simulationClass=tests.DeleteNoteSimulations -P smoke
