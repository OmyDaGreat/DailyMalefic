FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

# Create data directory for persistence
RUN mkdir /data

# Copy the distribution
COPY build/distributions/*.zip ./
RUN unzip *.zip && rm *.zip
RUN mv DailyMalefic/* . && rmdir DailyMalefic

EXPOSE 7290

VOLUME ["/data"]

ENTRYPOINT ["bin/DailyMalefic"]