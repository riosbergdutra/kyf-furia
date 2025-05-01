# Etapa de build
FROM maven:3.9.6 AS build

# Define o diretório de trabalho
WORKDIR /app

# Copia o arquivo pom.xml e o código-fonte
COPY pom.xml .
COPY src ./src

# Compila o projeto e gera o pacote JAR
RUN mvn clean install -DskipTests

# Etapa final - runtime
FROM openjdk:21-slim
WORKDIR /app

# Copia o JAR gerado da etapa de build
COPY --from=build /app/target/*.jar app.jar

# Exponha a porta usada pela aplicação
EXPOSE 8082

# Comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]