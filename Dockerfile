FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

# Copy the distribution
COPY build/distributions/*.zip ./
RUN unzip *.zip && rm *.zip
RUN mv DailyMalefic-*/* . && rmdir DailyMalefic-*

EXPOSE 7290

ENTRYPOINT ["bin/DailyMalefic"]