# ---------- Etapa 1: build com Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- Etapa 2: execução ----------
# Ubuntu 24.04 (noble) traz o Tesseract 5, compatível com o Tess4J 5.
FROM eclipse-temurin:17-jre-noble
RUN apt-get update \
 && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por libtesseract-dev wget \
 && rm -rf /var/lib/apt/lists/*
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata \
    ARMAZENAMENTO_DIR=/data/uploads
WORKDIR /app
COPY --from=build /app/target/vistoria-inteligente-*.jar app.jar
RUN mkdir -p /data/uploads
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
