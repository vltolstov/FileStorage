# Files-Storage-App #

Приложение для хранения файлов построенное на стеке: Spring, Postgres, S3 хранилище MinIO, Redis

Одностраничный фронт реализован на React.\
Взят и немного изменен https://github.com/zhukovsd/cloud-storage-frontend/tree/master/dist.

Допустимый размер для загрузки файлов 10Мб, для каталогов 50мб.

Доки реализованы через SWAGGER и доступны api/swagger-ui/index.html

Проект развернут на тестовом сервере: http://46.173.29.144:8081/

## Запуск локально: ##

В каталоге docker/local через командную строку запустить для Win:\
<code>docker-compose -p file-storage-app up -d</code>

Сконфигурировать application.properties под себя. Прогнать тесты и запустить.

## Запуск на удаленном сервере: ##

Сконфигурировать docker-compose.yaml в корне проекта.\
Сконфигурировать application.properties под прод.

Перенести на прод:\
Dockerfile\
docker-compose.yaml\
/target/скомпилированный-jar-файл

Запустить:
<code>docker compose -p file-storage-app up -d</code>

