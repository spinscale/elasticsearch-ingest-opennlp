@echo off

setlocal enabledelayedexpansion
setlocal enableextensions

set ES_MAIN_CLASS=de.spinscale.elasticsearch.ingest.opennlp.OpenNlpModelDownloader
set ES_ADDITIONAL_SOURCES=ingest-opennlp\ingest-opennlp-env
call "%~dp0..\elasticsearch-cli.bat" ^
  %%* ^
  || exit /b 1

endlocal
endlocal


