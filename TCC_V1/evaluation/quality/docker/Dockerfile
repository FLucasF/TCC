# The quality reviewer's image: the experiment's image (Java 21, Maven, Claude Code, jq, curl; jdeps comes with the
# JDK) plus the PostgreSQL 18 client (pg_dump, psql) and JaCoCo warmed in ~/.m2. Only the evaluation uses it; the
# agents under test never do. Build from TCC_V1/evaluation/quality:
#   docker build -t tcc-v1-reviewer:1 docker
# A change in any line = a new tag.

FROM experimento-harness:v5

USER root
# pg_dump has to be at least as new as the server (18), and Ubuntu 24.04 ships 16: the client comes from PGDG
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
 && install -d /usr/share/postgresql-common/pgdg \
 && curl -fsSL -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
        https://www.postgresql.org/media/keys/ACCC4CF8.asc \
 && echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt noble-pgdg main" \
        > /etc/apt/sources.list.d/pgdg.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends postgresql-client-18 \
 && rm -rf /var/lib/apt/lists/*

USER experimento
# JaCoCo, called from the command line by measure_tests.sh (the project's pom is never touched)
RUN mvn -B -q dependency:get -Dartifact=org.jacoco:jacoco-maven-plugin:0.8.14 \
 && mvn -B -q dependency:get -Dartifact=org.jacoco:org.jacoco.agent:0.8.14:jar:runtime

WORKDIR /review
