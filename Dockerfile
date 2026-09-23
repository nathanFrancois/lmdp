# syntax=docker/dockerfile:1

# ---- Étape 1 : build de l'application avec Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# On copie d'abord le pom.xml pour profiter du cache Docker sur les dépendances
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Puis le reste des sources
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Étape 2 : image d'exécution allégée ----
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Exécution avec un utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring /app/app.jar

USER spring:spring

# Render fournit dynamiquement la variable PORT ; l'appli doit écouter dessus.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]

