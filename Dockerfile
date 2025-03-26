ARG TARGETARCH
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

RUN mkdir /data

COPY build/distributions/*.zip ./
RUN unzip *.zip && rm *.zip
RUN mv DailyMalefic/* . && rmdir DailyMalefic

EXPOSE 7290

VOLUME ["/data"]

ENTRYPOINT ["bin/DailyMalefic"]