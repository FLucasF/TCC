#!/bin/bash

export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="pousada123"
export PORT=7070
export HOTEL_TEST_MODE=false

java -jar target/reservas-1.0-SNAPSHOT.jar
