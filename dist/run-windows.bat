@echo off
set DIR=%~dp0
java --module-path "%DIR%lib" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media -jar "%DIR%BookRecommender.jar" %*
