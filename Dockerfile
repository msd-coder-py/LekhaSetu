FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY backend ./backend

RUN mkdir -p out && javac -encoding UTF-8 -d out backend/inventory/*.java

CMD ["java", "-cp", "out", "inventory.ApiServer"]
